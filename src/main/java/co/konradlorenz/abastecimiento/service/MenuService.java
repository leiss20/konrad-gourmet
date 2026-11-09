package co.konradlorenz.abastecimiento.service;
import co.konradlorenz.abastecimiento.repository.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service @Transactional
public class MenuService {
  private final PlatoRepository platos; private final PedidoRepository pedidos; private final UsuarioRepository usuarios; private final AuditoriaService auditoria;
  public MenuService(PlatoRepository pl, PedidoRepository pe, UsuarioRepository u, AuditoriaService a) { platos = pl; pedidos = pe; usuarios = u; auditoria = a; }
  private Map<String, Object> yo(String email) { return usuarios.porEmail(email); }
  public long crearPlato(String email, String nombre, double precio, List<Map<String, Object>> ing) {
    if (nombre == null || nombre.isBlank() || precio <= 0 || ing == null || ing.isEmpty()) throw new IllegalArgumentException("Ingresa nombre, precio mayor que cero y al menos un ingrediente.");
    if (ing.stream().map(i -> i.get("idProducto")).distinct().count() != ing.size()) throw new IllegalArgumentException("Un producto no puede repetirse en el mismo plato.");
    long id = platos.crear(nombre.trim(), precio, ((Number) yo(email).get("id_sucursal")).longValue(), ing);
    auditoria.registrar(email, "CREAR", "plato", id, nombre); return id;
  }
  public long registrarPedido(String email, int mesa, List<Map<String, Object>> items) {
    if (mesa <= 0 || items == null || items.isEmpty() || items.stream().anyMatch(i -> ((Number) i.get("cantidad")).intValue() <= 0)) throw new IllegalArgumentException("Indica la mesa y al menos un plato con cantidad mayor que cero.");
    Map<String, Object> u = yo(email); long id = pedidos.crear(mesa, ((Number) u.get("id_sucursal")).longValue(), ((Number) u.get("id_usuario")).longValue(), items);
    auditoria.registrar(email, "CREAR", "pedido", id, "Mesa " + mesa); return id;
  }
  @Transactional(readOnly = true) public List<Map<String, Object>> platos(String email) { return platos.listar(((Number) yo(email).get("id_sucursal")).longValue()); }
  @Transactional(readOnly = true) public List<Map<String, Object>> pedidos(String email) { return pedidos.listar(((Number) yo(email).get("id_sucursal")).longValue()); }
}
