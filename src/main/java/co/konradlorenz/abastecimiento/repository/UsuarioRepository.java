
package co.konradlorenz.abastecimiento.repository;

import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Repositorio encargado de consultar y gestionar
 * los usuarios del sistema.
 */
@Repository
public class UsuarioRepository extends BaseRepository {

    private static final String SQL_CORREOS_JEFES = """
        SELECT u.email
        FROM usuario u
        JOIN rol r
            ON r.id_rol = u.id_rol
        WHERE r.nombre = 'JEFE_COCINA'
          AND u.activo = true
          AND u.id_sucursal = :s
          AND u.email IS NOT NULL
        ORDER BY u.email
        """;

    private static final String SQL_CORREOS_POR_ROL = """
        SELECT u.email
        FROM usuario u
        JOIN rol r
            ON r.id_rol = u.id_rol
        WHERE r.nombre = :r
          AND u.activo = true
          AND u.email IS NOT NULL
        ORDER BY u.email
        """;

    private static final String SQL_USUARIO_POR_EMAIL = """
        SELECT
            u.id_usuario,
            u.nombre,
            u.email,
            u.password_hash,
            u.activo,
            u.id_sucursal,
            r.nombre AS rol
        FROM usuario u
        JOIN rol r
            ON r.id_rol = u.id_rol
        WHERE LOWER(u.email) = LOWER(:e)
        """;

    private static final String SQL_CONTAR_USUARIOS = """
        SELECT COUNT(*)
        FROM usuario
        """;

    private static final String SQL_CREAR_USUARIO = """
        INSERT INTO usuario (
            nombre,
            email,
            password_hash,
            id_rol,
            id_sucursal
        )
        SELECT
            :n,
            :e,
            :h,
            r.id_rol,
            :s
        FROM rol r
        WHERE r.nombre = :r
        """;

    public UsuarioRepository(JdbcClient db) {
        super(db);
    }

    // ==================================================
    // CONSULTA DE CORREOS
    // ==================================================

    /**
     * Obtiene los correos de los jefes de cocina
     * pertenecientes a una sucursal específica.
     */
    public List<String> correosJefes(long sucursal) {

        if (sucursal <= 0) {
            throw new IllegalArgumentException(
                "El identificador de la sucursal no es válido."
            );
        }

        return db.sql(SQL_CORREOS_JEFES)
                .param("s", sucursal)
                .query(String.class)
                .list();
    }

    /**
     * Obtiene los correos de los usuarios activos
     * que pertenecen a un rol determinado.
     */
    public List<String> correosPorRol(String rol) {

        if (rol == null || rol.isBlank()) {
            throw new IllegalArgumentException(
                "El rol es obligatorio."
            );
        }

        return db.sql(SQL_CORREOS_POR_ROL)
                .param("r", rol.trim())
                .query(String.class)
                .list();
    }

    // ==================================================
    // CONSULTA DE USUARIOS
    // ==================================================

    /**
     * Busca un usuario por su correo electrónico.
     *
     * La búsqueda no distingue entre mayúsculas
     * y minúsculas.
     *
     * @return datos del usuario o null si no existe.
     */
    public Map<String, Object> porEmail(String email) {

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException(
                "El correo electrónico es obligatorio."
            );
        }

        return uno(
            SQL_USUARIO_POR_EMAIL,
            m("e", email.trim())
        );
    }

    /**
     * Cuenta el total de usuarios registrados.
     */
    public int contar() {

        Long total = db.sql(SQL_CONTAR_USUARIOS)
                .query(Long.class)
                .single();

        return Math.toIntExact(total);
    }

    // ==================================================
    // CREACIÓN DE USUARIOS
    // ==================================================

    /**
     * Registra un usuario asociado a un rol existente.
     *
     * La contraseña debe recibirse previamente cifrada
     * mediante un PasswordEncoder.
     */
    public void crear(
            String nombre,
            String email,
            String hash,
            String rol,
            Long sucursal) {

        validarDatosUsuario(nombre, email, hash, rol, sucursal);

        int insertados = db.sql(SQL_CREAR_USUARIO)
                .params(
                    m(
                        "n", nombre.trim(),
                        "e", email.trim(),
                        "h", hash,
                        "r", rol.trim(),
                        "s", sucursal
                    )
                )
                .update();

        if (insertados != 1) {
            throw new IllegalArgumentException(
                "No se pudo crear el usuario. "
                + "Verifica que el rol exista en la base de datos."
            );
        }
    }

    // ==================================================
    // VALIDACIONES
    // ==================================================

    /**
     * Comprueba los datos necesarios para crear un usuario.
     */
    private void validarDatosUsuario(
            String nombre,
            String email,
            String hash,
            String rol,
            Long sucursal) {

        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException(
                "El nombre del usuario es obligatorio."
            );
        }

        if (email == null || email.isBlank()
                || !email.trim().contains("@")) {
            throw new IllegalArgumentException(
                "El correo electrónico no es válido."
            );
        }

        if (hash == null || hash.isBlank()) {
            throw new IllegalArgumentException(
                "El hash de la contraseña es obligatorio."
            );
        }

        if (rol == null || rol.isBlank()) {
            throw new IllegalArgumentException(
                "El rol del usuario es obligatorio."
            );
        }

        if (sucursal != null && sucursal <= 0) {
            throw new IllegalArgumentException(
                "El identificador de la sucursal no es válido."
            );
        }
    }
}
