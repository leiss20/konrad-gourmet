
package co.konradlorenz.abastecimiento.repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Repositorio encargado de gestionar las solicitudes
 * de productos realizadas por las sucursales.
 */
@Repository
public class SolicitudRepository extends BaseRepository {

    private static final String SQL_CREAR_SOLICITUD = """
        INSERT INTO solicitud (
            id_sucursal,
            id_solicitante
        )
        VALUES (:s, :u)
        """;

    private static final String SQL_ACTUALIZAR_NUMERO = """
        UPDATE solicitud
        SET numero_solicitud = :n
        WHERE id_solicitud = :i
        """;

    private static final String SQL_INSERTAR_DETALLE = """
        INSERT INTO solicitud_detalle (
            id_solicitud,
            id_producto,
            id_marca,
            cantidad,
            id_unidad
        )
        VALUES (:s, :p, :m, :c, :u)
        """;

    private static final String SQL_LISTAR_SOLICITUDES = """
        SELECT
            s.id_solicitud,
            s.numero_solicitud,
            su.nombre AS sucursal,
            s.fecha_solicitud,
            s.estado,
            (
                SELECT string_agg(
                    p.nombre || ' ' || sd.cantidad::text || ' ' || u.nombre,
                    ', '
                    ORDER BY p.nombre
                )
                FROM solicitud_detalle sd
                JOIN producto p
                    ON p.id_producto = sd.id_producto
                JOIN unidad u
                    ON u.id_unidad = sd.id_unidad
                WHERE sd.id_solicitud = s.id_solicitud
            ) AS productos
        FROM solicitud s
        JOIN sucursal su
            ON su.id_sucursal = s.id_sucursal
        """;

    private static final String SQL_LINEAS_SOLICITUD = """
        SELECT
            sd.id_solicitud_detalle,
            sd.id_producto,
            p.nombre AS producto,
            sd.id_unidad,
            u.nombre AS unidad,
            sd.cantidad
        FROM solicitud_detalle sd
        JOIN producto p
            ON p.id_producto = sd.id_producto
        JOIN unidad u
            ON u.id_unidad = sd.id_unidad
        WHERE sd.id_solicitud = :i
        ORDER BY sd.id_solicitud_detalle
        """;

    private static final String SQL_ACTUALIZAR_ESTADO = """
        UPDATE solicitud
        SET estado = :e
        WHERE id_solicitud = :i
        """;

    public SolicitudRepository(JdbcClient db) {
        super(db);
    }

    // ==================================================
    // CREACIÓN DE SOLICITUDES
    // ==================================================

    /**
     * Crea una solicitud y registra sus productos.
     *
     * La operación completa se ejecuta en una transacción.
     *
     * @return identificador de la solicitud creada.
     */
    @Transactional
    public long crear(
            long sucursal,
            long solicitante,
            List<Map<String, Object>> items) {

        validarSolicitud(sucursal, solicitante, items);

        long idSolicitud = registrarSolicitud(sucursal, solicitante);

        actualizarNumeroSolicitud(idSolicitud);

        registrarDetalles(idSolicitud, items);

        return idSolicitud;
    }

    /**
     * Inserta la información principal de la solicitud.
     */
    private long registrarSolicitud(
            long sucursal,
            long solicitante) {

        return insert(
            SQL_CREAR_SOLICITUD,
            m(
                "s", sucursal,
                "u", solicitante
            ),
            "id_solicitud"
        );
    }

    /**
     * Genera el número visible de la solicitud.
     */
    private void actualizarNumeroSolicitud(long idSolicitud) {

        String numero = String.format("S-%04d", idSolicitud);

        int actualizados = db.sql(SQL_ACTUALIZAR_NUMERO)
                .params(
                    m(
                        "n", numero,
                        "i", idSolicitud
                    )
                )
                .update();

        if (actualizados != 1) {
            throw new IllegalStateException(
                "No se pudo asignar el número a la solicitud."
            );
        }
    }

    /**
     * Registra todos los productos solicitados.
     */
    private void registrarDetalles(
            long idSolicitud,
            List<Map<String, Object>> items) {

        for (Map<String, Object> item : items) {
            registrarDetalle(idSolicitud, item);
        }
    }

