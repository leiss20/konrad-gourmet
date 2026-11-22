
package co.konradlorenz.abastecimiento.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Repositorio encargado de gestionar cotizaciones
 * de proveedores y órdenes de compra.
 *
 * Permite registrar cotizaciones, agregar detalles,
 * validar precios y generar órdenes de compra.
 */
@Repository
public class CotizacionRepository extends BaseRepository {

    public CotizacionRepository(JdbcClient db) {
        super(db);
    }

    // ==================================================
    // CONSULTAS SQL
    // ==================================================

    private static final String SQL_CREAR_COTIZACION = """
        INSERT INTO cotizacion (
            id_proveedor,
            fecha_maxima_plazo,
            valor_total_ministerio
        )
        VALUES (:p, :f, :t)
        """;

    private static final String SQL_INSERTAR_DETALLE = """
        INSERT INTO cotizacion_detalle (
            id_cotizacion,
            id_solicitud_detalle,
            id_producto,
            id_unidad,
            cantidad,
            precio_ministerio
        )
        VALUES (:c, :sd, :p, :u, :q, :pm)
        """;

    private static final String SQL_LISTAR_COTIZACIONES = """
        SELECT
            c.id_cotizacion,
            pr.nombre AS proveedor,
            c.estado,
            c.valor_total_proveedor,
            c.valor_total_ministerio,
            c.fecha_maxima_plazo,

            (
                SELECT s.numero_solicitud
                FROM cotizacion_detalle cd
                JOIN solicitud_detalle sd
                    ON sd.id_solicitud_detalle = cd.id_solicitud_detalle
                JOIN solicitud s
                    ON s.id_solicitud = sd.id_solicitud
                WHERE cd.id_cotizacion = c.id_cotizacion
                LIMIT 1
            ) AS solicitud,

            (
                SELECT o.numero_orden
                FROM orden_compra o
                WHERE o.id_cotizacion = c.id_cotizacion
                LIMIT 1
            ) AS orden

        FROM cotizacion c
        JOIN proveedor pr
            ON pr.id_proveedor = c.id_proveedor
        ORDER BY c.id_cotizacion DESC
        """;

    private static final String SQL_COTIZACION_POR_ID = """
        SELECT
            c.*,
            (
                SELECT COUNT(*)
                FROM orden_compra o
                WHERE o.id_cotizacion = c.id_cotizacion
            ) AS ordenes
        FROM cotizacion c
        WHERE c.id_cotizacion = :i
        """;

    private static final String SQL_ACTUALIZAR_COTIZACION = """
        UPDATE cotizacion
        SET
            valor_total_proveedor = :v,
            estado = :e
        WHERE id_cotizacion = :i
        """;

    private static final String SQL_ACTUALIZAR_PRECIOS_DETALLE = """
        UPDATE cotizacion_detalle
        SET precio_unitario = precio_ministerio * :r
        WHERE id_cotizacion = :i
        """;

    private static final String SQL_CREAR_ORDEN = """
        INSERT INTO orden_compra (
            id_cotizacion,
            id_proveedor,
            total
        )
        VALUES (:c, :p, :t)
        """;

    private static final String SQL_ACTUALIZAR_NUMERO_ORDEN = """
        UPDATE orden_compra
        SET numero_orden = :n
        WHERE id_orden = :o
        """;

    private static final String SQL_INSERTAR_DETALLES_ORDEN = """
        INSERT INTO orden_compra_detalle (
            id_orden,
            id_producto,
            id_unidad,
            cantidad,
            precio_unitario,
            subtotal
        )
        SELECT
            :o,
            id_producto,
            id_unidad,
            cantidad,
            precio_unitario,
            cantidad * precio_unitario
        FROM cotizacion_detalle
        WHERE id_cotizacion = :c
        """;

    private static final String SQL_LISTAR_ORDENES = """
        SELECT
            o.id_orden,
            o.numero_orden,
            o.fecha_generacion,
            pr.nombre AS proveedor,
            o.estado,
            o.total,

            (
                SELECT string_agg(
                    su.nombre || ': ' || p.nombre || ' '
                        || trim_scale(sd.cantidad)::text || ' ' || u.nombre,
                    ' · '
                    ORDER BY su.nombre, p.nombre
                )
                FROM cotizacion_detalle cd
                JOIN solicitud_detalle sd
                    ON sd.id_solicitud_detalle = cd.id_solicitud_detalle
                JOIN solicitud so
                    ON so.id_solicitud = sd.id_solicitud
                JOIN sucursal su
                    ON su.id_sucursal = so.id_sucursal
                JOIN producto p
                    ON p.id_producto = sd.id_producto
                JOIN unidad u
                    ON u.id_unidad = sd.id_unidad
                WHERE cd.id_cotizacion = o.id_cotizacion
            ) AS detalle

        FROM orden_compra o
        JOIN proveedor pr
            ON pr.id_proveedor = o.id_proveedor
        ORDER BY o.id_orden DESC
        """;

