package co.konradlorenz.abastecimiento.adapter;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Simulación del Ministerio: usa el precio de referencia
 * que el administrador define por producto.
 * TODO: reemplazar por un cliente SOAP real.
 */
@Component
@ConditionalOnProperty(
    name = "ministerio.modo",
    havingValue = "mock",
    matchIfMissing = true
)
public class MinisterioMockAdapter implements MinisterioAdapter {

    private final JdbcClient db;

    public MinisterioMockAdapter(JdbcClient db) {
        this.db = db;
    }

    @Override
    public double precioPorKg(String producto) {
        return db.sql(
                "SELECT precio_referencia FROM producto WHERE nombre=:n LIMIT 1"
            )
            .param("n", producto)
            .query(Double.class)
            .optional()
            .orElse(5000.0);
    }
}