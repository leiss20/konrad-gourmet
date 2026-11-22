
package co.konradlorenz.abastecimiento.repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

/**
 * Clase base para los repositorios de Konrad Gourmet.
 *
 * Centraliza operaciones comunes de acceso a datos
 * utilizando JdbcClient.
 */
public abstract class BaseRepository {

    protected final JdbcClient db;

    protected BaseRepository(JdbcClient db) {
        this.db = db;
    }

    // ================================================
    // CONSTRUCCIÓN DE PARÁMETROS SQL
    // ================================================

    /**
     * Construye un mapa de parámetros a partir de pares
     * clave-valor.
     *
     * Ejemplo:
     * m("nombre", "Juan", "edad", 25)
     */
    protected static Map<String, Object> m(Object... parametros) {

        if (parametros.length % 2 != 0) {
            throw new IllegalArgumentException(
                "Los parámetros SQL deben proporcionarse en pares clave-valor."
            );
        }

        Map<String, Object> resultado = new HashMap<>();

        for (int i = 0; i < parametros.length; i += 2) {

            if (!(parametros[i] instanceof String clave)) {
                throw new IllegalArgumentException(
                    "Las claves de los parámetros SQL deben ser cadenas."
                );
            }

            resultado.put(clave, parametros[i + 1]);
        }

        return resultado;
    }

    // ================================================
    // CONVERSIÓN DE RESULTADOS
    // ================================================

    /**
     * Obtiene un valor numérico como long.
     */
    protected static long n(Map<String, Object> resultado, String clave) {

        Object valor = resultado.get(clave);

        if (!(valor instanceof Number numero)) {
            throw new IllegalStateException(
                "El campo '" + clave + "' no contiene un valor numérico válido."
            );
        }

        return numero.longValue();
    }

    /**
     * Obtiene un valor numérico como double.
     */
    protected static double d(Map<String, Object> resultado, String clave) {

        Object valor = resultado.get(clave);

        if (!(valor instanceof Number numero)) {
            throw new IllegalStateException(
                "El campo '" + clave + "' no contiene un valor numérico válido."
            );
        }

        return numero.doubleValue();
    }

    // ================================================
    // OPERACIONES DE INSERCIÓN
    // ================================================

    /**
     * Ejecuta una inserción y devuelve la clave generada.
     */
    protected long insert(
            String sql,
            Map<String, Object> parametros,
            String columnaClave) {

        KeyHolder keyHolder = new GeneratedKeyHolder();

        db.sql(sql)
            .params(parametros)
            .update(keyHolder, columnaClave);

        Number claveGenerada = keyHolder.getKey();

        if (claveGenerada == null) {
            throw new IllegalStateException(
                "La inserción se ejecutó, pero no se obtuvo una clave generada."
            );
        }

        return claveGenerada.longValue();
    }

    // ================================================
    // CONSULTAS
    // ================================================

    /**
     * Ejecuta una consulta y devuelve una lista de registros.
     */
    protected List<Map<String, Object>> rows(
            String sql,
            Map<String, Object> parametros) {

        return db.sql(sql)
                .params(parametros)
                .query()
                .listOfRows();
    }

    /**
     * Ejecuta una operación SQL de modificación.
     */
    protected void exec(
            String sql,
            Map<String, Object> parametros) {

        db.sql(sql)
            .params(parametros)
            .update();
    }

    /**
     * Ejecuta una consulta y devuelve el primer registro.
     *
     * Si no existen resultados, devuelve null.
     */
    protected Map<String, Object> uno(
            String sql,
            Map<String, Object> parametros) {

        List<Map<String, Object>> resultados = rows(sql, parametros);

        return resultados.isEmpty() ? null : resultados.get(0);
    }
}