    /*
     * Recepción de mercancía: la orden pasa de 'Generada' a 'Recibida'.
     * La condición estado = 'Generada' evita recibirla dos veces.
     */
    private static final String SQL_ORDEN_POR_ID = """
        SELECT id_orden, numero_orden, estado
        FROM orden_compra
        WHERE id_orden = :o
        """;

    private static final String SQL_MARCAR_RECIBIDA = """
        UPDATE orden_compra
        SET estado = 'Recibida'
        WHERE id_orden = :o
            AND estado = 'Generada'
        """;

    /*
     * Al recibir la orden, las solicitudes de alimentos que la originaron
     * dejan de estar 'EN TRÁMITE' y pasan a 'RECIBIDA'.
     */
    private static final String SQL_SOLICITUDES_RECIBIDAS = """
        UPDATE solicitud
        SET estado = 'RECIBIDA'
        WHERE id_solicitud IN (
            SELECT sd.id_solicitud
            FROM orden_compra o
            JOIN cotizacion_detalle cd
                ON cd.id_cotizacion = o.id_cotizacion
            JOIN solicitud_detalle sd
                ON sd.id_solicitud_detalle = cd.id_solicitud_detalle
            WHERE o.id_orden = :o
        )
        """;

    /*
     * Cantidades que pidió el jefe de cocina en la solicitud de origen
     * de la orden, agrupadas por sucursal y producto. La libra se
     * convierte a kilos, igual que en el descuento del inventario.
     */
    private static final String SQL_RECEPCION_LINEAS = """
        SELECT
            so.id_sucursal,
            sd.id_producto,
            SUM(
                sd.cantidad *
                CASE WHEN u.nombre = 'Libra' THEN 0.5 ELSE 1 END
            ) AS total
        FROM orden_compra o
        JOIN cotizacion_detalle cd
            ON cd.id_cotizacion = o.id_cotizacion
        JOIN solicitud_detalle sd
            ON sd.id_solicitud_detalle = cd.id_solicitud_detalle
        JOIN solicitud so
            ON so.id_solicitud = sd.id_solicitud
        JOIN unidad u
            ON u.id_unidad = sd.id_unidad
        WHERE o.id_orden = :o
        GROUP BY so.id_sucursal, sd.id_producto
        """;

    /*
     * Suma lo recibido al inventario y apaga la alarma si el stock
     * vuelve a superar el 25 % (en el SET se usan los valores previos).
     */
    private static final String SQL_SUMAR_INVENTARIO = """
        UPDATE inventario i
        SET
            cantidad_actual = i.cantidad_actual + r.total,
            ultima_actualizacion = NOW(),
            alarma_enviada = CASE
                WHEN i.cantidad_actual + r.total
                        > i.cantidad_referencia * 0.25
                    THEN FALSE
                ELSE i.alarma_enviada
            END
        FROM (
        """ + SQL_RECEPCION_LINEAS + """
        ) r
        WHERE i.id_sucursal = r.id_sucursal
            AND i.id_producto = r.id_producto
        """;

    // ==================================================
    // CREACIÓN DE COTIZACIONES
    // ==================================================

    /**
     * Crea una nueva cotización de proveedor.
     *
     * @return identificador de la cotización creada.
     */
    public long crear(
            long proveedor,
            LocalDate plazo,
            double totalMinisterio) {

        Objects.requireNonNull(
            plazo,
            "La fecha máxima del plazo es obligatoria."
        );

        validarImporte(totalMinisterio, "Total del Ministerio");

        return insert(
            SQL_CREAR_COTIZACION,
            m(
                "p", proveedor,
                "f", java.sql.Date.valueOf(plazo),
                "t", totalMinisterio
            ),
            "id_cotizacion"
        );
    }

    /**
     * Agrega un producto al detalle de una cotización.
     */
    public void detalle(
            long cotizacion,
            Map<String, Object> linea,
            double precioMinisterio) {

        Objects.requireNonNull(
            linea,
            "La información del detalle es obligatoria."
        );

        validarImporte(precioMinisterio, "Precio del Ministerio");

        double cantidad = d(linea, "cantidad");

        if (!Double.isFinite(cantidad) || cantidad <= 0) {
            throw new IllegalArgumentException(
                "La cantidad debe ser mayor que cero."
            );
        }

        exec(
            SQL_INSERTAR_DETALLE,
            m(
                "c", cotizacion,
                "sd", n(linea, "id_solicitud_detalle"),
                "p", n(linea, "id_producto"),
                "u", n(linea, "id_unidad"),
                "q", cantidad,
                "pm", precioMinisterio
            )
        );
    }

    // ==================================================
    // CONSULTA DE COTIZACIONES
    // ==================================================

    /**
     * Obtiene todas las cotizaciones registradas.
     */
    public List<Map<String, Object>> listar() {

        return rows(SQL_LISTAR_COTIZACIONES, m());
    }

    /**
     * Consulta una cotización por su identificador.
     */
    public Map<String, Object> porId(long id) {

        return uno(
            SQL_COTIZACION_POR_ID,
            m("i", id)
        );
    }

