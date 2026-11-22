
package co.konradlorenz.abastecimiento.repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Repositorio encargado de gestionar los platos
 * y sus respectivos ingredientes.
 */
@Repository
public class PlatoRepository extends BaseRepository {

    private static final String SQL_CREAR_PLATO = """
        INSERT INTO plato (
            nombre,
            precio_venta,
            id_sucursal
        )
        VALUES (:n, :p, :s)
        """;

    private static final String SQL_INSERTAR_INGREDIENTE = """
        INSERT INTO plato_ingrediente (
            id_plato,
            id_producto,
            cantidad,
            id_unidad
        )
        VALUES (:pl, :pr, :c, :u)
        """;

    private static final String SQL_LISTAR_PLATOS = """
        SELECT
            pl.id_plato,
            pl.nombre,
            pl.precio_venta,
            s.nombre AS sucursal,
            (
                SELECT string_agg(
                    p.nombre || ' ' || pi.cantidad::text || ' ' || u.nombre,
                    ', '
                    ORDER BY p.nombre
                )
                FROM plato_ingrediente pi
                JOIN producto p
                    ON p.id_producto = pi.id_producto
                JOIN unidad u
                    ON u.id_unidad = pi.id_unidad
                WHERE pi.id_plato = pl.id_plato
            ) AS ingredientes
        FROM plato pl
        JOIN sucursal s
            ON s.id_sucursal = pl.id_sucursal
        WHERE pl.activo
        """;

    public PlatoRepository(JdbcClient db) {
        super(db);
    }

    // ==================================================
    // CREACIÓN DE PLATOS
    // ==================================================

    /**
     * Crea un plato y registra sus ingredientes.
     *
     * Toda la operación se ejecuta dentro de una
     * transacción para evitar registros incompletos.
     *
     * @return identificador del plato creado.
     */
    @Transactional
    public long crear(
            String nombre,
            double precio,
            long sucursal,
            List<Map<String, Object>> ingredientes) {

        validarDatosPlato(nombre, precio, sucursal, ingredientes);

        long idPlato = registrarPlato(
            nombre,
            precio,
            sucursal
        );

        registrarIngredientes(idPlato, ingredientes);

        return idPlato;
    }

    /**
     * Inserta la información principal del plato.
     */
    private long registrarPlato(
            String nombre,
            double precio,
            long sucursal) {

        return insert(
            SQL_CREAR_PLATO,
            m(
                "n", nombre.trim(),
                "p", precio,
                "s", sucursal
            ),
            "id_plato"
        );
    }

    /**
     * Registra todos los ingredientes asociados al plato.
     */
    private void registrarIngredientes(
            long idPlato,
            List<Map<String, Object>> ingredientes) {

        for (Map<String, Object> ingrediente : ingredientes) {
            registrarIngrediente(idPlato, ingrediente);
        }
    }

    /**
     * Inserta un ingrediente en la receta del plato.
     */
    private void registrarIngrediente(
            long idPlato,
            Map<String, Object> ingrediente) {

        if (ingrediente == null) {
            throw new IllegalArgumentException(
                "Los datos del ingrediente no pueden ser nulos."
            );
        }

        long idProducto = n(ingrediente, "idProducto");
        long idUnidad = n(ingrediente, "idUnidad");
        double cantidad = d(ingrediente, "cantidad");

        if (idProducto <= 0) {
            throw new IllegalArgumentException(
                "El producto del ingrediente no es válido."
            );
        }

        if (idUnidad <= 0) {
            throw new IllegalArgumentException(
                "La unidad del ingrediente no es válida."
            );
        }

        if (!Double.isFinite(cantidad) || cantidad <= 0) {
            throw new IllegalArgumentException(
                "La cantidad del ingrediente debe ser mayor que cero."
            );
        }

        int registrosInsertados = db.sql(SQL_INSERTAR_INGREDIENTE)
                .params(
                    m(
                        "pl", idPlato,
                        "pr", idProducto,
                        "c", cantidad,
                        "u", idUnidad
                    )
                )
                .update();

        if (registrosInsertados != 1) {
            throw new IllegalStateException(
                "No se pudo registrar el ingrediente con producto: "
                    + idProducto
            );
        }
    }

    // ==================================================
    // CONSULTA DE PLATOS
    // ==================================================

    /**
     * Consulta los platos activos.
     *
     * Si se proporciona una sucursal, filtra los
     * resultados por su identificador.
     */
    public List<Map<String, Object>> listar(Long sucursal) {

        StringBuilder sql = new StringBuilder(SQL_LISTAR_PLATOS);

        Map<String, Object> parametros = new HashMap<>();

        if (sucursal != null) {
            sql.append(" AND pl.id_sucursal = :s");
            parametros.put("s", sucursal);
        }

        sql.append(" ORDER BY pl.nombre");

        return rows(sql.toString(), parametros);
    }

    // ==================================================
    // VALIDACIONES
    // ==================================================

    /**
     * Comprueba los datos generales del plato.
     */
    private void validarDatosPlato(
            String nombre,
            double precio,
            long sucursal,
            List<Map<String, Object>> ingredientes) {

        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException(
                "El nombre del plato es obligatorio."
            );
        }

        if (!Double.isFinite(precio) || precio < 0) {
            throw new IllegalArgumentException(
                "El precio del plato debe ser un valor válido y no negativo."
            );
        }

        if (sucursal <= 0) {
            throw new IllegalArgumentException(
                "La sucursal no es válida."
            );
        }

        if (ingredientes == null) {
            throw new IllegalArgumentException(
                "La lista de ingredientes no puede ser nula."
            );
        }
    }
}
