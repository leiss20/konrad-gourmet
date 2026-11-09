package co.konradlorenz.abastecimiento.service;
import co.konradlorenz.abastecimiento.repository.*;
import java.util.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service public class InventarioService {
  private final InventarioRepository repo; private final UsuarioRepository usuarios; private final CorreoService correo; private final AuditoriaService auditoria;
  public InventarioService(InventarioRepository r, UsuarioRepository u, CorreoService c, AuditoriaService a) { repo = r; usuarios = u; correo = c; auditoria = a; }

  /** Descuenta el inventario y avisa por correo de los productos que llegaron al 25 % o menos (una sola vez por producto). */
  @Scheduled(cron = "0 0 23 * * *") @Transactional
  public String procesar() {
    int pedidos = repo.descontar(); repo.reiniciarAlarmas(); int alarmas = 0;
    for (var a : repo.alarmasNuevas()) {
      for (String e : usuarios.correosJefes(((Number) a.get("id_sucursal")).longValue()))
        correo.enviar(e, "Alarma de inventario: " + a.get("producto"), "En " + a.get("sucursal") + " quedan " + a.get("cantidad_actual") + " de " + a.get("cantidad_referencia") + " (25 % o menos). Registra una solicitud de alimentos.", null, null);
      repo.marcar(((Number) a.get("id_inventario")).longValue()); alarmas++;
    }
    String r = pedidos + " pedidos procesados, " + alarmas + " alarmas nuevas";
    auditoria.registrar(null, "PROCESAR", "inventario", null, r); return r;
  }
  @Transactional(readOnly = true) public List<Map<String, Object>> inventario(String email) { return repo.listar(((Number) usuarios.porEmail(email).get("id_sucursal")).longValue()); }
}