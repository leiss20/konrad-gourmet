package co.konradlorenz.abastecimiento.repository;
import java.util.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
@Repository public class SolicitudRepository extends BaseRepository {
  public SolicitudRepository(JdbcClient db) { super(db); }
  public long crear(long sucursal, long solicitante, List<Map<String, Object>> items) {
    long id = insert("INSERT INTO solicitud(id_sucursal,id_solicitante) VALUES(:s,:u)", m("s", sucursal, "u", solicitante), "id_solicitud");
    exec("UPDATE solicitud SET numero_solicitud=:n WHERE id_solicitud=:i", m("n", String.format("S-%04d", id), "i", id));
    for (var i : items) exec("INSERT INTO solicitud_detalle(id_solicitud,id_producto,id_marca,cantidad,id_unidad) VALUES(:s,:p,:m,:c,:u)",
      m("s", id, "p", n(i, "idProducto"), "m", i.get("idMarca") == null ? null : n(i, "idMarca"), "c", d(i, "cantidad"), "u", n(i, "idUnidad")));
    return id;
  }
  public List<Map<String, Object>> listar(Long sucursal) {
    return rows("SELECT s.id_solicitud,s.numero_solicitud,su.nombre AS sucursal,s.fecha_solicitud,s.estado,(SELECT string_agg(p.nombre||' '||sd.cantidad::float8||' '||u.nombre,', ') FROM solicitud_detalle sd JOIN producto p ON p.id_producto=sd.id_producto JOIN unidad u ON u.id_unidad=sd.id_unidad WHERE sd.id_solicitud=s.id_solicitud) AS productos FROM solicitud s JOIN sucursal su ON su.id_sucursal=s.id_sucursal"
      + (sucursal == null ? "" : " WHERE s.id_sucursal=:s") + " ORDER BY s.id_solicitud DESC", m("s", sucursal));
  }
  public List<Map<String, Object>> lineas(long idSolicitud) {
    return rows("SELECT sd.id_solicitud_detalle,sd.id_producto,p.nombre AS producto,sd.id_unidad,u.nombre AS unidad,sd.cantidad FROM solicitud_detalle sd JOIN producto p ON p.id_producto=sd.id_producto JOIN unidad u ON u.id_unidad=sd.id_unidad WHERE sd.id_solicitud=:i", m("i", idSolicitud));
  }
  public void estado(long id, String estado) { exec("UPDATE solicitud SET estado=:e WHERE id_solicitud=:i", m("e", estado, "i", id)); }
}
