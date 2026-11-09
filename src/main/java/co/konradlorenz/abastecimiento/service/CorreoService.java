package co.konradlorenz.abastecimiento.service;
import co.konradlorenz.abastecimiento.repository.AuditoriaRepository;
import jakarta.mail.internet.MimeMessage;
import java.time.OffsetDateTime;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.*;
import org.springframework.stereotype.Service;
/** Envía correos con estampado cronológico. Sin SMTP configurado, los simula en la consola. Los fallos van a log_error. */
@Service public class CorreoService {
  private final ObjectProvider<JavaMailSender> mail; private final AuditoriaRepository log; private final String de;
  public CorreoService(ObjectProvider<JavaMailSender> mail, AuditoriaRepository log, @Value("${correo.remitente}") String de) { this.mail = mail; this.log = log; this.de = de; }
  public void enviar(String para, String asunto, String texto, String adjunto, byte[] pdf) {
    String sello = "\n\n— Mensaje certificado · estampado cronológico: " + OffsetDateTime.now();
    JavaMailSender s = mail.getIfAvailable();
    try {
      if (s == null) { System.out.println("[CORREO SIMULADO] a=" + para + " | " + asunto + " | " + texto + sello); return; }
      MimeMessage m = s.createMimeMessage(); MimeMessageHelper h = new MimeMessageHelper(m, pdf != null);
      h.setFrom(de); h.setTo(para); h.setSubject(asunto); h.setText(texto + sello);
      if (pdf != null) h.addAttachment(adjunto, new ByteArrayResource(pdf));
      s.send(m);
    } catch (Exception e) { log.error("correo", "No se pudo enviar a " + para + ": " + e.getMessage(), ""); }
  }
}