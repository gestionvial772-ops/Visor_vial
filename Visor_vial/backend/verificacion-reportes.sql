-- Verificacion de solo lectura para el SQL Editor de Supabase.
-- No modifica reportes, coordenadas, usuarios ni objetos de Storage.
BEGIN TRANSACTION READ ONLY;

-- Relacion completa: comprobar id_usuario, id_via y codigo con lo elegido en UI.
SELECT r.id_reporte, r.id_usuario, r.id_via,
       v.nombre_via, v.codigo_segmento,
       c.id_coordenada, ST_Y(c.punto_capturado) AS latitud,
       ST_X(c.punto_capturado) AS longitud,
       r.imagen_url, r.estado, r.eliminado_en,
       EXISTS (SELECT 1 FROM storage.objects o WHERE o.name = r.imagen_url) AS imagen_en_storage
FROM public.reporte r
LEFT JOIN public.via_segmento v ON v.id_via = r.id_via
LEFT JOIN public.coordenada c ON c.id_reporte = r.id_reporte
ORDER BY r.fecha_creacion DESC
LIMIT 30;

-- Comparar activos con el total del panel. Anulados deben seguir en la BD.
SELECT COUNT(*) AS filas_conservadas,
       COUNT(*) FILTER (WHERE eliminado_en IS NULL) AS activos,
       COUNT(*) FILTER (WHERE eliminado_en IS NOT NULL) AS anulados
FROM public.reporte;

SELECT estado, COUNT(*) AS cantidad
FROM public.reporte
WHERE eliminado_en IS NULL
GROUP BY estado;

-- Detectar referencias a vias inexistentes (resultado esperado: cero filas).
SELECT r.id_reporte, r.id_via
FROM public.reporte r
LEFT JOIN public.via_segmento v ON v.id_via = r.id_via
WHERE r.id_via IS NOT NULL AND v.id_via IS NULL;

-- Revisar si algun trigger reasigna id_via al guardar la coordenada.
SELECT event_object_table, trigger_name, action_statement
FROM information_schema.triggers
WHERE event_object_schema = 'public'
  AND event_object_table IN ('reporte', 'coordenada');

ROLLBACK;
