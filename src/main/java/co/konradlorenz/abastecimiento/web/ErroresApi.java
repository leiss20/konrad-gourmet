package co.konradlorenz.abastecimiento.web;
import co.konradlorenz.abastecimiento.repository.AuditoriaRepository;
import java.io.*;
import java.util.Map;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;
/** RNF-MAN-02: todo error inesperado queda registrado en log_error. */
@RestControllerAdvice public class ErroresApi {
  private final AuditoriaRepository repo;
  public ErroresApi(AuditoriaRepository r) { repo = r; }
  @ExceptionHandler(IllegalArgumentException.class) ResponseEntity<Map<String, String>> negocio(IllegalArgumentException e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
  @ExceptionHandler(Exception.class) ResponseEntity<Map<String, String>> inesperado(Exception e) throws Exception {
    if (e instanceof AccessDeniedException) throw e;
    StringWriter sw = new StringWriter(); e.printStackTrace(new PrintWriter(sw));
    try { repo.error("api", String.valueOf(e.getMessage()), sw.toString()); } catch (Exception ignorada) {}
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Error interno. Quedó registrado en el log."));
  }
}
