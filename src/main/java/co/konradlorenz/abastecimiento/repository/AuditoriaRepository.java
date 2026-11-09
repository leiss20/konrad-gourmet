package co.konradlorenz.abastecimiento.repository;
import java.util.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
@Repository public class AuditoriaRepository extends BaseRepository {
  public AuditoriaRepository(JdbcClient db) { super(db); }
  public void registrar(Long usuario, String accion, String tabla, Long registro, String detalle) {
    exec("INSERT INTO auditoria(id_usuario,accion,tabla_afectada,registro_id,detalle) VALUES(:u,:a,:t,:r,:d)", m("u", usuario, "a", accion, "t", tabla, "r", registro == null ? null : registro.intValue(), "d", detalle));
  }
  public List<Map<String, Object>> listar() { return rows("SELECT a.fecha_hora,COALESCE(u.email,'sistema') AS usuario,a.accion,a.tabla_afectada,a.registro_id,a.detalle FROM auditoria a LEFT JOIN usuario u ON u.id_usuario=a.id_usuario ORDER BY a.id_auditoria DESC LIMIT 200", m()); }
  public void error(String modulo, String mensaje, String stack) { exec("INSERT INTO log_error(modulo,mensaje,stack_trace) VALUES(:m,:s,:t)", m("m", modulo, "s", mensaje, "t", stack)); }
  public List<Map<String, Object>> errores() { return rows("SELECT fecha_hora,modulo,mensaje FROM log_error ORDER BY id_log DESC LIMIT 200", m()); }
  public String tema() { var r = uno("SELECT valor FROM parametro WHERE clave='color_primario'", m()); return r == null ? "#d6006b" : (String) r.get("valor"); }
  public void tema(String color) { exec("UPDATE parametro SET valor=:v WHERE clave='color_primario'", m("v", color)); }
}