    /**
     * Registra un producto dentro de la solicitud.
     */
    private void registrarDetalle(
            long idSolicitud,
            Map<String, Object> item) {

        if (item == null) {
            throw new IllegalArgumentException(
                "Los datos del producto no pueden ser nulos."
            );
        }

        long idProducto = n(item, "idProducto");
        long idUnidad = n(item, "idUnidad");
        double cantidad = d(item, "cantidad");

        Object marca = item.get("idMarca");
        Long idMarca = marca == null ? null : n(item, "idMarca");

        if (idProducto <= 0) {
            throw new IllegalArgumentException(
                "El identificador del producto no es válido."
            );
        }

        if (idUnidad <= 0) {
            throw new IllegalArgumentException(
                "El identificador de la unidad no es válido."
            );
        }

        if (idMarca != null && idMarca <= 0) {
            throw new IllegalArgumentException(
                "El identificador de la marca no es válido."
            );
        }

        if (!Double.isFinite(cantidad) || cantidad <= 0) {
            throw new IllegalArgumentException(
                "La cantidad solicitada debe ser mayor que cero."
            );
        }

        int insertados = db.sql(SQL_INSERTAR_DETALLE)
                .params(
                    m(
                        "s", idSolicitud,
                        "p", idProducto,
                        "m", idMarca,
                        "c", cantidad,
                        "u", idUnidad
                    )
                )
                .update();

        if (insertados != 1) {
            throw new IllegalStateException(
                "No se pudo registrar el producto: " + idProducto
            );
        }
    }

    // ==================================================
    // CONSULTA DE SOLICITUDES
    // ==================================================

    /**
     * Lista las solicitudes registradas.
     *
     * Si sucursal es null, consulta todas las sucursales.
     */
    public List<Map<String, Object>> listar(Long sucursal) {

        StringBuilder sql = new StringBuilder(SQL_LISTAR_SOLICITUDES);

        Map<String, Object> parametros = new HashMap<>();

        if (sucursal != null) {
            sql.append(" WHERE s.id_sucursal = :s");
            parametros.put("s", sucursal);
        }

        sql.append(" ORDER BY s.id_solicitud DESC");

        return rows(sql.toString(), parametros);
    }

    /**
     * Consulta los productos de una solicitud específica.
     */
    public List<Map<String, Object>> lineas(long idSolicitud) {

        if (idSolicitud <= 0) {
            throw new IllegalArgumentException(
                "El identificador de la solicitud no es válido."
            );
        }

        return rows(
            SQL_LINEAS_SOLICITUD,
            m("i", idSolicitud)
        );
    }

    // ==================================================
    // ACTUALIZACIÓN DE SOLICITUDES
    // ==================================================

    /**
     * Actualiza el estado de una solicitud.
     */
    public void estado(long id, String estado) {

        if (id <= 0) {
            throw new IllegalArgumentException(
                "El identificador de la solicitud no es válido."
            );
        }

        if (estado == null || estado.isBlank()) {
            throw new IllegalArgumentException(
                "El estado de la solicitud es obligatorio."
            );
        }

        int actualizados = db.sql(SQL_ACTUALIZAR_ESTADO)
                .params(
                    m(
                        "e", estado.trim(),
                        "i", id
                    )
                )
                .update();

        if (actualizados != 1) {
            throw new IllegalArgumentException(
                "No existe una solicitud con identificador: " + id
            );
        }
    }

    // ==================================================
    // VALIDACIONES
    // ==================================================

    /**
     * Valida los datos generales de la solicitud.
     */
    private void validarSolicitud(
            long sucursal,
            long solicitante,
            List<Map<String, Object>> items) {

        if (sucursal <= 0) {
            throw new IllegalArgumentException(
                "La sucursal no es válida."
            );
        }

        if (solicitante <= 0) {
            throw new IllegalArgumentException(
                "El solicitante no es válido."
            );
        }

        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException(
                "La solicitud debe contener al menos un producto."
            );
        }
    }
}
