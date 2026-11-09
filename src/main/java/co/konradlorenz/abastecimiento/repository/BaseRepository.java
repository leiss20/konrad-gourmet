package co.konradlorenz.abastecimiento.repository;
import java.util.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
public abstract class BaseRepository {
  protected final JdbcClient db;
  protected BaseRepository(JdbcClient db) { this.db = db; }
  protected static Map<String, Object> m(Object... kv) { Map<String, Object> r = new HashMap<>(); for (int i = 0; i < kv.length; i += 2) r.put((String) kv[i], kv[i + 1]); return r; }
  protected static long n(Map<String, Object> x, String k) { return ((Number) x.get(k)).longValue(); }
  protected static double d(Map<String, Object> x, String k) { return ((Number) x.get(k)).doubleValue(); }
  protected long insert(String sql, Map<String, Object> p, String key) { KeyHolder kh = new GeneratedKeyHolder(); db.sql(sql).params(p).update(kh, key); return kh.getKey().longValue(); }
  protected List<Map<String, Object>> rows(String sql, Map<String, Object> p) { return db.sql(sql).params(p).query().listOfRows(); }
  protected void exec(String sql, Map<String, Object> p) { db.sql(sql).params(p).update(); }
  protected Map<String, Object> uno(String sql, Map<String, Object> p) { var r = rows(sql, p); return r.isEmpty() ? null : r.get(0); }
}
