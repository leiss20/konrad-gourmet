
package co.konradlorenz.abastecimiento.service;

import co.konradlorenz.abastecimiento.adapter.MinisterioAdapter;
import co.konradlorenz.abastecimiento.repository.CatalogoRepository;
import co.konradlorenz.abastecimiento.repository.CotizacionRepository;
import co.konradlorenz.abastecimiento.repository.SolicitudRepository;
import co.konradlorenz.abastecimiento.repository.UsuarioRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio encargado de coordinar el proceso de compras:
 * solicitudes, cotizaciones, validaciones y órdenes de compra.
 */
@Service
@Transactional
public class ComprasService {

    private final SolicitudRepository solicitudes;
    private final CotizacionRepository cotizaciones;
    private final UsuarioRepository usuarios;
    private final MinisterioAdapter ministerio;
    private final AuditoriaService auditoria;
    private final CorreoService correo;
    private final CatalogoRepository catalogo;

    /*
     * Conversión utilizada actualmente por las reglas
     * existentes del proyecto.
     *
     * IMPORTANTE: 0.5 es una aproximación, no la
     * equivalencia exacta de una libra en kilogramos.
     */
    private static final double LIBRA_A_KILOGRAMOS = 0.5;

    public ComprasService(
            SolicitudRepository solicitudes,
            CotizacionRepository cotizaciones,
            UsuarioRepository usuarios,
            MinisterioAdapter ministerio,
            AuditoriaService auditoria,
            CorreoService correo,
            CatalogoRepository catalogo) {

        this.solicitudes = solicitudes;
        this.cotizaciones = cotizaciones;
        this.usuarios = usuarios;
        this.ministerio = ministerio;
        this.auditoria = auditoria;
        this.correo = correo;
        this.catalogo = catalogo;
    }

    // ==================================================
    // SOLICITUDES
    // ==================================================

    /**
     * Registra una solicitud de productos para la
     * sucursal a la que pertenece el solicitante.
     */
    public long solicitar(
            String email,
            List<Map<String, Object>> items) {

        Map<String, Object> usuario = obtenerUsuario(email);

        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException(
                "Agrega al menos un producto a la solicitud."
            );
        }

        long idSucursal = obtenerId(usuario, "id_sucursal");
        long idSolicitante = obtenerId(usuario, "id_usuario");

        long idSolicitud = solicitudes.crear(
            idSucursal,
            idSolicitante,
            items
        );

        auditoria.registrar(
            email,
            "CREAR",
            "solicitud",
            idSolicitud,
            "Estado PENDIENTE"
        );

        correo.enviar(
            email,
            String.format("Solicitud S-%04d registrada", idSolicitud),
            "Tu solicitud quedó registrada en estado PENDIENTE.",
            null,
            null
        );