    // ==================================================
    // VALIDACIÓN DE COTIZACIONES
    // ==================================================

    /**
     * Valida una cotización y actualiza sus precios.
     *
     * Ambas modificaciones se ejecutan en una transacción.
     */
    @Transactional
    public void validar(
            long id,
            double valor,
            String estado,
            double ratio) {

        validarImporte(valor, "Valor de la cotización");

        if (estado == null || estado.isBlank()) {
            throw new IllegalArgumentException(
                "El estado de la cotización es obligatorio."
            );
        }

        if (!Double.isFinite(ratio) || ratio < 0) {
            throw new IllegalArgumentException(
                "El factor de distribución no es válido."
            );
        }

        exec(
            SQL_ACTUALIZAR_COTIZACION,
            m(
                "v", valor,
                "e", estado,
                "i", id
            )
        );

        exec(
            SQL_ACTUALIZAR_PRECIOS_DETALLE,
            m(
                "r", ratio,
                "i", id
            )
        );
    }

    // ==================================================
    // GENERACIÓN DE ÓRDENES DE COMPRA
    // ==================================================

    /**
     * Genera una orden de compra a partir de una cotización.
     *
     * Registra la orden, genera su número e incorpora
     * los detalles de la cotización.
     *
     * @return número de orden generado.
     */
    @Transactional
    public String ordenar(
            long id,
            long proveedor,
            double total) {

        validarImporte(total, "Total de la orden");

        long idOrden = insert(
            SQL_CREAR_ORDEN,
            m(
                "c", id,
                "p", proveedor,
                "t", total
            ),
            "id_orden"
        );

        String numeroOrden = generarNumeroOrden(idOrden);

        actualizarNumeroOrden(idOrden, numeroOrden);

        insertarDetallesOrden(idOrden, id);

        return numeroOrden;
    }

    /**
     * Genera el consecutivo de la orden de compra.
     */
    private String generarNumeroOrden(long idOrden) {

        return String.format("OC-%04d", idOrden);
    }

    /**
     * Actualiza el número de la orden registrada.
     */
    private void actualizarNumeroOrden(
            long idOrden,
            String numeroOrden) {

        exec(
            SQL_ACTUALIZAR_NUMERO_ORDEN,
            m(
                "n", numeroOrden,
                "o", idOrden
            )
        );
    }

    /**
     * Copia los detalles de la cotización a la orden.
     */
    private void insertarDetallesOrden(
            long idOrden,
            long idCotizacion) {

        exec(
            SQL_INSERTAR_DETALLES_ORDEN,
            m(
                "o", idOrden,
                "c", idCotizacion
            )
        );
    }

    // ==================================================
    // CONSULTA DE ÓRDENES
    // ==================================================

    /**
     * Obtiene el listado de órdenes de compra.
     */
    public List<Map<String, Object>> ordenes() {

        return rows(SQL_LISTAR_ORDENES, m());
    }

    /**
     * Consulta una orden de compra por su identificador.
     * Devuelve null si no existe.
     */
    public Map<String, Object> orden(long idOrden) {

        return uno(SQL_ORDEN_POR_ID, m("o", idOrden));
    }

    // ==================================================
    // RECEPCIÓN DE MERCANCÍA
    // ==================================================

    /**
     * Marca la orden como recibida y suma al inventario de cada
     * sucursal las cantidades que pidió el jefe de cocina.
     *
     * Todo ocurre en una transacción: si algo falla, no queda nada
     * a medias (ni orden recibida ni inventario modificado).
     *
     * @return cantidad de productos de inventario actualizados.
     */
    @Transactional
    public int recibir(long idOrden) {

        int marcadas = db.sql(SQL_MARCAR_RECIBIDA)
                .params(m("o", idOrden))
                .update();

        if (marcadas == 0) {
            throw new IllegalArgumentException(
                "La orden no existe o ya fue recibida."
            );
        }

        int esperadas = rows(SQL_RECEPCION_LINEAS, m("o", idOrden)).size();

        if (esperadas == 0) {
            throw new IllegalStateException(
                "La orden no está enlazada a ninguna solicitud de alimentos."
            );
        }

        int actualizadas = db.sql(SQL_SUMAR_INVENTARIO)
                .params(m("o", idOrden))
                .update();

        if (actualizadas != esperadas) {
            throw new IllegalStateException(
                "No se encontró el inventario de todos los productos "
                + "de la orden."
            );
        }

        // Actualiza el estado de las solicitudes de origen
        db.sql(SQL_SOLICITUDES_RECIBIDAS)
                .params(m("o", idOrden))
                .update();

        return actualizadas;
    }

    // ==================================================
    // VALIDACIONES
    // ==================================================

    private void validarImporte(double importe, String campo) {

        if (!Double.isFinite(importe) || importe < 0) {
            throw new IllegalArgumentException(
                campo + " debe ser un valor válido mayor o igual a cero."
            );
        }
    }
}
