package co.konradlorenz.abastecimiento.web;
import co.konradlorenz.abastecimiento.facade.MenuFacade;
import java.security.Principal;
import java.util.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/menu")
public class MenuController {
  record PlatoReq(String nombre, double precio, List<Map<String, Object>> ingredientes) {}
  record PedidoReq(int mesa, List<Map<String, Object>> items) {}
  private final MenuFacade f;
  public MenuController(MenuFacade f) { this.f = f; }
  @GetMapping("/platos") @PreAuthorize("hasAnyRole('JEFE_COCINA','MESERO')") public List<Map<String, Object>> platos(Principal p) { return f.platos(p.getName()); }
  @PostMapping("/platos") @PreAuthorize("hasRole('JEFE_COCINA')") public Map<String, Object> plato(@RequestBody PlatoReq r, Principal p) { return Map.of("id", f.registrarPlato(p.getName(), r.nombre(), r.precio(), r.ingredientes())); }
  @GetMapping("/pedidos") @PreAuthorize("hasRole('MESERO')") public List<Map<String, Object>> pedidos(Principal p) { return f.pedidos(p.getName()); }
  @PostMapping("/pedidos") @PreAuthorize("hasRole('MESERO')") public Map<String, Object> pedido(@RequestBody PedidoReq r, Principal p) { return Map.of("id", f.registrarPedido(p.getName(), r.mesa(), r.items())); }
}
