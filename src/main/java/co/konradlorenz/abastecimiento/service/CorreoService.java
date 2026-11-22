
package co.konradlorenz.abastecimiento.service;

import co.konradlorenz.abastecimiento.repository.AuditoriaRepository;

import jakarta.mail.internet.MimeMessage;

import java.time.OffsetDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class CorreoService {

    private static final Logger LOGGER =
        LoggerFactory.getLogger(CorreoService.class);

    private final ObjectProvider<JavaMailSender> mail;
    private final AuditoriaRepository log;
    private final String remitente;

    public CorreoService(
            ObjectProvider<JavaMailSender> mail,
            AuditoriaRepository log,
            @Value("${correo.remitente:}") String remitente) {

        this.mail = mail;
        this.log = log;
        this.remitente = remitente;
    }

    /**
     * Envía un correo electrónico con opción de adjuntar un PDF.
     *
     * Si no existe un servidor SMTP configurado, simula el envío.
     * Los errores quedan registrados en log_error.
     */
    public void enviar(
            String para,
            String asunto,
            String texto,
            String adjunto,
            byte[] pdf) {

        validarCorreo(para);
        validarContenido(asunto, texto);
        validarAdjunto(adjunto, pdf);

        String sello = generarSelloCronologico();

        JavaMailSender sender = mail.getIfAvailable();

        if (sender == null) {
            simularEnvio(para, asunto, texto, sello, adjunto);
            return;
        }

        try {
            enviarCorreoReal(
                sender,
                para,
                asunto,
                texto + sello,
                adjunto,
                pdf
            );

        } catch (Exception e) {

            registrarError(para, e);

            throw new IllegalStateException(
                "No se pudo enviar el correo electrónico a " + para,
                e
            );
        }
    }

    /**
     * Construye y envía el mensaje mediante SMTP.
     */
    private void enviarCorreoReal(
            JavaMailSender sender,
            String para,
            String asunto,
            String texto,
            String adjunto,
            byte[] pdf) throws Exception {

        MimeMessage mensaje = sender.createMimeMessage();

        MimeMessageHelper helper = new MimeMessageHelper(
            mensaje,
            pdf != null,
            "UTF-8"
        );

        if (remitente == null || remitente.isBlank()) {
            throw new IllegalStateException(
                "No se ha configurado correo.remitente."
            );
        }

        helper.setFrom(remitente);
        helper.setTo(para);
        helper.setSubject(asunto);
        helper.setText(texto);

        if (pdf != null) {
            helper.addAttachment(
                adjunto,
                new ByteArrayResource(pdf)
            );
        }

        sender.send(mensaje);
    }

    /**
     * Simula el envío cuando no existe un JavaMailSender.
     */
    private void simularEnvio(
            String para,
            String asunto,
            String texto,
            String sello,
            String adjunto) {

        LOGGER.info(
            "[CORREO SIMULADO] Destinatario: {} | Asunto: {} | "
            + "Mensaje: {}{} | Adjunto: {}",
            para,
            asunto,
            texto,
            sello,
            adjunto == null ? "Ninguno" : adjunto
        );
    }

    /**
     * Genera el sello cronológico del mensaje.
     */
    private String generarSelloCronologico() {

        return "\n\n— Mensaje certificado · "
            + "estampado cronológico: "
            + OffsetDateTime.now();
    }

    /**
     * Registra los errores de envío en la base de datos.
     */
    private void registrarError(String para, Exception error) {

        String mensaje = "No se pudo enviar a "
            + para
            + ": "
            + error.getMessage();

        LOGGER.error(mensaje, error);

        try {
            log.error(
                "correo",
                mensaje,
                obtenerStackTrace(error)
            );

        } catch (Exception errorRegistro) {

            LOGGER.error(
                "Tampoco se pudo registrar el error de correo en la base de datos.",
                errorRegistro
            );
        }
    }

    /**
     * Obtiene el stack trace para almacenarlo en log_error.
     */
    private String obtenerStackTrace(Exception error) {

        java.io.StringWriter writer = new java.io.StringWriter();

        error.printStackTrace(
            new java.io.PrintWriter(writer)
        );

        return writer.toString();
    }

    private void validarCorreo(String para) {

        if (para == null || para.isBlank() || !para.contains("@")) {
            throw new IllegalArgumentException(
                "El correo destinatario no es válido."
            );
        }
    }

    private void validarContenido(String asunto, String texto) {

        if (asunto == null || asunto.isBlank()) {
            throw new IllegalArgumentException(
                "El asunto del correo es obligatorio."
            );
        }

        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException(
                "El contenido del correo es obligatorio."
            );
        }
    }

    private void validarAdjunto(String adjunto, byte[] pdf) {

        if (pdf != null && pdf.length == 0) {
            throw new IllegalArgumentException(
                "El archivo adjunto está vacío."
            );
        }

        if (pdf != null && (adjunto == null || adjunto.isBlank())) {
            throw new IllegalArgumentException(
                "El nombre del archivo adjunto es obligatorio."
            );
        }
    }
}
