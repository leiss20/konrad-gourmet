
package co.konradlorenz.abastecimiento.service;

import co.konradlorenz.abastecimiento.repository.PlatoRepository;
import co.konradlorenz.abastecimiento.repository.PedidoRepository;
import co.konradlorenz.abastecimiento.repository.UsuarioRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio encargado de administrar el menú y los pedidos.
 */
@Service
@Transactional
public class MenuService {

    private final PlatoRepository platos;
    private final PedidoRepository pedidos;
    private final UsuarioRepository usuarios;
    private final AuditoriaService auditoria;

    public MenuService(
            PlatoRepository platos,
            PedidoRepository pedidos,
            UsuarioRepository usuarios,
            AuditoriaService auditoria) {

        this.platos = platos;
        this.pedidos = pedidos;
        this.usuarios = usuarios;
        this.auditoria = auditoria;
    }

    // ==================================================
    // CREACIÓN DE PLATOS
    // ==================================================

    /**
     * Registra un plato con sus ingredientes en la sucursal
     * del jefe de cocina que realiza la operación.
     */
    public long crearPlato(
            String email,
            String nombre,
            double precio,
            List<Map<String, Object>> ingredientes) {

        Map<String, Object> usuario = obtenerUsuario(email);

        validarRol(usuario, "JEFE_COCINA");

        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException(
                "El nombre del plato es obligatorio."
            );
        }

        if (!Double.isFinite(precio) || precio <= 0) {
            throw new IllegalArgumentException(
                "El precio debe ser un número mayor que cero."
            );
        }

        validarIngredientes(ingredientes);

        long idSucursal = obtenerId(usuario, "id_sucursal");

        long idPlato = platos.crear(
            nombre.trim(),
            precio,
            idSucursal,
            ingredientes
        );

        auditoria.registrar(
            email,
            "CREAR",
            "plato",
            idPlato,
            nombre.trim()
        );

        return idPlato;
    }

    // ==================================================
    // REGISTRO DE PEDIDOS
    // ==================================================

    /**
     * Registra un pedido para una mesa de la sucursal
     * a la que pertenece el mesero.
     */
    public long registrarPedido(
            String email,
            int mesa,
            List<Map<String, Object>> items) {

        Map<String, Object> usuario = obtenerUsuario(email);

        validarRol(usuario, "MESERO");

        if (mesa <= 0) {
            throw new IllegalArgumentException(
                "El número de mesa debe ser mayor que cero."
            );
        }

        validarItemsPedido(items);

        long idSucursal = obtenerId(usuario, "id_sucursal");
        long idMesero = obtenerId(usuario, "id_usuario");

        long idPedido = pedidos.crear(
            mesa,
            idSucursal,
            idMesero,
            items
        );

        auditoria.registrar(
            email,
            "CREAR",
            "pedido",
            idPedido,
            "Mesa " + mesa
        );

        return idPedido;
    }

    // ==================================================
    // CONSULTAS
    // ==================================================

    /**
     * Consulta los platos disponibles en la sucursal
     * del usuario autenticado.
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> platos(String email) {

        Map<String, Object> usuario = obtenerUsuario(email);

        validarRolConsulta(usuario);

        long idSucursal = obtenerId(usuario, "id_sucursal");

        return platos.listar(idSucursal);
    }

    /**
     * Consulta los pedidos de la sucursal
     * del usuario autenticado.
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> pedidos(String email) {

        Map<String, Object> usuario = obtenerUsuario(email);

        validarRolConsulta(usuario);

        long idSucursal = obtenerId(usuario, "id_sucursal");

        return pedidos.listar(idSucursal);
    }

    // ==================================================
    // VALIDACIONES
    // ==================================================

    /**
     * Busca al usuario por su correo electrónico.
     */
    private Map<String, Object> obtenerUsuario(String email) {

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException(
                "El correo electrónico es obligatorio."
            );
        }

        Map<String, Object> usuario = usuarios.porEmail(email.trim());

        if (usuario == null) {
            throw new IllegalArgumentException(
                "El usuario no existe o no está registrado."
            );
        }

        return usuario;
    }

    /**
     * Comprueba que el usuario tenga el rol requerido.
     */
    private void validarRol(
            Map<String, Object> usuario,
            String rolRequerido) {

        String rol = (String) usuario.get("rol");

        if (!rolRequerido.equals(rol)) {
            throw new IllegalArgumentException(
                "No tienes permisos para realizar esta operación."
            );
        }
    }

    /**
     * Permite consultar el menú y los pedidos a los roles
     * relacionados con la operación de cocina.
     */
    private void validarRolConsulta(Map<String, Object> usuario) {

        String rol = (String) usuario.get("rol");

        if (!"JEFE_COCINA".equals(rol)
                && !"MESERO".equals(rol)) {

            throw new IllegalArgumentException(
                "No tienes permisos para consultar esta información."
            );
        }
    }

    /**
     * Comprueba los ingredientes antes de enviarlos al repositorio.
     */
    private void validarIngredientes(
            List<Map<String, Object>> ingredientes) {

        if (ingredientes == null || ingredientes.isEmpty()) {
            throw new IllegalArgumentException(
                "El plato debe tener al menos un ingrediente."
            );
        }

        Set<Long> productos = new HashSet<>();

        for (Map<String, Object> ingrediente : ingredientes) {

            if (ingrediente == null) {
                throw new IllegalArgumentException(
                    "Hay un ingrediente vacío."
                );
            }

            long idProducto = obtenerId(ingrediente, "idProducto");
            long idUnidad = obtenerId(ingrediente, "idUnidad");

            double cantidad = obtenerCantidad(ingrediente);

            if (!productos.add(idProducto)) {
                throw new IllegalArgumentException(
                    "Un producto no puede repetirse en el mismo plato."
                );
            }
        }
    }

    /**
     * Comprueba los platos y cantidades del pedido.
     */
    private void validarItemsPedido(
            List<Map<String, Object>> items) {

        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException(
                "Agrega al menos un plato al pedido."
            );
        }

        for (Map<String, Object> item : items) {

            if (item == null) {
                throw new IllegalArgumentException(
                    "Hay un elemento vacío en el pedido."
                );
            }

            obtenerId(item, "idPlato");

            double cantidad = obtenerCantidad(item);

            if (cantidad != Math.floor(cantidad)) {
                throw new IllegalArgumentException(
                    "La cantidad de platos debe ser un número entero."
                );
            }
        }
    }

    /**
     * Extrae y valida un identificador.
     */
    private long obtenerId(
            Map<String, Object> datos,
            String campo) {

        Object valor = datos.get(campo);

        if (!(valor instanceof Number)) {
            throw new IllegalArgumentException(
                "El campo " + campo + " debe contener un identificador numérico."
            );
        }

        long id = ((Number) valor).longValue();

        if (id <= 0) {
            throw new IllegalArgumentException(
                "El identificador " + campo + " no es válido."
            );
        }

        return id;
    }

    /**
     * Extrae y valida una cantidad numérica.
     */
    private double obtenerCantidad(Map<String, Object> datos) {

        Object valor = datos.get("cantidad");

        if (!(valor instanceof Number)) {
            throw new IllegalArgumentException(
                "La cantidad debe ser numérica."
            );
        }

        double cantidad = ((Number) valor).doubleValue();

        if (!Double.isFinite(cantidad) || cantidad <= 0) {
            throw new IllegalArgumentException(
                "La cantidad debe ser mayor que cero."
            );
        }

        return cantidad;
    }
}
