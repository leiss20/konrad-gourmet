
package co.konradlorenz.abastecimiento.repository;

import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Repositorio encargado de gestionar las operaciones
 * de auditoría del sistema.
 *
 * Registra acciones de usuarios, consulta el historial
 * y conserva métodos de compatibilidad para errores
 * y parámetros visuales.
 */
@Repository
public class AuditoriaRepository extends BaseRepository {

    private static final int LIMITE_REGISTROS = 200;

    private static final String SQL_REGISTRAR_AUDITORIA = """
        INSERT INTO auditoria (
            id_usuario,
            accion,
            tabla_afectada,
            registro_id,
            detalle
        )
        VALUES (:u, :a, :t, :r, :d)
        """;

    private static final String SQL_LISTAR_AUDITORIA = """
        SELECT
            a.fecha_hora,
            COALESCE(u.email, 'sistema') AS usuario,
            a.accion,
            a.tabla_afectada,
            a.registro_id,
            a.detalle
        FROM auditoria a
        LEFT JOIN usuario u
            ON u.id_usuario = a.id_usuario
        ORDER BY a.id_auditoria DESC
        LIMIT 200
        """;

    private static final String SQL_REGISTRAR_ERROR = """
        INSERT INTO log_error (
            modulo,
            mensaje,
            stack_trace
        )
        VALUES (:m, :s, :t)
        """;

    private static final String SQL_LISTAR_ERRORES = """
        SELECT
            fecha_hora,
            modulo,
            mensaje
        FROM log_error
        ORDER BY id_log DESC
        LIMIT 200
        """;

    private static final String SQL_OBTENER_TEMA = """
        SELECT valor
        FROM parametro
        WHERE clave = 'color_primario'
        """;

    private static final String SQL_ACTUALIZAR_TEMA = """
        UPDATE parametro
        SET valor = :v
        WHERE clave = 'color_primario'
        """;

    private static final String COLOR_TEMA_PREDETERMINADO = "#d6006b";

    public AuditoriaRepository(JdbcClient db) {
        super(db);
    }

    // ==================================================
    // AUDITORÍA
    // ==================================================

    /**
     * Registra una acción realizada por un usuario.
     */
    public void registrar(
            Long usuario,
            String accion,
            String tabla,
            Long registro,
            String detalle) {

        exec(
            SQL_REGISTRAR_AUDITORIA,
            m(
                "u", usuario,
                "a", accion,
                "t", tabla,
                "r", registro,
                "d", detalle
            )
        );
    }

    /**
     * Obtiene los últimos registros de auditoría.
     */
    public List<Map<String, Object>> listar() {

        return rows(SQL_LISTAR_AUDITORIA, m());
    }

    // ==================================================
    // REGISTRO DE ERRORES
    // ==================================================

    /**
     * Registra un error producido por un módulo del sistema.
     */
    public void error(
            String modulo,
            String mensaje,
            String stack) {

        exec(
            SQL_REGISTRAR_ERROR,
            m(
                "m", modulo,
                "s", mensaje,
                "t", stack
            )
        );
    }

    /**
     * Consulta los últimos errores registrados.
     */
    public List<Map<String, Object>> errores() {

        return rows(SQL_LISTAR_ERRORES, m());
    }

    // ==================================================
    // CONFIGURACIÓN VISUAL
    // ==================================================

    /**
     * Obtiene el color principal de la aplicación.
     *
     * Si no existe una configuración, devuelve
     * el color predeterminado.
     */
    public String tema() {

        Map<String, Object> resultado = uno(
            SQL_OBTENER_TEMA,
            m()
        );

        if (resultado == null || resultado.get("valor") == null) {
            return COLOR_TEMA_PREDETERMINADO;
        }

        return resultado.get("valor").toString();
    }

    /**
     * Actualiza el color principal de la aplicación.
     */
    public void tema(String color) {

        exec(
            SQL_ACTUALIZAR_TEMA,
            m("v", color)
        );
    }
}
