-- Las solicitudes cuyas órdenes de compra ya fueron recibidas
-- no deben seguir apareciendo como 'EN TRÁMITE'.
UPDATE solicitud
SET estado = 'RECIBIDA'
WHERE id_solicitud IN (
    SELECT sd.id_solicitud
    FROM orden_compra o
    JOIN cotizacion_detalle cd ON cd.id_cotizacion = o.id_cotizacion
    JOIN solicitud_detalle sd ON sd.id_solicitud_detalle = cd.id_solicitud_detalle
    WHERE o.estado = 'Recibida'
);
