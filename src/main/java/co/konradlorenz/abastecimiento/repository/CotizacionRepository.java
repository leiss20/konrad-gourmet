package co.konradlorenz.abastecimiento.repository;
import java.time.LocalDate;
import java.util.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
@Repository public class CotizacionRepository extends BaseRepository {
  public CotizacionRepository(JdbcClient db) { super(db); }
  public long crear(long proveedor, LocalDate plazo, double totalMinisterio) {
    return insert("INSERT INTO cotizacion(id_proveedor,fecha_maxima_plazo,valor_total_ministerio) VALUES(:p,:f,:t)", m("p", proveedor, "f", java.sql.Date.valueOf(plazo), "t", totalMinisterio), "id_cotizacion");
  }
  public void detalle(long cot, Map<String, Object> l, double precioMinisterio) {
    exec("INSERT INTO cotizacion_detalle(id_cotizacion,id_solicitud_detalle,id_producto,id_unidad,cantidad,precio_ministerio) VALUES(:c,:sd,:p,:u,:q,:pm)",
      m("c", cot, "sd", n(l, "id_solicitud_detalle"), "p", n(l, "id_producto"), "u", n(l, "id_unidad"), "q", d(l, "cantidad"), "pm", precioMinisterio));
  }
  public List<Map<String, Object>> listar() {
    return rows("SELECT c.id_cotizacion,pr.nombre AS proveedor,c.estado,c.valor_total_proveedor,c.valor_total_ministerio,c.fecha_maxima_plazo,"
      + "(SELECT s.numero_solicitud FROM cotizacion_detalle cd JOIN solicitud_detalle sd ON sd.id_solicitud_detalle=cd.id_solicitud_detalle JOIN solicitud s ON s.id_solicitud=sd.id_solicitud WHERE cd.id_cotizacion=c.id_cotizacion LIMIT 1) AS solicitud,"
      + "(SELECT o.numero_orden FROM orden_compra o WHERE o.id_cotizacion=c.id_cotizacion LIMIT 1) AS orden FROM cotizacion c JOIN proveedor pr ON pr.id_proveedor=c.id_proveedor ORDER BY c.id_cotizacion DESC", m());
  }
  public Map<String, Object> porId(long id) { return uno("SELECT c.*,(SELECT count(*) FROM orden_compra o WHERE o.id_cotizacion=c.id_cotizacion) AS ordenes FROM cotizacion c WHERE c.id_cotizacion=:i", m("i", id)); }
  /** Guarda el valor del proveedor y lo reparte proporcionalmente entre las líneas. */
  public void validar(long id, double valor, String estado, double ratio) {
    exec("UPDATE cotizacion SET valor_total_proveedor=:v,estado=:e WHERE id_cotizacion=:i", m("v", valor, "e", estado, "i", id));
    exec("UPDATE cotizacion_detalle SET precio_unitario=precio_ministerio*:r WHERE id_cotizacion=:i", m("r", ratio, "i", id));
  }
  public String ordenar(long id, long proveedor, double total) {
    long o = insert("INSERT INTO orden_compra(id_cotizacion,id_proveedor,total) VALUES(:c,:p,:t)", m("c", id, "p", proveedor, "t", total), "id_orden");
    String num = String.format("OC-%04d", o);
    exec("UPDATE orden_compra SET numero_orden=:n WHERE id_orden=:o", m("n", num, "o", o));
    exec("INSERT INTO orden_compra_detalle(id_orden,id_producto,id_unidad,cantidad,precio_unitario,subtotal) SELECT CAST(:o AS int),id_producto,id_unidad,cantidad,precio_unitario,cantidad*precio_unitario FROM cotizacion_detalle WHERE id_cotizacion=:c", m("o", o, "c", id));
    return num;
  }
  public List<Map<String, Object>> ordenes() {
    return rows("SELECT o.numero_orden,o.fecha_generacion,pr.nombre AS proveedor,o.estado,o.total FROM orden_compra o JOIN proveedor pr ON pr.id_proveedor=o.id_proveedor ORDER BY o.id_orden DESC", m());
  }
}
