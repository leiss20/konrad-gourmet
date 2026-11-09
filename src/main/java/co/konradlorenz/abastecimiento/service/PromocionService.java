package co.konradlorenz.abastecimiento.service;
import co.konradlorenz.abastecimiento.adapter.TendenciasAdapter;
import co.konradlorenz.abastecimiento.repository.*;
import java.util.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
@Service public class PromocionService {
  private final BamRepository bam; private final UsuarioRepository usuarios; private final CorreoService correo; private final TendenciasAdapter tendencias; private final AuditoriaService auditoria;
  public PromocionService(BamRepository b, UsuarioRepository u, CorreoService c, TendenciasAdapter t, AuditoriaService a) { bam = b; usuarios = u; correo = c; tendencias = t; auditoria = a; }
  @SuppressWarnings("unchecked") @Scheduled(cron = "0 0 8 * * MON")
  public String enviar() {
    var top = ((List<Map<String, Object>>) bam.kpis().get("demanda")).stream().limit(3).map(x -> (String) x.get("nombre")).toList();
    String texto = "Productos más solicitados este mes: " + String.join(", ", top) + ".\nTendencias en redes: " + String.join(", ", tendencias.tendencias()) + ".\nAprovecha promociones con tus proveedores.";
    var jefes = usuarios.correosPorRol("JEFE_COCINA"); jefes.forEach(e -> correo.enviar(e, "Promociones y productos destacados", texto, null, null));
    String r = jefes.size() + " correos enviados"; auditoria.registrar(null, "ENVIAR", "promocion", null, r); return r;
  }
}