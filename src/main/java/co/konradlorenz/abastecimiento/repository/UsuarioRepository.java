package co.konradlorenz.abastecimiento.repository;
import java.util.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
@Repository public class UsuarioRepository extends BaseRepository {
    public List<String> correosJefes(long sucursal) { return db.sql("SELECT u.email FROM usuario u JOIN rol r ON r.id_rol=u.id_rol WHERE r.nombre='JEFE_COCINA' AND u.activo AND u.id_sucursal=:s").param("s", sucursal).query(String.class).list(); }
  public List<String> correosPorRol(String rol) { return db.sql("SELECT u.email FROM usuario u JOIN rol r ON r.id_rol=u.id_rol WHERE r.nombre=:r AND u.activo").param("r", rol).query(String.class).list(); }
  public UsuarioRepository(JdbcClient db) { super(db); }
  public Map<String, Object> porEmail(String e) { return uno("SELECT u.id_usuario,u.nombre,u.email,u.password_hash,u.activo,u.id_sucursal,r.nombre AS rol FROM usuario u JOIN rol r ON r.id_rol=u.id_rol WHERE u.email=:e", m("e", e)); }
  public int contar() { return db.sql("SELECT count(*) FROM usuario").query(Integer.class).single(); }
  public void crear(String nombre, String email, String hash, String rol, Long sucursal) {
    exec("INSERT INTO usuario(nombre,email,password_hash,id_rol,id_sucursal) VALUES(:n,:e,:h,(SELECT id_rol FROM rol WHERE nombre=:r),:s)", m("n", nombre, "e", email, "h", hash, "r", rol, "s", sucursal));
  }
}
