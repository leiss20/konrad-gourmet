
package co.konradlorenz.abastecimiento.service;

import co.konradlorenz.abastecimiento.repository.InventarioRepository;
import co.konradlorenz.abastecimiento.repository.UsuarioRepository;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventarioService {

    private static final Logger LOGGER =
        LoggerFactory.getLogger(InventarioService.class);

    private final InventarioRepository repo;
    private final UsuarioRepository usuarios;
    private final CorreoService correo;
    private final AuditoriaService auditoria;

    public InventarioService(
            InventarioRepository repo,
            UsuarioRepository usuarios,
            CorreoService correo,
            AuditoriaService auditoria) {

        this.repo = repo;
        this.usuarios = usuarios;
        this.correo = correo;
        this.auditoria = auditoria;
    }

    /**
     * Ejecuta diariamente el procesamiento del inventario.
     *
     * Descuenta los pedidos registrados, reinicia las alarmas
     * que dejaron de cumplir la condición y notifica los
     * productos que alcanzaron el 25 % o menos.
     */
    @Scheduled(cron = "0 0 23 * * *")
    public String procesar() {

        int pedidos = repo.descontar();

        repo.reiniciarAlarmas();

        List<Map<String, Object>> alarmas = repo.alarmasNuevas();

        int alarmasNotificadas = 0;

        for (Map<String, Object> alarma : alarmas) {

            long idSucursal = obtenerId(alarma, "id_sucursal");
            long idInventario = obtenerId(alarma, "id_inventario");

            List<String> destinatarios =
                usuarios.correosJefes(idSucursal);

            if (destinatarios == null || destinatarios.isEmpty()) {
                LOGGER.warn(
                    "No hay jefes de cocina registrados para la sucursal {}. "
                    + "La alarma {} permanece pendiente.",
                    idSucursal,
                    idInventario
                );

                continue;
            }

            boolean envioCompleto = notificarJefes(
                alarma,
                destinatarios
            );

            /*
             * La alarma solo se marca cuando todos los correos
             * se enviaron sin lanzar excepciones.
             */
            if (envioCompleto) {
                repo.marcar(idInventario);
                alarmasNotificadas++;
            }
        }

        String resultado = pedidos
            + " pedidos procesados, "
            + alarmasNotificadas
            + " alarmas notificadas.";

        auditoria.registrarSistema(
            "PROCESAR",
            "inventario",
            null,
            resultado
        );

        LOGGER.info("Procesamiento de inventario: {}", resultado);

        return resultado;
    }

    /**
     * Envía la notificación a los jefes de cocina de una sucursal.
     *
     * Si falla al menos un envío, la alarma permanece pendiente.
     */
    private boolean notificarJefes(
            Map<String, Object> alarma,
            List<String> destinatarios) {

        String producto = String.valueOf(alarma.get("producto"));
        String sucursal = String.valueOf(alarma.get("sucursal"));

        Object actual = alarma.get("cantidad_actual");
        Object referencia = alarma.get("cantidad_referencia");

        String asunto = "Alarma de inventario: " + producto;

        String mensaje = "En "
            + sucursal
            + " quedan "
            + actual
            + " de "
            + referencia
            + " (25 % o menos). Registra una solicitud de alimentos.";

        boolean envioCompleto = true;

        for (String destinatario : destinatarios) {

            try {
                correo.enviar(
                    destinatario,
                    asunto,
                    mensaje,
                    null,
                    null
                );

            } catch (RuntimeException e) {

                envioCompleto = false;

                LOGGER.error(
                    "No se pudo enviar la alarma de inventario "
                    + "del producto {} a {}.",
                    producto,
                    destinatario,
                    e
                );
            }
        }

        return envioCompleto;
    }

    /**
     * Consulta el inventario según el rol del usuario.
     *
     * JEFE_COCINA: únicamente su sucursal.
     * AUXILIAR_COMPRAS: todas las sucursales.
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> inventario(String email) {

        Map<String, Object> usuario = usuarios.porEmail(email);

        if (usuario == null) {
            throw new IllegalArgumentException(
                "El usuario no existe o no está registrado."
            );
        }

        String rol = (String) usuario.get("rol");

        if ("JEFE_COCINA".equals(rol)) {

            long sucursal = obtenerId(usuario, "id_sucursal");

            return repo.listar(sucursal);
        }

        if ("AUXILIAR_COMPRAS".equals(rol)) {
            return repo.listar(null);
        }

        throw new IllegalArgumentException(
            "No tienes permisos para consultar el inventario."
        );
    }

    /**
     * Extrae un identificador numérico de un resultado SQL.
     */
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
}
