
package co.konradlorenz.abastecimiento.service;

import co.konradlorenz.abastecimiento.repository.AuditoriaRepository;
import co.konradlorenz.abastecimiento.repository.UsuarioRepository;

import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio encargado de registrar las acciones
 * realizadas por los usuarios del sistema.
 *
 * RNF-MAN-01:
 * Cada acción debe registrar usuario, fecha y hora.
 */
@Service
public class AuditoriaService {

    private final AuditoriaRepository repo;
    private final UsuarioRepository usuarios;

    public AuditoriaService(
            AuditoriaRepository repo,
            UsuarioRepository usuarios) {

        this.repo = repo;
        this.usuarios = usuarios;
    }

    /**
     * Registra una acción realizada por un usuario.
     *
     * Busca el identificador del usuario mediante
     * su correo electrónico y registra la operación.
     *
     * @param email correo del usuario que realiza la acción.
     * @param accion tipo de operación realizada.
     * @param tabla tabla o entidad afectada.
     * @param registro identificador del registro afectado.
     * @param detalle información adicional de la operación.
     */
    @Transactional
    public void registrar(
            String email,
            String accion,
            String tabla,
            Long registro,
            String detalle) {

        validarDatos(email, accion, tabla);

        Map<String, Object> usuario = usuarios.porEmail(email.trim());

        if (usuario == null) {
            throw new IllegalArgumentException(
                "No se puede registrar la auditoría: "
                + "el usuario no existe."
            );
        }

        Object idUsuario = usuario.get("id_usuario");

        if (!(idUsuario instanceof Number)) {
            throw new IllegalStateException(
                "No se pudo obtener el identificador del usuario."
            );
        }

        long id = ((Number) idUsuario).longValue();

        repo.registrar(
            id,
            accion.trim(),
            tabla.trim(),
            registro,
            detalle
        );
    }

    // ==================================================
    // VALIDACIONES
    // ==================================================

    private void validarDatos(
            String email,
            String accion,
            String tabla) {

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException(
                "El correo del usuario es obligatorio para auditar."
            );
        }

        if (accion == null || accion.isBlank()) {
            throw new IllegalArgumentException(
                "La acción de auditoría es obligatoria."
            );
        }

        if (tabla == null || tabla.isBlank()) {
            throw new IllegalArgumentException(
                "La tabla o entidad afectada es obligatoria."
            );
        }
    }
    /**
 * Registra acciones ejecutadas automáticamente por el sistema.
 */
  public void registrarSistema(
          String accion,
          String tabla,
          Long registro,
          String detalle) {

      repo.registrar(
          null,
          accion,
          tabla,
          registro,
          detalle
      );
  }
}
