package co.konradlorenz.abastecimiento.repository;
import java.util.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
@Repository public class PlatoRepository extends BaseRepository {
  public PlatoRepository(JdbcClient db) { super(db); }
  public long crear(String nombre, double precio, long sucursal, List<Map<String, Object>> ingredientes) {
    long id = insert("INSERT INTO plato(nombre,precio_venta,id_sucursal) VALUES(:n,:p,:s)", m("n", nombre, "p", precio, "s", sucursal), "id_plato");
    for (var i : ingredientes) exec("INSERT INTO plato_ingrediente(id_plato,id_producto,cantidad,id_unidad) VALUES(:pl,:pr,:c,:u)", m("pl", id, "pr", n(i, "idProducto"), "c", d(i, "cantidad"), "u", n(i, "idUnidad")));
    return id;
  }
  public List<Map<String, Object>> listar(Long sucursal) {
    return rows("SELECT pl.id_plato,pl.nombre,pl.precio_venta,s.nombre AS sucursal,(SELECT string_agg(p.nombre||' '||pi.cantidad::float8||' '||u.nombre,', ') FROM plato_ingrediente pi JOIN producto p ON p.id_producto=pi.id_producto JOIN unidad u ON u.id_unidad=pi.id_unidad WHERE pi.id_plato=pl.id_plato) AS ingredientes FROM plato pl JOIN sucursal s ON s.id_sucursal=pl.id_sucursal WHERE pl.activo"
      + (sucursal == null ? "" : " AND pl.id_sucursal=:s") + " ORDER BY pl.nombre", m("s", sucursal));
  }
}
