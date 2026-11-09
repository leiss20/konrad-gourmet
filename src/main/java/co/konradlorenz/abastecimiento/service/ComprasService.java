package co.konradlorenz.abastecimiento.service;
import co.konradlorenz.abastecimiento.adapter.MinisterioAdapter;
import co.konradlorenz.abastecimiento.repository.*;
import java.time.LocalDate;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service @Transactional
public class ComprasService {
private final SolicitudRepository solicitudes;
private final CotizacionRepository cotizaciones;
private final UsuarioRepository usuarios;
private final MinisterioAdapter ministerio;
private final AuditoriaService auditoria;
private final CorreoService correo;
private final CatalogoRepository catalogo;

public ComprasService(
        SolicitudRepository s,
        CotizacionRepository c,
        UsuarioRepository u,
        MinisterioAdapter m,
        AuditoriaService a,
        CorreoService co,
        CatalogoRepository ca) {

    solicitudes = s;
    cotizaciones = c;
    usuarios = u;
    ministerio = m;
    auditoria = a;
    correo = co;
    catalogo = ca;
}
  public long solicitar(String email, List<Map<String, Object>> items) {
    if (items == null || items.isEmpty() || items.stream().anyMatch(i -> ((Number) i.get("cantidad")).doubleValue() <= 0)) throw new IllegalArgumentException("Agrega al menos un producto con cantidad mayor que cero.");
    Map<String, Object> u = usuarios.porEmail(email);
    long id = solicitudes.crear(((Number) u.get("id_sucursal")).longValue(), ((Number) u.get("id_usuario")).longValue(), items);
    auditoria.registrar(email, "CREAR", "solicitud", id, "Estado PENDIENTE");
    correo.enviar(email, String.format("Solicitud S-%04d registrada", id), "Tu solicitud quedó registrada en estado PENDIENTE.", null, null); return id;
  }
  /** El jefe de cocina solo ve las de su sucursal; el auxiliar ve las de todos los puntos. */
  @Transactional(readOnly = true) public List<Map<String, Object>> solicitudes(String email) {
    Map<String, Object> u = usuarios.porEmail(email);
    return solicitudes.listar("JEFE_COCINA".equals(u.get("rol")) ? ((Number) u.get("id_sucursal")).longValue() : null);
  }
  public long cotizar(String email, long idSolicitud, long idProveedor, LocalDate plazo) {
    if (plazo == null || plazo.isBefore(LocalDate.now())) throw new IllegalArgumentException("La fecha máxima no puede ser anterior a hoy.");
    var lineas = solicitudes.lineas(idSolicitud); if (lineas.isEmpty()) throw new IllegalArgumentException("Solicitud no encontrada.");
    double total = 0; List<Double> precios = new ArrayList<>();
    for (var l : lineas) { double p = ministerio.precioPorKg((String) l.get("producto")) * ("Libra".equals(l.get("unidad")) ? 0.5 : 1); precios.add(p); total += ((Number) l.get("cantidad")).doubleValue() * p; }
    long id = cotizaciones.crear(idProveedor, plazo, total);
    for (int i = 0; i < lineas.size(); i++) cotizaciones.detalle(id, lineas.get(i), precios.get(i));
    solicitudes.estado(idSolicitud, "EN TRÁMITE");
        var prov = catalogo.proveedor(idProveedor);
    correo.enviar((String) prov.get("email"), "Solicitud de cotización " + id, "Adjuntamos la solicitud de cotización. Fecha máxima de respuesta: " + plazo,
      "cotizacion-" + id + ".pdf", PdfCotizacion.generar(String.valueOf(id), (String) prov.get("nombre"), plazo, lineas));
    auditoria.registrar(email, "COTIZAR", "cotizacion", id, "Solicitud " + idSolicitud); return id;
  }
  /** Regla de negocio: >25 % sobre el estándar = RECHAZADA; >50 % por debajo = SOSPECHOSA; en otro caso OPCIONADA. */
  public String validar(String email, long id, double valor) {
    if (valor <= 0) throw new IllegalArgumentException("Ingresa un valor mayor que cero.");
    var c = cotizaciones.porId(id); if (c == null || !"ENVIADA".equals(c.get("estado"))) throw new IllegalArgumentException("Solo se validan cotizaciones en estado ENVIADA.");
    double ratio = valor / ((Number) c.get("valor_total_ministerio")).doubleValue();
    String estado = ratio > 1.25 ? "RECHAZADA" : ratio < 0.5 ? "SOSPECHOSA" : "OPCIONADA";
    cotizaciones.validar(id, valor, estado, ratio); auditoria.registrar(email, "VALIDAR", "cotizacion", id, estado); return estado;
  }
  public String ordenar(String email, long id) {
    var c = cotizaciones.porId(id);
    if (c == null || !"OPCIONADA".equals(c.get("estado")) || ((Number) c.get("ordenes")).intValue() > 0) throw new IllegalArgumentException("Solo una cotización OPCIONADA sin orden puede ordenarse.");
    String num = cotizaciones.ordenar(id, ((Number) c.get("id_proveedor")).longValue(), ((Number) c.get("valor_total_proveedor")).doubleValue());
    auditoria.registrar(email, "ORDENAR", "orden_compra", id, num); return num;
  }
  @Transactional(readOnly = true) public List<Map<String, Object>> cotizaciones() { return cotizaciones.listar(); }
  @Transactional(readOnly = true) public List<Map<String, Object>> ordenes() { return cotizaciones.ordenes(); }
}
