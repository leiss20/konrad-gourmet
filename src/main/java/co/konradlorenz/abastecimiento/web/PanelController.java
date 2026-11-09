package co.konradlorenz.abastecimiento.web;
import co.konradlorenz.abastecimiento.facade.AdminFacade;
import java.security.Principal;
import java.util.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api")
public class PanelController {
  record NombreReq(String nombre) {}
  record TemaReq(String color) {}
  record ProductoReq(String nombre, long idCategoria, long idUnidad, double precio) {}
  record PrecioReq(double precio) {}
  private final AdminFacade f;
  public PanelController(AdminFacade f) { this.f = f; }
  @GetMapping("/me") Map<String, Object> me(Principal p) { return f.me(p.getName()); }
  @GetMapping("/catalogo") Map<String, Object> catalogo() { return f.catalogo(); }
  @GetMapping("/tema") Map<String, String> tema() { return Map.of("color", f.tema()); }
  @GetMapping("/bam") @PreAuthorize("hasRole('DIRECTOR_COMERCIAL')") public Map<String, Object> bam() { return f.bam(); }
  @PostMapping("/admin/catalogo/{tabla}") @PreAuthorize("hasRole('ADMIN')") public void agregar(@PathVariable String tabla, @RequestBody NombreReq r, Principal p) { f.agregar(p.getName(), tabla, r.nombre()); }
  @PutMapping("/admin/tema") @PreAuthorize("hasRole('ADMIN')") public void tema(@RequestBody TemaReq r, Principal p) { f.tema(p.getName(), r.color()); }
  @GetMapping("/admin/auditoria") @PreAuthorize("hasRole('ADMIN')") public List<Map<String, Object>> auditoria() { return f.auditoria(); }
  @GetMapping("/admin/logs") @PreAuthorize("hasRole('ADMIN')") public List<Map<String, Object>> logs() { return f.errores(); }
  @GetMapping("/admin/productos") @PreAuthorize("hasRole('ADMIN')") public List<Map<String, Object>> productos() { return f.productos(); }
  @GetMapping("/inventario") @PreAuthorize("hasRole('JEFE_COCINA')") public List<Map<String, Object>> inventario(Principal p) { return f.inventario(p.getName()); }
  @PostMapping("/admin/inventario/procesar") @PreAuthorize("hasRole('ADMIN')") public Map<String, String> procesar() { return Map.of("resultado", f.procesarInventario()); }
  @PostMapping("/admin/promociones/enviar") @PreAuthorize("hasRole('ADMIN')") public Map<String, String> promociones() { return Map.of("resultado", f.enviarPromociones()); }
  @PostMapping("/admin/productos") @PreAuthorize("hasRole('ADMIN')") public void producto(@RequestBody ProductoReq r, Principal p) { f.agregarProducto(p.getName(), r.nombre(), r.idCategoria(), r.idUnidad(), r.precio()); }
  @PutMapping("/admin/productos/{id}/precio") @PreAuthorize("hasRole('ADMIN')") public void precio(@PathVariable long id, @RequestBody PrecioReq r, Principal p) { f.precioProducto(p.getName(), id, r.precio()); }
}
