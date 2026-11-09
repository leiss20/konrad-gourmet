package co.konradlorenz.abastecimiento.adapter;
import java.util.regex.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.xml.transform.*;
/** Cliente SOAP real. Se activa con ministerio.modo=soap y ministerio.url. TODO: ajustar namespace y elementos al WSDL del Ministerio. */
@Component @ConditionalOnProperty(name = "ministerio.modo", havingValue = "soap")
public class MinisterioSoapAdapter implements MinisterioAdapter {
  private final WebServiceTemplate ws = new WebServiceTemplate();
  public MinisterioSoapAdapter(@Value("${ministerio.url}") String url) { ws.setDefaultUri(url); }
  public double precioPorKg(String producto) {
    String xml = "<ConsultarPrecioRequest xmlns=\"http://comercio.gov.co/precios\"><producto>" + producto.replace("&", "&amp;").replace("<", "&lt;") + "</producto></ConsultarPrecioRequest>";
    StringResult out = new StringResult(); ws.sendSourceAndReceiveToResult(new StringSource(xml), out);
    Matcher m = Pattern.compile("<(?:\\w+:)?precio>([0-9.]+)</").matcher(out.toString());
    if (!m.find()) throw new IllegalStateException("La respuesta SOAP no trae el precio de " + producto);
    return Double.parseDouble(m.group(1));
  }
}