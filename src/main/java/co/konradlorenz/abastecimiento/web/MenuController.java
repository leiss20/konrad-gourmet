
package co.konradlorenz.abastecimiento.web;

import co.konradlorenz.abastecimiento.facade.MenuFacade;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.security.Principal;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/menu")
public class MenuController {

    public record PlatoReq(
            @NotBlank String nombre,
            @Positive double precio,
            @NotNull @NotEmpty List<Map<String, Object>> ingredientes
    ) {}

    public record PedidoReq(
            @Positive int mesa,
            @NotNull @NotEmpty List<Map<String, Object>> items
    ) {}

    private final MenuFacade facade;

    public MenuController(MenuFacade facade) {
        this.facade = facade;
    }

    /**
     * Consulta los platos disponibles para el usuario.
     */
    @GetMapping("/platos")
    @PreAuthorize("hasAnyRole('JEFE_COCINA', 'MESERO')")
    public List<Map<String, Object>> platos(Principal principal) {

        return facade.platos(principal.getName());
    }

    /**
     * Registra un nuevo plato.
     */
    @PostMapping("/platos")
    @PreAuthorize("hasRole('JEFE_COCINA')")
    public ResponseEntity<Map<String, Object>> plato(
            @Valid @RequestBody PlatoReq request,
            Principal principal) {

        long id = facade.registrarPlato(
                principal.getName(),
                request.nombre(),
                request.precio(),
                request.ingredientes()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(Map.of("id", id));
    }

    /**
     * Consulta los pedidos de la sucursal del mesero.
     */
    @GetMapping("/pedidos")
    @PreAuthorize("hasRole('MESERO')")
    public List<Map<String, Object>> pedidos(Principal principal) {

        return facade.pedidos(principal.getName());
    }

    /**
     * Registra un nuevo pedido.
     */
    @PostMapping("/pedidos")
    @PreAuthorize("hasRole('MESERO')")
    public ResponseEntity<Map<String, Object>> pedido(
            @Valid @RequestBody PedidoReq request,
            Principal principal) {

        long id = facade.registrarPedido(
                principal.getName(),
                request.mesa(),
                request.items()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(Map.of("id", id));
    }
}
