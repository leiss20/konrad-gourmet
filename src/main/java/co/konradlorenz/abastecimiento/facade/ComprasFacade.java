package co.konradlorenz.abastecimiento.facade;
import co.konradlorenz.abastecimiento.service.ComprasService;
import java.time.LocalDate;
import java.util.*;
import org.springframework.stereotype.Component;
/** Patrón Facade: punto de entrada del área de compras. La capa web no conoce servicios, repositorios ni adaptadores. */
@Component public class ComprasFacade {
  private final ComprasService c;
  public ComprasFacade(ComprasService c) { this.c = c; }
  public long solicitar(String e, List<Map<String, Object>> i) { return c.solicitar(e, i); }
  public List<Map<String, Object>> solicitudes(String e) { return c.solicitudes(e); }
  public long cotizar(String e, long s, long p, LocalDate f) { return c.cotizar(e, s, p, f); }
  public String validar(String e, long id, double v) { return c.validar(e, id, v); }
  public String ordenar(String e, long id) { return c.ordenar(e, id); }
  public List<Map<String, Object>> cotizaciones() { return c.cotizaciones(); }
  public List<Map<String, Object>> ordenes() { return c.ordenes(); }
}
