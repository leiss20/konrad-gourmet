
package co.konradlorenz.abastecimiento.adapter;

import javax.xml.transform.Source;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.ws.client.WebServiceIOException;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.xml.transform.StringResult;
import org.springframework.xml.transform.StringSource;

/**
 * Adaptador SOAP para consultar los precios de productos
 * en el servicio externo del Ministerio.
 *
 * Se activa cuando ministerio.modo=soap.
 */
@Component
@ConditionalOnProperty(
    name = "ministerio.modo",
    havingValue = "soap"
)
public class MinisterioSoapAdapter implements MinisterioAdapter {

    private final WebServiceTemplate webServiceTemplate;

    public MinisterioSoapAdapter(
            WebServiceTemplate webServiceTemplate,
            @Value("${ministerio.url}") String url) {

        this.webServiceTemplate = webServiceTemplate;
        this.webServiceTemplate.setDefaultUri(url);
    }

    @Override
    public double precioPorKg(String producto) {

        validarProducto(producto);

        String requestXml = construirSolicitud(producto);

        StringResult response = new StringResult();

        try {
            Source request = new StringSource(requestXml);

            webServiceTemplate.sendSourceAndReceiveToResult(
                request,
                response
            );

            return extraerPrecio(response.toString(), producto);

        } catch (WebServiceIOException exception) {
            throw new IllegalStateException(
                "Error de comunicación con el servicio SOAP del Ministerio.",
                exception
            );
        }
    }

    private void validarProducto(String producto) {

        if (producto == null || producto.isBlank()) {
            throw new IllegalArgumentException(
                "El nombre del producto no puede estar vacío."
            );
        }
    }

    private String construirSolicitud(String producto) {

        String productoSeguro = producto
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;");

        return """
            <ConsultarPrecioRequest xmlns="http://comercio.gov.co/precios">
                <producto>%s</producto>
            </ConsultarPrecioRequest>
            """.formatted(productoSeguro);
    }

    private double extraerPrecio(String respuesta, String producto) {

        // Pendiente: implementar extracción mediante JAXB
        // o un parser XML según la estructura real del WSDL.

        throw new UnsupportedOperationException(
            "La extracción del precio requiere conocer la estructura real del XML SOAP."
        );
    }
}
