package co.konradlorenz.abastecimiento.facade;
import co.konradlorenz.abastecimiento.service.MenuService;
import java.util.*;
import org.springframework.stereotype.Component;
/** Patrón Facade: punto de entrada del área de cocina (menú y pedidos). */
@Component public class MenuFacade {
  private final MenuService menu;
  public MenuFacade(MenuService m) { menu = m; }
  public long registrarPlato(String e, String n, double p, List<Map<String, Object>> i) { return menu.crearPlato(e, n, p, i); }
  public List<Map<String, Object>> platos(String e) { return menu.platos(e); }
  public long registrarPedido(String e, int mesa, List<Map<String, Object>> i) { return menu.registrarPedido(e, mesa, i); }
  public List<Map<String, Object>> pedidos(String e) { return menu.pedidos(e); }
}
