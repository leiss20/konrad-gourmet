package co.konradlorenz.abastecimiento.service;
import co.konradlorenz.abastecimiento.repository.*;
import java.util.Map;
import org.springframework.stereotype.Service;
/** RNF-MAN-01: cada acción queda con usuario, fecha y hora. */
@Service public class AuditoriaService {
  private final AuditoriaRepository repo; private final UsuarioRepository usuarios;
  public AuditoriaService(AuditoriaRepository r, UsuarioRepository u) { repo = r; usuarios = u; }
  public void registrar(String email, String accion, String tabla, Long registro, String detalle) {
    Map<String, Object> u = usuarios.porEmail(email); repo.registrar(u == null ? null : ((Number) u.get("id_usuario")).longValue(), accion, tabla, registro, detalle);
  }
}
