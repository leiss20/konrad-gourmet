package co.konradlorenz.abastecimiento.service;
import co.konradlorenz.abastecimiento.repository.*;
import java.util.*;
import org.springframework.stereotype.Service;
@Service public class AdminService {
  private final CatalogoRepository catalogo; private final AuditoriaRepository repo; private final UsuarioRepository usuarios; private final BamRepository bam; private final AuditoriaService auditoria;
  public AdminService(CatalogoRepository c, AuditoriaRepository r, UsuarioRepository u, BamRepository b, AuditoriaService a) { catalogo = c; repo = r; usuarios = u; bam = b; auditoria = a; }
  public Map<String, Object> me(String email) { Map<String, Object> u = new HashMap<>(usuarios.porEmail(email)); u.remove("password_hash"); return u; }
  public Map<String, Object> catalogo() { return catalogo.todo(); }
  public void agregar(String email, String tabla, String nombre) {
    if (nombre == null || nombre.isBlank()) throw new IllegalArgumentException("Escribe un nombre.");
    catalogo.agregar(tabla, nombre.trim()); auditoria.registrar(email, "CREAR", tabla, null, nombre);
  }
  public String tema() { return repo.tema(); }
  public void tema(String email, String color) { if (color == null || !color.matches("#[0-9a-fA-F]{6}")) throw new IllegalArgumentException("Color inválido."); repo.tema(color); auditoria.registrar(email, "ACTUALIZAR", "parametro", null, color); }
  public List<Map<String, Object>> auditoria() { return repo.listar(); }
  public List<Map<String, Object>> errores() { return repo.errores(); }
  public Map<String, Object> kpis() { return bam.kpis(); }
    public List<Map<String, Object>> productos() { return catalogo.productos(); }
  public void agregarProducto(String email, String nombre, long idCategoria, long idUnidad, double precio) {
    if (nombre == null || nombre.isBlank() || precio <= 0) throw new IllegalArgumentException("Escribe el nombre y un precio mayor que cero.");
    catalogo.agregarProducto(nombre.trim(), idCategoria, idUnidad, precio); auditoria.registrar(email, "CREAR", "producto", null, nombre);
  }
  public void precioProducto(String email, long id, double precio) {
    if (precio <= 0) throw new IllegalArgumentException("El precio debe ser mayor que cero.");
    catalogo.precio(id, precio); auditoria.registrar(email, "ACTUALIZAR", "producto", id, "precio " + precio);
  }
}
