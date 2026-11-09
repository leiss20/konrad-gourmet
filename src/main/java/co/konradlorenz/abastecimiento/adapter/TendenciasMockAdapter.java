package co.konradlorenz.abastecimiento.adapter;
import java.util.List;
import org.springframework.stereotype.Component;
@Component public class TendenciasMockAdapter implements TendenciasAdapter {
  public List<String> tendencias() { return List.of("Jugos naturales", "Comida saludable", "Platos con proteína vegetal"); }
}