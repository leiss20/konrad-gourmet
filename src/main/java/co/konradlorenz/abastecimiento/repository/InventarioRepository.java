package co.konradlorenz.abastecimiento.repository;
import java.util.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
@Repository public class InventarioRepository extends BaseRepository {
  public InventarioRepository(JdbcClient db) { super(db); }
  public List<Map<String, Object>> listar(Long sucursal) {
    return rows("SELECT i.id_inventario,p.nombre AS producto,u.nombre AS unidad,i.cantidad_actual,i.cantidad_referencia,round(i.cantidad_actual*100/NULLIF(i.cantidad_referencia,0),1) AS porcentaje FROM inventario i JOIN producto p ON p.id_producto=i.id_producto JOIN unidad u ON u.id_unidad=p.id_unidad"
      + (sucursal == null ? "" : " WHERE i.id_sucursal=:s") + " ORDER BY porcentaje,p.nombre", m("s", sucursal));
  }
  /** Descuenta lo consumido por los pedidos sin procesar (ingredientes del plato × cantidad pedida) y los marca como procesados. */
  public int descontar() {
    exec("UPDATE inventario i SET cantidad_actual=GREATEST(0,i.cantidad_actual-c.total),ultima_actualizacion=now() FROM (SELECT pe.id_sucursal,pi.id_producto,SUM(pd.cantidad*pi.cantidad*CASE WHEN u.nombre='Libra' THEN 0.5 ELSE 1 END) AS total "
      + "FROM pedido pe JOIN pedido_detalle pd ON pd.id_pedido=pe.id_pedido JOIN plato_ingrediente pi ON pi.id_plato=pd.id_plato JOIN unidad u ON u.id_unidad=pi.id_unidad WHERE pe.estado='Registrado' GROUP BY pe.id_sucursal,pi.id_producto) c "
      + "WHERE i.id_sucursal=c.id_sucursal AND i.id_producto=c.id_producto", m());
    return db.sql("UPDATE pedido SET estado='Procesado' WHERE estado='Registrado'").update();
  }
  public void reiniciarAlarmas() { exec("UPDATE inventario SET alarma_enviada=false WHERE alarma_enviada AND cantidad_actual>cantidad_referencia*0.25", m()); }
  public List<Map<String, Object>> alarmasNuevas() {
    return rows("SELECT i.id_inventario,i.id_sucursal,s.nombre AS sucursal,p.nombre AS producto,i.cantidad_actual,i.cantidad_referencia FROM inventario i JOIN sucursal s ON s.id_sucursal=i.id_sucursal JOIN producto p ON p.id_producto=i.id_producto WHERE i.cantidad_actual<=i.cantidad_referencia*0.25 AND NOT i.alarma_enviada", m());
  }
  public void marcar(long id) { exec("UPDATE inventario SET alarma_enviada=true WHERE id_inventario=:i", m("i", id)); }
}