
package co.konradlorenz.abastecimiento.repository;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Repositorio encargado de administrar los catálogos
 * y productos de Konrad Gourmet.
 *
 * Gestiona categorías, productos, unidades, marcas,
 * proveedores y sucursales.
 */
@Repository
public class CatalogoRepository extends BaseRepository {

    private static final Set<String> TABLAS_PERMITIDAS =
            Set.of("categoria", "marca", "unidad");

    private static final String SQL_CATEGORIAS = """
        SELECT id_categoria, nombre
        FROM categoria
        ORDER BY nombre
        """;

    private static final String SQL_PRODUCTOS = """
        SELECT id_producto, nombre, id_categoria, id_unidad
        FROM producto
        WHERE activo
        ORDER BY nombre
        """;

    private static final String SQL_UNIDADES = """
        SELECT id_unidad, nombre
        FROM unidad
        ORDER BY id_unidad
        """;

    private static final String SQL_MARCAS = """
        SELECT id_marca, nombre
        FROM marca
        ORDER BY nombre
        """;

    private static final String SQL_PROVEEDORES = """
        SELECT id_proveedor, nombre
        FROM proveedor
        WHERE activo
        ORDER BY nombre
        """;

    private static final String SQL_SUCURSALES = """
        SELECT id_sucursal, nombre
        FROM sucursal
        WHERE activo
        ORDER BY nombre
        """;

    private static final String SQL_LISTAR_PRODUCTOS = """
        SELECT
            p.id_producto,
            p.nombre,
            c.nombre AS categoria,
            u.nombre AS unidad,
            p.precio_referencia
        FROM producto p
        JOIN categoria c
            ON c.id_categoria = p.id_categoria
        JOIN unidad u
            ON u.id_unidad = p.id_unidad
        WHERE p.activo
        ORDER BY c.nombre, p.nombre
        """;

    private static final String SQL_OBTENER_PROVEEDOR = """
        SELECT nombre, email
        FROM proveedor
        WHERE id_proveedor = :i
        """;

    private static final String SQL_ACTUALIZAR_PRECIO = """
        UPDATE producto
        SET precio_referencia = :p
        WHERE id_producto = :i
        """;

    public CatalogoRepository(JdbcClient db) {
        super(db);
    }

    // ==================================================
    // CONSULTA GENERAL DE CATÁLOGOS
    // ==================================================

    /**
     * Obtiene la información general de los catálogos.
     */
    public Map<String, Object> todo() {

        Map<String, Object> catalogos = new HashMap<>();

        catalogos.put("categorias", rows(SQL_CATEGORIAS, m()));
        catalogos.put("productos", rows(SQL_PRODUCTOS, m()));
        catalogos.put("unidades", rows(SQL_UNIDADES, m()));
        catalogos.put("marcas", rows(SQL_MARCAS, m()));
        catalogos.put("proveedores", rows(SQL_PROVEEDORES, m()));
        catalogos.put("sucursales", rows(SQL_SUCURSALES, m()));

        return catalogos;
    }

    // ==================================================
    // PROVEEDORES
    // ==================================================

    /**
     * Consulta los datos básicos de un proveedor.
     */
    public Map<String, Object> proveedor(long id) {

        return uno(
            SQL_OBTENER_PROVEEDOR,
            m("i", id)
        );
    }

    // ==================================================
    // ADMINISTRACIÓN DE CATÁLOGOS
    // ==================================================

    /**
     * Agrega un elemento a una tabla de parametrización.
     *
     * Solo permite las tablas definidas en la lista blanca.
     */
    public void agregar(String tabla, String nombre) {

        validarNombre(nombre);

        if (!TABLAS_PERMITIDAS.contains(tabla)) {
            throw new IllegalArgumentException(
                "Catálogo no permitido: " + tabla
            );
        }

        String sql = "INSERT INTO " + tabla + " (nombre) VALUES (:n)";

        exec(sql, m("n", nombre.trim()));
    }

    // ==================================================
    // PRODUCTOS
    // ==================================================

    /**
     * Registra un producto y crea su inventario inicial
     * en las sucursales activas.
     *
     * Ambas operaciones se ejecutan en una transacción.
     */
    @Transactional
    public void agregarProducto(
            String nombre,
            long idCategoria,
            long idUnidad,
            double precio) {

        validarNombre(nombre);
        validarPrecio(precio);

        long idProducto = insert(
            """
            INSERT INTO producto (
                nombre,
                id_categoria,
                id_unidad,
                precio_referencia
            )
            VALUES (:n, :c, :u, :p)
            """,
            m(
                "n", nombre.trim(),
                "c", idCategoria,
                "u", idUnidad,
                "p", precio
            ),
            "id_producto"
        );

        crearInventarioInicial(idProducto);
    }

    /**
     * Crea el inventario inicial del producto
     * en todas las sucursales activas.
     */
    private void crearInventarioInicial(long idProducto) {

        exec(
            """
            INSERT INTO inventario (
                id_sucursal,
                id_producto,
                cantidad_actual,
                cantidad_referencia
            )
            SELECT
                s.id_sucursal,
                :p,
                100,
                100
            FROM sucursal s
            WHERE s.activo
            """,
            m("p", idProducto)
        );
    }

    /**
     * Consulta los productos activos con su categoría
     * y unidad de medida.
     */
    public List<Map<String, Object>> productos() {

        return rows(SQL_LISTAR_PRODUCTOS, m());
    }

    /**
     * Actualiza el precio de referencia de un producto.
     */
    public void precio(long id, double precio) {

        validarPrecio(precio);

        exec(
            SQL_ACTUALIZAR_PRECIO,
            m(
                "p", precio,
                "i", id
            )
        );
    }

    // ==================================================
    // VALIDACIONES
    // ==================================================

    private void validarNombre(String nombre) {

        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException(
                "El nombre no puede estar vacío."
            );
        }
    }

    private void validarPrecio(double precio) {

        if (!Double.isFinite(precio) || precio < 0) {
            throw new IllegalArgumentException(
                "El precio debe ser un número válido mayor o igual a cero."
            );
        }
    }
}

