package co.konradlorenz.abastecimiento.repository;
import java.util.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
@Repository public class CatalogoRepository extends BaseRepository {
  public CatalogoRepository(JdbcClient db) { super(db); }
  public Map<String, Object> todo() {
    Map<String, Object> r = new HashMap<>(); Map<String, Object> v = Map.of();
    r.put("categorias", rows("SELECT id_categoria,nombre FROM categoria ORDER BY nombre", v));
    r.put("productos", rows("SELECT p.id_producto,p.nombre,p.id_categoria,p.id_unidad FROM producto p WHERE p.activo ORDER BY p.nombre", v));
    r.put("unidades", rows("SELECT id_unidad,nombre FROM unidad ORDER BY id_unidad", v));
    r.put("marcas", rows("SELECT id_marca,nombre FROM marca ORDER BY nombre", v));
    r.put("proveedores", rows("SELECT id_proveedor,nombre FROM proveedor WHERE activo ORDER BY nombre", v));
    r.put("sucursales", rows("SELECT id_sucursal,nombre FROM sucursal WHERE activo ORDER BY nombre", v));
    return r;
  }
  public Map<String, Object> proveedor(long id) { return uno("SELECT nombre,email FROM proveedor WHERE id_proveedor=:i", m("i", id)); }
  /** Solo tablas de parametrización permitidas (lista blanca). */
  public void agregar(String tabla, String nombre) {
    if (!List.of("categoria", "marca", "unidad").contains(tabla)) throw new IllegalArgumentException("Catálogo no permitido.");
    exec("INSERT INTO " + tabla + "(nombre) VALUES(:n)", m("n", nombre));
  }
      public void agregarProducto(String nombre, long idCategoria, long idUnidad, double precio) {
    long id = insert("INSERT INTO producto(nombre,id_categoria,id_unidad,precio_referencia) VALUES(:n,:c,:u,:p)", m("n", nombre, "c", idCategoria, "u", idUnidad, "p", precio), "id_producto");
    exec("INSERT INTO inventario(id_sucursal,id_producto,cantidad_actual,cantidad_referencia) SELECT id_sucursal,CAST(:p AS int),100,100 FROM sucursal", m("p", id));
  }
  public List<Map<String, Object>> productos() {
    return rows("SELECT p.id_producto,p.nombre,c.nombre AS categoria,u.nombre AS unidad,p.precio_referencia FROM producto p JOIN categoria c ON c.id_categoria=p.id_categoria JOIN unidad u ON u.id_unidad=p.id_unidad WHERE p.activo ORDER BY c.nombre,p.nombre", m());
  }

  public void precio(long id, double precio) { exec("UPDATE producto SET precio_referencia=:p WHERE id_producto=:i", m("p", precio, "i", id)); }
    
}
