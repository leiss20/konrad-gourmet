
package co.konradlorenz.abastecimiento.repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Repository encargado de consultar los indicadores BAM
 * (Business Activity Monitoring) del sistema.
 */
@Repository
public class BamRepository extends BaseRepository {

    public BamRepository(JdbcClient db) {
        super(db);
    }

    /**
     * Obtiene los indicadores principales del sistema:
     * - Demanda de productos de los últimos 30 días.
     * - Sucursal con mayor actividad.
     * - Proveedor con mejor relación de precios.
     * - Producto con mayor demanda.
     */
    public Map<String, Object> kpis() {

        Map<String, Object> resultado = new HashMap<>();

        // 1. Consultar la demanda de productos de los últimos 30 días.
        List<Map<String, Object>> demanda = rows(
            "SELECT p.nombre, SUM(sd.cantidad)::float8 AS total " +
            "FROM solicitud_detalle sd " +
            "JOIN solicitud s ON s.id_solicitud = sd.id_solicitud " +
            "JOIN producto p ON p.id_producto = sd.id_producto " +
            "WHERE s.fecha_solicitud >= now() - interval '30 days' " +
            "GROUP BY p.nombre " +
            "ORDER BY total DESC",
            m()
        );

        resultado.put("demanda", demanda);

        // 2. Consultar la sucursal con mayor actividad.
        var restaurante = uno(
            "SELECT su.nombre FROM (" +
            "SELECT id_sucursal FROM pedido " +
            "WHERE fecha_hora >= now() - interval '30 days' " +
            "UNION ALL " +
            "SELECT id_sucursal FROM solicitud " +
            "WHERE fecha_solicitud >= now() - interval '30 days'" +
            ") x " +
            "JOIN sucursal su ON su.id_sucursal = x.id_sucursal " +
            "GROUP BY su.nombre " +
            "ORDER BY count(*) DESC " +
            "LIMIT 1",
            m()
        );

        // 3. Consultar el proveedor con mejor relación de precios.
        var proveedor = uno(
            "SELECT pr.nombre FROM cotizacion c " +
            "JOIN proveedor pr ON pr.id_proveedor = c.id_proveedor " +
            "WHERE c.valor_total_proveedor IS NOT NULL " +
            "AND c.fecha_envio >= now() - interval '1 year' " +
            "GROUP BY pr.nombre " +
            "ORDER BY avg(c.valor_total_proveedor / " +
            "NULLIF(c.valor_total_ministerio, 0)) " +
            "LIMIT 1",
            m()
        );

        // 4. Guardar la sucursal con mayor actividad.
        resultado.put(
            "restauranteTop",
            restaurante == null
                ? "—"
                : restaurante.get("nombre")
        );

        // 5. Guardar el proveedor con mejor relación de precios.
        resultado.put(
            "proveedorTop",
            proveedor == null
                ? "—"
                : proveedor.get("nombre")
        );

        // 6. Identificar el producto con mayor demanda.
        String productoTop = demanda.isEmpty()
            ? "—"
            : String.valueOf(demanda.get(0).get("nombre"));

        resultado.put("productoTop", productoTop);

        return resultado;
    }
}
