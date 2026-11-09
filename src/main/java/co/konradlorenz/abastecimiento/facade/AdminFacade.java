package co.konradlorenz.abastecimiento.facade;

import co.konradlorenz.abastecimiento.service.*;
import java.util.*;
import org.springframework.stereotype.Component;

/**
 * Patrón Facade: administración, catálogos, tema,
 * BAM, auditoría, errores, inventario y promociones.
 */
@Component
public class AdminFacade {

    private final AdminService a;
    private final InventarioService inv;
    private final PromocionService promo;

    // Constructor con las tres dependencias
    public AdminFacade(
            AdminService a,
            InventarioService i,
            PromocionService p) {

        this.a = a;
        this.inv = i;
        this.promo = p;
    }

    // Administración
    public Map<String, Object> me(String e) {
        return a.me(e);
    }

    public Map<String, Object> catalogo() {
        return a.catalogo();
    }

    public void agregar(String e, String t, String n) {
        a.agregar(e, t, n);
    }

    // Tema
    public String tema() {
        return a.tema();
    }

    public void tema(String e, String c) {
        a.tema(e, c);
    }

    // Auditoría y errores
    public List<Map<String, Object>> auditoria() {
        return a.auditoria();
    }

    public List<Map<String, Object>> errores() {
        return a.errores();
    }

    // BAM
    public Map<String, Object> bam() {
        return a.kpis();
    }

    // Productos
    public List<Map<String, Object>> productos() {
        return a.productos();
    }

    public void agregarProducto(
            String e, String n, long c, long u, double p) {
        a.agregarProducto(e, n, c, u, p);
    }

    public void precioProducto(String e, long id, double p) {
        a.precioProducto(e, id, p);
    }

    // Inventario
    public List<Map<String, Object>> inventario(String e) {
        return inv.inventario(e);
    }

    public String procesarInventario() {
        return inv.procesar();
    }

    // Promociones
    public String enviarPromociones() {
        return promo.enviar();
    }
}