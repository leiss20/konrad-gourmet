
package co.konradlorenz.abastecimiento.web;

import co.konradlorenz.abastecimiento.facade.AdminFacade;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.security.Principal;
import java.util.List;
import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class PanelController {

    public record NombreReq(
            @NotBlank String nombre
    ) {}

    public record TemaReq(
            @NotBlank String color
    ) {}

    public record ProductoReq(
            @NotBlank String nombre,
            @Positive long idCategoria,
            @Positive long idUnidad,
            @Positive double precio
    ) {}

    public record PrecioReq(
            @Positive double precio
    ) {}

    public record ReabastecerReq(
            @Positive double cantidad
    ) {}

    private final AdminFacade facade;

    public PanelController(AdminFacade facade) {
        this.facade = facade;
    }

    /**
     * Obtiene la información del usuario autenticado.
     */
    @GetMapping("/me")
    public Map<String, Object> me(Principal principal) {
        return facade.me(principal.getName());
    }

    /**
     * Consulta el catálogo general.
     */
    @GetMapping("/catalogo")
    public Map<String, Object> catalogo() {
        return facade.catalogo();
    }

    /**
     * Consulta el tema visual actual.
     */
    @GetMapping("/tema")
    public Map<String, String> tema() {
        return Map.of("color", facade.tema());
    }

    /**
     * Consulta los indicadores del área comercial.
     */
    @GetMapping("/bam")
    @PreAuthorize("hasRole('DIRECTOR_COMERCIAL')")
    public Map<String, Object> bam() {
        return facade.bam();
    }

    /**
     * Agrega un elemento al catálogo administrativo.
     */
    @PostMapping("/admin/catalogo/{tabla}")
    @PreAuthorize("hasRole('ADMIN')")
    public void agregar(
            @PathVariable String tabla,
            @Valid @RequestBody NombreReq request,
            Principal principal) {

        facade.agregar(
                principal.getName(),
                tabla,
                request.nombre()
        );
    }

    /**
     * Actualiza el tema visual.
     */
    @PutMapping("/admin/tema")
    @PreAuthorize("hasRole('ADMIN')")
    public void tema(
            @Valid @RequestBody TemaReq request,
            Principal principal) {

        facade.tema(
                principal.getName(),
                request.color()
        );
    }

    /**
     * Consulta el historial de auditoría.
     */
    @GetMapping("/admin/auditoria")
    @PreAuthorize("hasRole('ADMIN')")
    public List<Map<String, Object>> auditoria() {
        return facade.auditoria();
    }

    /**
     * Consulta los errores registrados.
     */
    @GetMapping("/admin/logs")
    @PreAuthorize("hasRole('ADMIN')")
    public List<Map<String, Object>> logs() {
        return facade.errores();
    }

    /**
     * Consulta los productos registrados.
     */
    @GetMapping("/admin/productos")
    @PreAuthorize("hasRole('ADMIN')")
    public List<Map<String, Object>> productos() {
        return facade.productos();
    }

    /**
     * Consulta el inventario de la sucursal del jefe de cocina.
     */
    @GetMapping("/inventario")
    @PreAuthorize("hasRole('JEFE_COCINA')")
    public List<Map<String, Object>> inventario(Principal principal) {
        return facade.inventario(principal.getName());
    }

    /**
     * Consulta el inventario de todas las sucursales.
     */
    @GetMapping("/admin/inventario")
    @PreAuthorize("hasRole('ADMIN')")
    public List<Map<String, Object>> inventarioGeneral() {
        return facade.inventarioGeneral();
    }

    /**
     * Registra la llegada de mercancía de los proveedores
     * y reabastece el inventario.
     */
    @PostMapping("/admin/inventario/{id}/reabastecer")
    @PreAuthorize("hasRole('ADMIN')")
    public void reabastecer(
            @PathVariable @Positive long id,
            @Valid @RequestBody ReabastecerReq request,
            Principal principal) {

        facade.reabastecer(
                principal.getName(),
                id,
                request.cantidad()
        );
    }

    /**
     * Ejecuta manualmente el procesamiento del inventario.
     */
    @PostMapping("/admin/inventario/procesar")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, String> procesar() {
        return Map.of(
                "resultado",
                facade.procesarInventario()
        );
    }

    /**
     * Ejecuta manualmente el envío de promociones.
     */
    @PostMapping("/admin/promociones/enviar")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, String> promociones() {
        return Map.of(
                "resultado",
                facade.enviarPromociones()
        );
    }

    /**
     * Registra un nuevo producto.
     */
    @PostMapping("/admin/productos")
    @PreAuthorize("hasRole('ADMIN')")
    public void producto(
            @Valid @RequestBody ProductoReq request,
            Principal principal) {

        facade.agregarProducto(
                principal.getName(),
                request.nombre(),
                request.idCategoria(),
                request.idUnidad(),
                request.precio()
        );
    }

    /**
     * Actualiza el precio de un producto.
     */
    @PutMapping("/admin/productos/{id}/precio")
    @PreAuthorize("hasRole('ADMIN')")
    public void precio(
            @PathVariable @Positive long id,
            @Valid @RequestBody PrecioReq request,
            Principal principal) {

        facade.precioProducto(
                principal.getName(),
                id,
                request.precio()
        );
    }
}
