
package co.konradlorenz.abastecimiento.repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Repositorio encargado de gestionar los pedidos
 * realizados en las sucursales de Konrad Gourmet.
 *
 * Permite registrar pedidos, agregar sus productos
 * y consultar el historial de pedidos.
 */
@Repository
public class PedidoRepository extends BaseRepository {

    private static final String SQL_CREAR_PEDIDO = """
        INSERT INTO pedido (
            numero_mesa,
            id_sucursal,
            id_mesero
        )
        VALUES (:m, :s, :u)
        """;

    private static final String SQL_INSERTAR_DETALLE = """
        INSERT INTO pedido_detalle (
            id_pedido,
            id_plato,
            cantidad,
            precio_unitario
        )
        SELECT
            :p,
            id_plato,
            :c,
            precio_venta
        FROM plato
        WHERE id_plato = :pl
        """;

    private static final String SQL_LISTAR_PEDIDOS = """
        SELECT
            pe.id_pedido,
            pe.numero_mesa,
            s.nombre AS sucursal,
            pe.fecha_hora,
            pe.estado,
            (
                SELECT string_agg(
                    pl.nombre || ' x' || d.cantidad,
                    ', '
                    ORDER BY d.id_pedido_detalle
                )
                FROM pedido_detalle d
                JOIN plato pl
                    ON pl.id_plato = d.id_plato
                WHERE d.id_pedido = pe.id_pedido
            ) AS detalle
        FROM pedido pe
        JOIN sucursal s
            ON s.id_sucursal = pe.id_sucursal
        """;

    public PedidoRepository(JdbcClient db) {
        super(db);
    }

    // ==================================================
    // CREACIÓN DE PEDIDOS
    // ==================================================

    /**
     * Registra un pedido y sus respectivos productos.
     *
     * El precio de cada producto se obtiene directamente
     * de la base de datos para evitar manipulaciones
     * desde el cliente.
     *
     * Todas las operaciones se ejecutan en una transacción.
     *
     * @return identificador del pedido creado.
     */
    @Transactional
    public long crear(
            int mesa,
            long sucursal,
            long mesero,
            List<Map<String, Object>> items) {

        validarDatosPedido(mesa, sucursal, mesero, items);

        long idPedido = registrarPedido(mesa, sucursal, mesero);

        registrarDetalles(idPedido, items);

        return idPedido;
    }

    /**
     * Registra la información principal del pedido.
     */
    private long registrarPedido(
            int mesa,
            long sucursal,
            long mesero) {

        return insert(
            SQL_CREAR_PEDIDO,
            m(
                "m", mesa,
                "s", sucursal,
                "u", mesero
            ),
            "id_pedido"
        );
    }

    /**
     * Registra los productos asociados al pedido.
     */
    private void registrarDetalles(
            long idPedido,
            List<Map<String, Object>> items) {

        for (Map<String, Object> item : items) {
            registrarDetalle(idPedido, item);
        }
    }

    /**
     * Registra un producto dentro del pedido.
     *
     * El precio se obtiene de la tabla plato.
     */
    private void registrarDetalle(
            long idPedido,
            Map<String, Object> item) {

        Objects.requireNonNull(
            item,
            "Los datos del producto no pueden ser nulos."
        );

        long idPlato = n(item, "idPlato");
        long cantidad = n(item, "cantidad");

        if (idPlato <= 0) {
            throw new IllegalArgumentException(
                "El identificador del plato no es válido."
            );
        }

        if (cantidad <= 0 || cantidad > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(
                "La cantidad del producto debe ser un entero positivo válido."
            );
        }

        int registrosInsertados = db.sql(SQL_INSERTAR_DETALLE)
                .params(
                    m(
                        "p", idPedido,
                        "c", cantidad,
                        "pl", idPlato
                    )
                )
                .update();

        if (registrosInsertados != 1) {
            throw new IllegalArgumentException(
                "No se pudo registrar el plato con identificador: " + idPlato
            );
        }
    }

    // ==================================================
    // CONSULTA DE PEDIDOS
    // ==================================================

    /**
     * Consulta los pedidos registrados.
     *
     * Si se proporciona una sucursal, limita
     * los resultados a dicha sucursal.
     *
     * @param sucursal identificador de sucursal.
     *                  Si es null, consulta todas.
     */
    public List<Map<String, Object>> listar(Long sucursal) {

        StringBuilder sql = new StringBuilder(SQL_LISTAR_PEDIDOS);

        Map<String, Object> parametros = new HashMap<>();

        if (sucursal != null) {
            sql.append(" WHERE pe.id_sucursal = :s");
            parametros.put("s", sucursal);
        }

        sql.append(" ORDER BY pe.id_pedido DESC");

        return rows(sql.toString(), parametros);
    }

    // ==================================================
    // VALIDACIONES
    // ==================================================

    /**
     * Valida los datos generales del pedido.
     */
    private void validarDatosPedido(
            int mesa,
            long sucursal,
            long mesero,
            List<Map<String, Object>> items) {

        if (mesa <= 0) {
            throw new IllegalArgumentException(
                "El número de mesa debe ser mayor que cero."
            );
        }

        if (sucursal <= 0) {
            throw new IllegalArgumentException(
                "La sucursal no es válida."
            );
        }

        if (mesero <= 0) {
            throw new IllegalArgumentException(
                "El identificador del mesero no es válido."
            );
        }

        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException(
                "El pedido debe contener al menos un producto."
            );
        }
    }
}