        return idSolicitud;
    }

    /**
     * Consulta las solicitudes según el rol del usuario.
     *
     * JEFE_COCINA: consulta únicamente su sucursal.
     * AUXILIAR_COMPRAS: consulta todas las sucursales.
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> solicitudes(String email) {

        Map<String, Object> usuario = obtenerUsuario(email);

        String rol = (String) usuario.get("rol");

        if ("JEFE_COCINA".equals(rol)) {
            long sucursal = obtenerId(usuario, "id_sucursal");
            return solicitudes.listar(sucursal);
        }

        if ("AUXILIAR_COMPRAS".equals(rol)) {
            return solicitudes.listar(null);
        }

        throw new IllegalArgumentException(
            "No tienes permisos para consultar las solicitudes."
        );
    }

    // ==================================================
    // COTIZACIONES
    // ==================================================

    /**
     * Genera una cotización a partir de una solicitud.
     *
     * Consulta los precios de referencia del Ministerio,
     * registra los detalles y envía la solicitud al proveedor.
     */
    public long cotizar(
            String email,
            long idSolicitud,
            long idProveedor,
            LocalDate plazo) {

        validarIdentificadores(idSolicitud, idProveedor);

        if (plazo == null || plazo.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException(
                "La fecha máxima no puede ser anterior a hoy."
            );
        }

        Map<String, Object> usuario = obtenerUsuario(email);

        List<Map<String, Object>> lineas = solicitudes.lineas(idSolicitud);

        if (lineas.isEmpty()) {
            throw new IllegalArgumentException(
                "La solicitud no existe o no tiene productos."
            );
        }

        Map<String, Object> proveedor = catalogo.proveedor(idProveedor);

        if (proveedor == null) {
            throw new IllegalArgumentException(
                "El proveedor seleccionado no existe."
            );
        }

        String emailProveedor = (String) proveedor.get("email");
        String nombreProveedor = (String) proveedor.get("nombre");

        if (emailProveedor == null || emailProveedor.isBlank()) {
            throw new IllegalArgumentException(
                "El proveedor no tiene un correo electrónico registrado."
            );
        }

        double totalMinisterio = 0;
        List<Double> preciosMinisterio = new ArrayList<>();

        for (Map<String, Object> linea : lineas) {

            String producto = (String) linea.get("producto");
            String unidad = (String) linea.get("unidad");

            if (producto == null || unidad == null) {
                throw new IllegalStateException(
                    "La solicitud contiene un producto o unidad incompletos."
                );
            }

            double precioPorKg = ministerio.precioPorKg(producto);

            if (!Double.isFinite(precioPorKg) || precioPorKg <= 0) {
                throw new IllegalStateException(
                    "El Ministerio devolvió un precio inválido para: "
                    + producto
                );
            }

            double factor = obtenerFactorUnidad(unidad);
            double precioUnitario = precioPorKg * factor;

            double cantidad = obtenerCantidad(linea);

            double subtotal = cantidad * precioUnitario;

            if (!Double.isFinite(subtotal)) {
                throw new IllegalStateException(
                    "El subtotal calculado no es válido para: " + producto
                );
            }

            preciosMinisterio.add(precioUnitario);
            totalMinisterio += subtotal;
        }

        if (!Double.isFinite(totalMinisterio) || totalMinisterio <= 0) {
            throw new IllegalStateException(
                "El total de referencia del Ministerio no es válido."
            );
        }

        long idCotizacion = cotizaciones.crear(
            idProveedor,
            plazo,
            totalMinisterio
        );

        registrarDetallesCotizacion(
            idCotizacion,
            lineas,
            preciosMinisterio
        );

        solicitudes.estado(idSolicitud, "EN TRÁMITE");

        String asunto = "Solicitud de cotización " + idCotizacion;

        String mensaje =
            "Adjuntamos la solicitud de cotización. "
            + "Fecha máxima de respuesta: " + plazo;

        byte[] archivo = PdfCotizacion.generar(
            String.valueOf(idCotizacion),
            nombreProveedor,
            plazo,
            lineas
        );

        correo.enviar(
            emailProveedor,
            asunto,
            mensaje,
            "cotizacion-" + idCotizacion + ".pdf",
            archivo
        );

        auditoria.registrar(
            email,
            "COTIZAR",
            "cotizacion",
            idCotizacion,
            "Solicitud " + idSolicitud
        );

        return idCotizacion;
    }

    /**
     * Registra los detalles de una cotización.
     */
    private void registrarDetallesCotizacion(
            long idCotizacion,
            List<Map<String, Object>> lineas,
            List<Double> precios) {

        for (int i = 0; i < lineas.size(); i++) {
            cotizaciones.detalle(
                idCotizacion,
                lineas.get(i),
                precios.get(i)
            );
        }
    }

    // ==================================================
    // VALIDACIÓN DE COTIZACIONES
    // ==================================================

    /**
     * Compara el precio del proveedor con el precio
     * de referencia del Ministerio.
     *
     * Regla de negocio:
     * - Más de 25 % por encima: RECHAZADA.
     * - Más de 50 % por debajo: SOSPECHOSA.
     * - En los demás casos: OPCIONADA.
     */
    public String validar(
            String email,
            long id,
            double valor) {

        validarEmail(email);

        if (id <= 0) {
            throw new IllegalArgumentException(
                "El identificador de la cotización no es válido."
            );
        }

        if (!Double.isFinite(valor) || valor <= 0) {
            throw new IllegalArgumentException(
                "Ingresa un valor válido mayor que cero."
            );
        }

        obtenerUsuario(email);

        Map<String, Object> cotizacion = cotizaciones.porId(id);

        if (cotizacion == null
                || !"ENVIADA".equals(cotizacion.get("estado"))) {

            throw new IllegalArgumentException(
                "Solo se validan cotizaciones en estado ENVIADA."
            );
        }

        Object referencia = cotizacion.get("valor_total_ministerio");

        if (!(referencia instanceof Number)) {
            throw new IllegalStateException(
                "La cotización no tiene un precio de referencia válido."
            );
        }

        double totalMinisterio = ((Number) referencia).doubleValue();

        if (!Double.isFinite(totalMinisterio) || totalMinisterio <= 0) {
            throw new IllegalStateException(
                "El precio de referencia debe ser mayor que cero."
            );
        }

        double ratio = valor / totalMinisterio;

        if (!Double.isFinite(ratio)) {
            throw new IllegalArgumentException(
                "No se pudo calcular la comparación de precios."
            );
        }

        String estado = determinarEstado(ratio);

        cotizaciones.validar(
            id,
            valor,
            estado,
            ratio
        );

        auditoria.registrar(
            email,
            "VALIDAR",
            "cotizacion",
            id,
            estado
        );

        return estado;
    }

    /**
     * Determina el estado de una cotización según su ratio.
     */
    private String determinarEstado(double ratio) {

        if (ratio > 1.25) {
            return "RECHAZADA";
        }

        if (ratio < 0.5) {
            return "SOSPECHOSA";
        }

        return "OPCIONADA";
    }

    // ==================================================
    // ÓRDENES DE COMPRA
    // ==================================================

    /**
     * Genera una orden de compra a partir de una
     * cotización que haya sido opcionada.
     */
    public String ordenar(String email, long id) {

        obtenerUsuario(email);

        if (id <= 0) {
            throw new IllegalArgumentException(
                "El identificador de la cotización no es válido."
            );
        }

        Map<String, Object> cotizacion = cotizaciones.porId(id);

        if (cotizacion == null
                || !"OPCIONADA".equals(cotizacion.get("estado"))) {

            throw new IllegalArgumentException(
                "Solo se puede ordenar una cotización OPCIONADA."
            );
        }

        Object ordenesRegistradas = cotizacion.get("ordenes");

        if (!(ordenesRegistradas instanceof Number)
                || ((Number) ordenesRegistradas).intValue() > 0) {

            throw new IllegalArgumentException(
                "La cotización ya tiene una orden o no se pudo verificar."
            );
        }

        long idProveedor = obtenerId(cotizacion, "id_proveedor");

        double total = obtenerNumeroPositivo(
            cotizacion,
            "valor_total_proveedor"
        );

        String numeroOrden = cotizaciones.ordenar(
            id,
            idProveedor,
            total
        );

        auditoria.registrar(
            email,
            "ORDENAR",
            "orden_compra",
            id,
            numeroOrden
        );

        return numeroOrden;
    }

    // ==================================================
    // CONSULTAS
    // ==================================================

    @Transactional(readOnly = true)
    public List<Map<String, Object>> cotizaciones() {
        return cotizaciones.listar();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> ordenes() {
        return cotizaciones.ordenes();
    }

    // ==================================================
    // VALIDACIONES Y UTILIDADES
    // ==================================================

    private Map<String, Object> obtenerUsuario(String email) {

        validarEmail(email);

        Map<String, Object> usuario = usuarios.porEmail(email.trim());

        if (usuario == null) {
            throw new IllegalArgumentException(
                "El usuario no existe o no está registrado."
            );
        }

        return usuario;
    }

    private void validarEmail(String email) {

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException(
                "El correo electrónico es obligatorio."
            );
        }
    }

    private void validarIdentificadores(long solicitud, long proveedor) {

        if (solicitud <= 0 || proveedor <= 0) {
            throw new IllegalArgumentException(
                "La solicitud y el proveedor deben tener identificadores válidos."
            );
        }
    }

    private long obtenerId(Map<String, Object> datos, String campo) {

        Object valor = datos.get(campo);

        if (!(valor instanceof Number)) {
            throw new IllegalStateException(
                "No se pudo obtener el identificador: " + campo
            );
        }

        long id = ((Number) valor).longValue();

        if (id <= 0) {
            throw new IllegalStateException(
                "El identificador " + campo + " no es válido."
            );
        }

        return id;
    }

    private double obtenerCantidad(Map<String, Object> linea) {

        Object valor = linea.get("cantidad");

        if (!(valor instanceof Number)) {
            throw new IllegalStateException(
                "La cantidad de un producto no es válida."
            );
        }

        double cantidad = ((Number) valor).doubleValue();

        if (!Double.isFinite(cantidad) || cantidad <= 0) {
            throw new IllegalStateException(
                "La cantidad de un producto debe ser mayor que cero."
            );
        }

        return cantidad;
    }

    private double obtenerNumeroPositivo(
            Map<String, Object> datos,
            String campo) {

        Object valor = datos.get(campo);

        if (!(valor instanceof Number)) {
            throw new IllegalStateException(
                "No se encontró un valor numérico para: " + campo
            );
        }

        double numero = ((Number) valor).doubleValue();

        if (!Double.isFinite(numero) || numero <= 0) {
            throw new IllegalStateException(
                "El valor de " + campo + " debe ser mayor que cero."
            );
        }

        return numero;
    }

    /**
     * Convierte la unidad de la solicitud al factor
     * utilizado por el servicio de precios por kilogramo.
     */
    private double obtenerFactorUnidad(String unidad) {

        if ("Libra".equalsIgnoreCase(unidad.trim())) {
            return LIBRA_A_KILOGRAMOS;
        }

        return 1.0;
    }
}
