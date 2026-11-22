
package co.konradlorenz.abastecimiento.service;

import co.konradlorenz.abastecimiento.repository.AuditoriaRepository;
import co.konradlorenz.abastecimiento.repository.BamRepository;
import co.konradlorenz.abastecimiento.repository.CatalogoRepository;
import co.konradlorenz.abastecimiento.repository.UsuarioRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio encargado de las operaciones administrativas:
 * catálogos, usuarios, parámetros, productos, auditoría y KPI.
 */
@Service
public class AdminService {

    private final CatalogoRepository catalogo;
    private final AuditoriaRepository repo;
    private final UsuarioRepository usuarios;
    private final BamRepository bam;
    private final AuditoriaService auditoria;

    public AdminService(
            CatalogoRepository catalogo,
            AuditoriaRepository repo,
            UsuarioRepository usuarios,
            BamRepository bam,
            AuditoriaService auditoria) {

        this.catalogo = catalogo;
        this.repo = repo;
        this.usuarios = usuarios;
        this.bam = bam;
        this.auditoria = auditoria;
    }

    // ==================================================
    // USUARIO ACTUAL
    // ==================================================

    /**
     * Obtiene los datos del usuario autenticado
     * sin exponer su hash de contraseña.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> me(String email) {

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException(
                "El correo del usuario es obligatorio."
            );
        }

        Map<String, Object> usuario = usuarios.porEmail(email.trim());

        if (usuario == null) {
            throw new IllegalArgumentException(
                "No se encontró el usuario solicitado."
            );
        }

        Map<String, Object> resultado = new HashMap<>(usuario);

        resultado.remove("password_hash");

        return resultado;
    }

    // ==================================================
    // CATÁLOGOS
    // ==================================================

    @Transactional(readOnly = true)
    public Map<String, Object> catalogo() {
        return catalogo.todo();
    }

    /**
     * Agrega un elemento a un catálogo permitido
     * y registra la operación en auditoría.
     */
    @Transactional
    public void agregar(
            String email,
            String tabla,
            String nombre) {

        validarEmail(email);
        validarNombre(nombre);

        String nombreNormalizado = nombre.trim();

        catalogo.agregar(tabla, nombreNormalizado);

        auditoria.registrar(
            email,
            "CREAR",
            tabla,
            null,
            nombreNormalizado
        );
    }

    // ==================================================
    // TEMA VISUAL
    // ==================================================

    @Transactional(readOnly = true)
    public String tema() {
        return repo.tema();
    }

    /**
     * Actualiza el color principal de la aplicación
     * y registra el cambio.
     */
    @Transactional
    public void tema(String email, String color) {

        validarEmail(email);

        if (color == null || !color.trim().matches("#[0-9a-fA-F]{6}")) {
            throw new IllegalArgumentException(
                "El color debe tener el formato hexadecimal #RRGGBB."
            );
        }

        String colorNormalizado = color.trim();

        repo.tema(colorNormalizado);

        auditoria.registrar(
            email,
            "ACTUALIZAR",
            "parametro",
            null,
            colorNormalizado
        );
    }

    // ==================================================
    // AUDITORÍA Y ERRORES
    // ==================================================

    @Transactional(readOnly = true)
    public List<Map<String, Object>> auditoria() {
        return repo.listar();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> errores() {
        return repo.errores();
    }

    // ==================================================
    // INDICADORES
    // ==================================================

    @Transactional(readOnly = true)
    public Map<String, Object> kpis() {
        return bam.kpis();
    }

    // ==================================================
    // PRODUCTOS
    // ==================================================

    @Transactional(readOnly = true)
    public List<Map<String, Object>> productos() {
        return catalogo.productos();
    }

    /**
     * Crea un producto y registra la operación.
     */
    @Transactional
    public void agregarProducto(
            String email,
            String nombre,
            long idCategoria,
            long idUnidad,
            double precio) {

        validarEmail(email);
        validarNombre(nombre);

        if (idCategoria <= 0 || idUnidad <= 0) {
            throw new IllegalArgumentException(
                "La categoría y la unidad deben ser válidas."
            );
        }

        validarPrecio(precio);

        String nombreNormalizado = nombre.trim();

        catalogo.agregarProducto(
            nombreNormalizado,
            idCategoria,
            idUnidad,
            precio
        );

        auditoria.registrar(
            email,
            "CREAR",
            "producto",
            null,
            nombreNormalizado
        );
    }

    /**
     * Actualiza el precio de referencia de un producto
     * y registra la operación.
     */
    @Transactional
    public void precioProducto(
            String email,
            long id,
            double precio) {

        validarEmail(email);

        if (id <= 0) {
            throw new IllegalArgumentException(
                "El identificador del producto no es válido."
            );
        }

        validarPrecio(precio);

        catalogo.precio(id, precio);

        auditoria.registrar(
            email,
            "ACTUALIZAR",
            "producto",
            id,
            "precio " + precio
        );
    }

    // ==================================================
    // VALIDACIONES
    // ==================================================

    private void validarEmail(String email) {

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException(
                "El correo del usuario es obligatorio."
            );
        }
    }

    private void validarNombre(String nombre) {

        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException(
                "El nombre es obligatorio."
            );
        }
    }

    private void validarPrecio(double precio) {

        if (!Double.isFinite(precio) || precio <= 0) {
            throw new IllegalArgumentException(
                "El precio debe ser un número válido mayor que cero."
            );
        }
    }
}
