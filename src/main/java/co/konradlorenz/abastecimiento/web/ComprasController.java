package co.konradlorenz.abastecimiento.web;
import co.konradlorenz.abastecimiento.facade.ComprasFacade;
import java.security.Principal;
import java.time.LocalDate;
import java.util.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/compras")
public class ComprasController {
  record SolicitudReq(List<Map<String, Object>> items) {}
  record CotizarReq(long idProveedor, LocalDate plazo) {}
  record ValidarReq(double valor) {}
  private final ComprasFacade f;
  public ComprasController(ComprasFacade f) { this.f = f; }
  @GetMapping("/solicitudes") @PreAuthorize("hasAnyRole('JEFE_COCINA','AUXILIAR_COMPRAS')") public List<Map<String, Object>> solicitudes(Principal p) { return f.solicitudes(p.getName()); }
  @PostMapping("/solicitudes") @PreAuthorize("hasRole('JEFE_COCINA')") public Map<String, Object> solicitar(@RequestBody SolicitudReq r, Principal p) { return Map.of("id", f.solicitar(p.getName(), r.items())); }
  @PostMapping("/solicitudes/{id}/cotizar") @PreAuthorize("hasRole('AUXILIAR_COMPRAS')") public Map<String, Object> cotizar(@PathVariable long id, @RequestBody CotizarReq r, Principal p) { return Map.of("id", f.cotizar(p.getName(), id, r.idProveedor(), r.plazo())); }
  @GetMapping("/cotizaciones") @PreAuthorize("hasRole('DIRECTOR_COMPRAS')") public List<Map<String, Object>> cotizaciones() { return f.cotizaciones(); }
  @PostMapping("/cotizaciones/{id}/validar") @PreAuthorize("hasRole('DIRECTOR_COMPRAS')") public Map<String, Object> validar(@PathVariable long id, @RequestBody ValidarReq r, Principal p) { return Map.of("estado", f.validar(p.getName(), id, r.valor())); }
  @PostMapping("/cotizaciones/{id}/orden") @PreAuthorize("hasRole('DIRECTOR_COMPRAS')") public Map<String, Object> orden(@PathVariable long id, Principal p) { return Map.of("orden", f.ordenar(p.getName(), id)); }
  @GetMapping("/ordenes") @PreAuthorize("hasRole('DIRECTOR_COMPRAS')") public List<Map<String, Object>> ordenes() { return f.ordenes(); }
}
