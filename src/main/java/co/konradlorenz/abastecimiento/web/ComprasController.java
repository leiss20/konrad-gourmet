
package co.konradlorenz.abastecimiento.web;

import co.konradlorenz.abastecimiento.facade.ComprasFacade;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;

/**
 * Controlador REST encargado de gestionar las operaciones
 * del módulo de compras.
 */
@RestController
@RequestMapping("/api/compras")
public class ComprasController {

    private final ComprasFacade facade;

    public ComprasController(ComprasFacade facade) {
        this.facade = facade;
    }

    // ==================================================
    // DTO DE SOLICITUDES
    // ==================================================

    public record SolicitudReq(
        @NotNull
        @NotEmpty
        List<Map<String, Object>> items
    ) {}

    public record CotizarReq(
        @Positive
        long idProveedor,

        @NotNull
        LocalDate plazo
    ) {}

    public record ValidarReq(
        @Positive
        double valor
    ) {}

    // ==================================================
    // SOLICITUDES
    // ==================================================

    /**
     * Consulta las solicitudes de compra.
     */
    @GetMapping("/solicitudes")
    @PreAuthorize("hasAnyRole('JEFE_COCINA', 'AUXILIAR_COMPRAS')")
    public List<Map<String, Object>> solicitudes(Principal principal) {

        return facade.solicitudes(principal.getName());
    }

    /**
     * Registra una nueva solicitud de productos.
     */
    @PostMapping("/solicitudes")
    @PreAuthorize("hasRole('JEFE_COCINA')")
    public ResponseEntity<Map<String, Object>> solicitar(
            @Valid @RequestBody SolicitudReq request,
            Principal principal) {

        long id = facade.solicitar(
            principal.getName(),
            request.items()
        );

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(Map.of("id", id));
    }

    /**
     * Genera una cotización para una solicitud.
     */
    @PostMapping("/solicitudes/{id}/cotizar")
    @PreAuthorize("hasRole('AUXILIAR_COMPRAS')")
    public ResponseEntity<Map<String, Object>> cotizar(
            @PathVariable @Positive long id,
            @Valid @RequestBody CotizarReq request,
            Principal principal) {

        long idCotizacion = facade.cotizar(
            principal.getName(),
            id,
            request.idProveedor(),
            request.plazo()
        );

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(Map.of("id", idCotizacion));
    }

    // ==================================================
    // COTIZACIONES
    // ==================================================

    /**
     * Consulta todas las cotizaciones.
     */
    @GetMapping("/cotizaciones")
    @PreAuthorize("hasRole('DIRECTOR_COMPRAS')")
    public List<Map<String, Object>> cotizaciones() {

        return facade.cotizaciones();
    }

    /**
     * Valida el precio recibido de un proveedor.
     */
    @PostMapping("/cotizaciones/{id}/validar")
    @PreAuthorize("hasRole('DIRECTOR_COMPRAS')")
    public Map<String, Object> validar(
            @PathVariable @Positive long id,
            @Valid @RequestBody ValidarReq request,
            Principal principal) {

        String estado = facade.validar(
            principal.getName(),
            id,
            request.valor()
        );

        return Map.of("estado", estado);
    }

    // ==================================================
    // ÓRDENES DE COMPRA
    // ==================================================

    /**
     * Genera una orden de compra a partir de una cotización.
     */
    @PostMapping("/cotizaciones/{id}/orden")
    @PreAuthorize("hasRole('DIRECTOR_COMPRAS')")
    public Map<String, Object> orden(
            @PathVariable @Positive long id,
            Principal principal) {

        String numeroOrden = facade.ordenar(
            principal.getName(),
            id
        );

        return Map.of("orden", numeroOrden);
    }

    /**
     * Consulta las órdenes de compra registradas.
     */
    @GetMapping("/ordenes")
    @PreAuthorize("hasRole('DIRECTOR_COMPRAS')")
    public List<Map<String, Object>> ordenes() {

        return facade.ordenes();
    }

    /**
     * Consulta las órdenes de compra para registrar su recepción.
     */
    @GetMapping("/admin/ordenes")
    @PreAuthorize("hasRole('ADMIN')")
    public List<Map<String, Object>> ordenesAdmin() {

        return facade.ordenes();
    }

    /**
     * Registra la llegada de la mercancía de una orden de compra y
     * reabastece el inventario con lo que pidió el jefe de cocina.
     */
    @PostMapping("/ordenes/{id}/recibir")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> recibirOrden(
            @PathVariable @Positive long id,
            Principal principal) {

        String numeroOrden = facade.recibirOrden(
            principal.getName(),
            id
        );

        return Map.of("orden", numeroOrden);
    }
}
