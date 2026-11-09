package co.konradlorenz.abastecimiento.adapter;
/** Patrón Adapter: contrato del servicio de precios del Ministerio. Cambiar la implementación no afecta al negocio. */
public interface MinisterioAdapter { double precioPorKg(String producto); }
