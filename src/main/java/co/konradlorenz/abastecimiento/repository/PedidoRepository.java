package co.konradlorenz.abastecimiento.repository;
import java.util.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
@Repository public class PedidoRepository extends BaseRepository {
  public PedidoRepository(JdbcClient db) { super(db); }
  public long crear(int mesa, long sucursal, long mesero, List<Map<String, Object>> items) {
    long id = insert("INSERT INTO pedido(numero_mesa,id_sucursal,id_mesero) VALUES(:m,:s,:u)", m("m", mesa, "s", sucursal, "u", mesero), "id_pedido");
    for (var i : items) exec("INSERT INTO pedido_detalle(id_pedido,id_plato,cantidad,precio_unitario) SELECT CAST(:p AS int),id_plato,CAST(:c AS int),precio_venta FROM plato WHERE id_plato=:pl", m("p", id, "c", n(i, "cantidad"), "pl", n(i, "idPlato")));
    return id;
  }
  public List<Map<String, Object>> listar(Long sucursal) {
    return rows("SELECT pe.id_pedido,pe.numero_mesa,s.nombre AS sucursal,pe.fecha_hora,pe.estado,(SELECT string_agg(pl.nombre||' x'||d.cantidad,', ') FROM pedido_detalle d JOIN plato pl ON pl.id_plato=d.id_plato WHERE d.id_pedido=pe.id_pedido) AS detalle FROM pedido pe JOIN sucursal s ON s.id_sucursal=pe.id_sucursal"
      + (sucursal == null ? "" : " WHERE pe.id_sucursal=:s") + " ORDER BY pe.id_pedido DESC", m("s", sucursal));
  }
}
