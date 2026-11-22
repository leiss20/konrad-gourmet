
package co.konradlorenz.abastecimiento.repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Repositorio encargado de administrar el inventario
 * de las sucursales de Konrad Gourmet.
 *
 * Gestiona existencias, consumo de ingredientes,
 * alarmas y actualización de cantidades.
 */
@Repository
public class InventarioRepository extends BaseRepository {

    private static final double PORCENTAJE_ALARMA = 0.25;

    private static final String SQL_LISTAR_INVENTARIO = """
        SELECT
            i.id_inventario,
            i.id_sucursal,
            s.nombre AS sucursal,
            p.nombre AS producto,
            u.nombre AS unidad,
            i.cantidad_actual,
            i.cantidad_referencia,
            ROUND(
                i.cantidad_actual * 100.0 /
                NULLIF(i.cantidad_referencia, 0),
                1
            ) AS porcentaje
        FROM inventario i
        JOIN sucursal s
            ON s.id_sucursal = i.id_sucursal
        JOIN producto p
            ON p.id_producto = i.id_producto
        JOIN unidad u
            ON u.id_unidad = p.id_unidad
        """;

    private static final String SQL_DETALLE_INVENTARIO = """
        SELECT
            i.id_inventario,
            s.nombre AS sucursal,
            p.nombre AS producto,
            u.nombre AS unidad,
            i.cantidad_actual,
            i.cantidad_referencia
        FROM inventario i
        JOIN sucursal s
            ON s.id_sucursal = i.id_sucursal
        JOIN producto p
            ON p.id_producto = i.id_producto
        JOIN unidad u
            ON u.id_unidad = p.id_unidad
        WHERE i.id_inventario = :i
        """;

    /*
     * Suma la mercancía recibida y apaga la alarma en la misma
     * sentencia cuando el stock vuelve a superar el 25 %.
     * (En PostgreSQL las expresiones del SET usan los valores
     * anteriores a la actualización.)
     */
    private static final String SQL_REABASTECER = """
        UPDATE inventario
        SET
            cantidad_actual = cantidad_actual + :c,
            ultima_actualizacion = NOW(),
            alarma_enviada = CASE
                WHEN cantidad_actual + :c > cantidad_referencia * 0.25
                    THEN FALSE
                ELSE alarma_enviada
            END
        WHERE id_inventario = :i
        """;

    private static final String SQL_DESCONTAR_INVENTARIO = """
        UPDATE inventario i
        SET
            cantidad_actual = GREATEST(
                0,
                i.cantidad_actual - consumo.total
            ),
            ultima_actualizacion = NOW()
        FROM (
            SELECT
                pe.id_sucursal,
                pi.id_producto,
                SUM(
                    pd.cantidad *
                    pi.cantidad *
                    CASE
                        WHEN u.nombre = 'Libra' THEN 0.5
                        ELSE 1
                    END
                ) AS total
            FROM pedido pe
            JOIN pedido_detalle pd
                ON pd.id_pedido = pe.id_pedido
            JOIN plato_ingrediente pi
                ON pi.id_plato = pd.id_plato
            JOIN unidad u
                ON u.id_unidad = pi.id_unidad
            WHERE pe.estado = 'Registrado'
            GROUP BY
                pe.id_sucursal,
                pi.id_producto
        ) consumo
        WHERE
            i.id_sucursal = consumo.id_sucursal
            AND i.id_producto = consumo.id_producto
        """;

    private static final String SQL_PROCESAR_PEDIDOS = """
        UPDATE pedido
        SET estado = 'Procesado'
        WHERE estado = 'Registrado'
        """;

    private static final String SQL_REINICIAR_ALARMAS = """
        UPDATE inventario
        SET alarma_enviada = FALSE
        WHERE
            alarma_enviada = TRUE
            AND cantidad_actual > cantidad_referencia * 0.25
        """;

    private static final String SQL_ALARMAS_NUEVAS = """
        SELECT
            i.id_inventario,
            i.id_sucursal,
            s.nombre AS sucursal,
            p.nombre AS producto,
            i.cantidad_actual,
            i.cantidad_referencia
        FROM inventario i
        JOIN sucursal s
            ON s.id_sucursal = i.id_sucursal
        JOIN producto p
            ON p.id_producto = i.id_producto
        WHERE
            i.cantidad_actual <= i.cantidad_referencia * 0.25
            AND NOT i.alarma_enviada
        """;

    private static final String SQL_MARCAR_ALARMA = """
        UPDATE inventario
        SET alarma_enviada = TRUE
        WHERE id_inventario = :i
        """;

    public InventarioRepository(JdbcClient db) {
        super(db);
    }

    // ==================================================
    // CONSULTA DE INVENTARIO
    // ==================================================

    /**
     * Consulta el inventario general o el de una sucursal.
     *
     * @param sucursal identificador de sucursal.
     *                  Si es null, consulta todas.
     */
    public List<Map<String, Object>> listar(Long sucursal) {

        StringBuilder sql = new StringBuilder(SQL_LISTAR_INVENTARIO);

        Map<String, Object> parametros = new HashMap<>();

        if (sucursal != null) {
            sql.append(" WHERE i.id_sucursal = :s");
            parametros.put("s", sucursal);
        }

        sql.append(" ORDER BY porcentaje, p.nombre");

        return rows(sql.toString(), parametros);
    }

    /**
     * Consulta un registro de inventario con su sucursal,
     * producto y unidad. Devuelve null si no existe.
     */
    public Map<String, Object> detalle(long idInventario) {

        return uno(SQL_DETALLE_INVENTARIO, m("i", idInventario));
    }

    // ==================================================
    // REABASTECIMIENTO
    // ==================================================

    /**
     * Suma al inventario la mercancía recibida de los proveedores.
     * La cantidad se expresa en la unidad base del producto.
     *
     * @return filas actualizadas (0 si el inventario no existe).
     */
    public int reabastecer(long idInventario, double cantidad) {

        return db.sql(SQL_REABASTECER)
                .params(m("i", idInventario, "c", cantidad))
                .update();
    }

    // ==================================================
    // DESCUENTO DE INVENTARIO
    // ==================================================

    /**
     * Descuenta los ingredientes consumidos por los pedidos
     * registrados y posteriormente marca los pedidos
     * como procesados.
     *
     * Ambas operaciones se ejecutan en una transacción.
     *
     * @return cantidad de pedidos procesados.
     */
    @Transactional
    public int descontar() {

        descontarIngredientes();

        return procesarPedidos();
    }

    /**
     * Calcula y descuenta el consumo de ingredientes
     * correspondiente a los pedidos registrados.
     */
    private void descontarIngredientes() {

        exec(SQL_DESCONTAR_INVENTARIO, m());
    }

    /**
     * Marca los pedidos registrados como procesados.
     */
    private int procesarPedidos() {

        return db.sql(SQL_PROCESAR_PEDIDOS).update();
    }

    // ==================================================
    // ADMINISTRACIÓN DE ALARMAS
    // ==================================================

    /**
     * Restablece las alarmas cuando el inventario
     * supera el 25 % de su cantidad de referencia.
     */
    public void reiniciarAlarmas() {

        exec(SQL_REINICIAR_ALARMAS, m());
    }

    /**
     * Consulta los productos que han alcanzado
     * el nivel mínimo de inventario.
     */
    public List<Map<String, Object>> alarmasNuevas() {

        return rows(SQL_ALARMAS_NUEVAS, m());
    }

    /**
     * Marca una alarma como enviada.
     */
    public void marcar(long id) {

        exec(
            SQL_MARCAR_ALARMA,
            m("i", id)
        );
    }
}
