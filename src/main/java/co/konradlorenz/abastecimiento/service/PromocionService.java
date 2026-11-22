
package co.konradlorenz.abastecimiento.service;

import co.konradlorenz.abastecimiento.adapter.TendenciasAdapter;
import co.konradlorenz.abastecimiento.repository.BamRepository;
import co.konradlorenz.abastecimiento.repository.UsuarioRepository;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class PromocionService {

    private static final Logger LOGGER =
        LoggerFactory.getLogger(PromocionService.class);

    private final BamRepository bam;
    private final UsuarioRepository usuarios;
    private final CorreoService correo;
    private final TendenciasAdapter tendencias;
    private final AuditoriaService auditoria;

    public PromocionService(
            BamRepository bam,
            UsuarioRepository usuarios,
            CorreoService correo,
            TendenciasAdapter tendencias,
            AuditoriaService auditoria) {

        this.bam = bam;
        this.usuarios = usuarios;
        this.correo = correo;
        this.tendencias = tendencias;
        this.auditoria = auditoria;
    }

    /**
     * Envía semanalmente recomendaciones de productos
     * y tendencias a los jefes de cocina.
     *
     * Se ejecuta los lunes a las 8:00 a. m.
     */
    @Scheduled(cron = "0 0 8 * * MON")
    public String enviar() {

        List<String> productos = obtenerProductosDestacados();

        List<String> tendenciasActuales = obtenerTendencias();

        List<String> destinatarios = obtenerDestinatarios();

        if (destinatarios.isEmpty()) {

            String resultado =
                "No hay jefes de cocina registrados para recibir promociones.";

            LOGGER.warn(resultado);

            auditoria.registrarSistema(
                "ENVIAR",
                "promocion",
                null,
                resultado
            );

            return resultado;
        }

        String mensaje = construirMensaje(
            productos,
            tendenciasActuales
        );

        int enviados = 0;
        int fallidos = 0;

        for (String destinatario : destinatarios) {

            try {

                correo.enviar(
                    destinatario,
                    "Promociones y productos destacados",
                    mensaje,
                    null,
                    null
                );

                enviados++;

            } catch (RuntimeException e) {

                fallidos++;

                LOGGER.error(
                    "No se pudo enviar la promoción a {}.",
                    destinatario,
                    e
                );
            }
        }

        String resultado = enviados
            + " correos enviados, "
            + fallidos
            + " correos fallidos.";

        auditoria.registrarSistema(
            "ENVIAR",
            "promocion",
            null,
            resultado
        );

        LOGGER.info("Resultado del envío de promociones: {}", resultado);

        return resultado;
    }

    /**
     * Obtiene los tres productos con mayor demanda.
     */
    private List<String> obtenerProductosDestacados() {

        Map<String, Object> indicadores = bam.kpis();

        if (indicadores == null) {
            return List.of();
        }

        Object demanda = indicadores.get("demanda");

        if (!(demanda instanceof List<?> lista)) {
            return List.of();
        }

        List<String> productos = new ArrayList<>();

        for (Object elemento : lista) {

            if (!(elemento instanceof Map<?, ?> producto)) {
                continue;
            }

            Object nombre = producto.get("nombre");

            if (nombre instanceof String texto && !texto.isBlank()) {
                productos.add(texto.trim());
            }

            if (productos.size() == 3) {
                break;
            }
        }

        return productos;
    }

    /**
     * Consulta las tendencias disponibles.
     */
    private List<String> obtenerTendencias() {

        List<String> resultado = tendencias.tendencias();

        if (resultado == null) {
            return List.of();
        }

        return resultado.stream()
            .filter(t -> t != null && !t.isBlank())
            .map(String::trim)
            .distinct()
            .toList();
    }

    /**
     * Obtiene los correos de los jefes de cocina,
     * eliminando duplicados y valores vacíos.
     */
    private List<String> obtenerDestinatarios() {

        List<String> correos =
            usuarios.correosPorRol("JEFE_COCINA");

        if (correos == null) {
            return List.of();
        }

        Set<String> destinatarios = new LinkedHashSet<>();

        for (String email : correos) {

            if (email != null && !email.isBlank()) {
                destinatarios.add(email.trim());
            }
        }

        return new ArrayList<>(destinatarios);
    }

    /**
     * Construye el contenido del correo.
     */
    private String construirMensaje(
            List<String> productos,
            List<String> tendenciasActuales) {

        String productosTexto = productos.isEmpty()
            ? "No hay productos destacados disponibles."
            : String.join(", ", productos);

        String tendenciasTexto = tendenciasActuales.isEmpty()
            ? "No hay tendencias disponibles actualmente."
            : String.join(", ", tendenciasActuales);

        return "Productos más solicitados este mes: "
            + productosTexto
            + ".\n\nTendencias en redes: "
            + tendenciasTexto
            + ".\n\nAprovecha las oportunidades de promoción "
            + "con tus proveedores.";
    }
}
