
package co.konradlorenz.abastecimiento.web;

import co.konradlorenz.abastecimiento.repository.AuditoriaRepository;

import jakarta.validation.ConstraintViolationException;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.security.access.AccessDeniedException;

import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

/**
 * RNF-MAN-02:
 * Todo error inesperado queda registrado en log_error.
 */
@RestControllerAdvice
public class ErroresApi {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(ErroresApi.class);

    private final AuditoriaRepository repo;

    public ErroresApi(AuditoriaRepository repo) {
        this.repo = repo;
    }

    /**
     * Errores de reglas de negocio.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> negocio(
            IllegalArgumentException e) {

        return respuesta(
                HttpStatus.BAD_REQUEST,
                e.getMessage()
        );
    }

    /**
     * Errores de validación de datos recibidos.
     */
    @ExceptionHandler({
        MethodArgumentNotValidException.class,
        HandlerMethodValidationException.class,
        ConstraintViolationException.class,
        HttpMessageNotReadableException.class
    })
    public ResponseEntity<Map<String, String>> validacion(Exception e) {

        return respuesta(
                HttpStatus.BAD_REQUEST,
                "Los datos enviados no son válidos."
        );
    }

    /**
     * El usuario está autenticado, pero no tiene permisos.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> accesoDenegado(
            AccessDeniedException e) {

        return respuesta(
                HttpStatus.FORBIDDEN,
                "No tienes permisos para realizar esta operación."
        );
    }

    /**
     * Errores inesperados del servidor.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> inesperado(Exception e) {

        StringWriter sw = new StringWriter();

        e.printStackTrace(new PrintWriter(sw));

        try {
            repo.error(
                    "api",
                    String.valueOf(e.getMessage()),
                    sw.toString()
            );
        } catch (RuntimeException errorLog) {
            LOGGER.error(
                    "No se pudo registrar el error en log_error",
                    errorLog
            );
        }

        LOGGER.error("Error inesperado en la API", e);

        return respuesta(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Error interno. Quedó registrado en el log."
        );
    }

    /**
     * Construye respuestas de error uniformes.
     */
    private ResponseEntity<Map<String, String>> respuesta(
            HttpStatus estado,
            String mensaje) {

        return ResponseEntity
                .status(estado)
                .body(Map.of(
                        "error",
                        mensaje == null ? "Error en la solicitud." : mensaje
                ));
    }
}
