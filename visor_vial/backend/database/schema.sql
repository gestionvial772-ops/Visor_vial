-- =====================================================================
-- Esquema de base de datos
-- Sistema web de reporte de incidencias en infraestructura vial
-- Trujillo, 2026
-- Motor: PostgreSQL 15+ con extensión PostGIS
-- Ejecutar en: Supabase Dashboard -> SQL Editor
-- =====================================================================

CREATE EXTENSION IF NOT EXISTS postgis;

-- =====================================================================
-- 1. USUARIO
-- No es la unidad de estudio (el ciudadano no se mide), solo soporta
-- autenticación y la regla de negocio "1 reporte por día".
-- =====================================================================
CREATE TABLE usuario (
    id_usuario          SERIAL PRIMARY KEY,
    dni                 CHAR(8)      NOT NULL UNIQUE
                         CHECK (dni ~ '^[0-9]{8}$'),
    nombre              VARCHAR(100) NOT NULL,
    apellidos           VARCHAR(100) NOT NULL,
    correo_electronico  VARCHAR(150) NOT NULL UNIQUE,
    contrasena_hash     VARCHAR(255) NOT NULL,      -- hash + salt (RNF07)
    rol                 VARCHAR(15)  NOT NULL DEFAULT 'CIUDADANO'
                         CHECK (rol IN ('ADMIN', 'CIUDADANO', 'INVESTIGADOR')),
    reportes_hoy        SMALLINT     NOT NULL DEFAULT 0,
    fecha_ultimo_reporte DATE,
    fecha_creacion      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

COMMENT ON COLUMN usuario.reportes_hoy IS
    'Auxiliar de aplicación para límite de 1 reporte/día. No es variable de estudio.';

-- =====================================================================
-- 2. VIA_SEGMENTO
-- Los 30 segmentos viales críticos de la muestra. Necesaria para el
-- mapa temático (RF13); no es la unidad de estudio (el reporte sí lo es).
-- =====================================================================
CREATE TABLE via_segmento (
    id_via           SERIAL PRIMARY KEY,
    codigo_segmento  VARCHAR(20)  NOT NULL UNIQUE,   -- ej. 'VIA-001'
    nombre_via       VARCHAR(200) NOT NULL,
    geom             GEOMETRY(LineString, 4326) NOT NULL,
    fecha_registro   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_via_segmento_geom ON via_segmento USING GIST (geom);

-- =====================================================================
-- 3. REPORTE
-- Tabla núcleo: es la UNIDAD DE ESTUDIO del proyecto. Todas las demás
-- tablas de medición cuelgan de aquí.
-- =====================================================================
CREATE TABLE reporte (
    id_reporte         SERIAL PRIMARY KEY,
    id_usuario         INTEGER NOT NULL REFERENCES usuario(id_usuario)
                        ON DELETE RESTRICT,
    id_via             INTEGER REFERENCES via_segmento(id_via)
                        ON DELETE RESTRICT,
    reporte_original_id INTEGER REFERENCES reporte(id_reporte)
                        ON DELETE SET NULL,           -- control de duplicados

    -- Variable independiente: fase de intervención (O1 tradicional / O2 sistema web)
    es_preprueba       BOOLEAN NOT NULL DEFAULT FALSE,

    imagen_url         VARCHAR(500) NOT NULL,          -- URL de Cloudinary/similar (RF12)
    descripcion        TEXT,

    estado             VARCHAR(12) NOT NULL DEFAULT 'PENDIENTE'
                        CHECK (estado IN ('PENDIENTE','EN_PROCESO','RESUELTO','DUPLICADO')),

    fecha_creacion     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_reporte_preprueba_estado ON reporte (es_preprueba, estado);
CREATE INDEX idx_reporte_via_estado ON reporte (id_via, estado);

COMMENT ON COLUMN reporte.es_preprueba IS
    'TRUE = método tradicional (O1). FALSE = sistema web (O2). Clave para la prueba t de Student.';

-- =====================================================================
-- 4. COORDENADA
-- Dimensión 2 (Precisión de geolocalización, en METROS).
-- =====================================================================
CREATE TABLE coordenada (
    id_coordenada     SERIAL PRIMARY KEY,
    id_reporte        INTEGER NOT NULL UNIQUE
                       REFERENCES reporte(id_reporte) ON DELETE CASCADE,

    punto_capturado   GEOMETRY(Point, 4326) NOT NULL,  -- GPS/Mapbox del dispositivo (RF06)
    punto_control     GEOMETRY(Point, 4326),           -- Ground Truth topográfico

    error_metros      NUMERIC(8,3),                    -- indicador de RNF05
    fecha_calculo     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_coordenada_punto_capturado ON coordenada USING GIST (punto_capturado);
CREATE INDEX idx_coordenada_punto_control ON coordenada USING GIST (punto_control);

COMMENT ON COLUMN coordenada.error_metros IS
    'Distancia geodésica entre punto_capturado y punto_control, reproyectada a SRID métrico (ej. 32717 UTM 17S). Alimenta la Dimensión 2.';

-- =====================================================================
-- 5. CLASIFICACION
-- Dimensión 3 (Precisión de clasificación, ORDINAL: Leve/Moderado/Severo).
-- =====================================================================
CREATE TABLE clasificacion (
    id_clasificacion    SERIAL PRIMARY KEY,
    id_reporte          INTEGER NOT NULL UNIQUE
                         REFERENCES reporte(id_reporte) ON DELETE CASCADE,

    severidad           SMALLINT NOT NULL
                         CHECK (severidad IN (1,2,3)),  -- 1=Leve 2=Moderado 3=Severo
    etiqueta_detectada  VARCHAR(50),                    -- ej. 'Bache' (referencial, no estadístico)
    nivel_confianza     NUMERIC(5,2) NOT NULL
                         CHECK (nivel_confianza BETWEEN 0 AND 100),
    modelo_ia_utilizado VARCHAR(50) NOT NULL DEFAULT 'Gemini 3.5 Flash',
    fecha_procesamiento TIMESTAMPTZ NOT NULL DEFAULT now()
);

COMMENT ON COLUMN clasificacion.severidad IS
    '1=Daño Leve, 2=Daño Moderado, 3=Daño Severo. Valor ordinal exclusivo exigido por la matriz de operacionalización.';

-- =====================================================================
-- 6. REGISTRO_TIEMPO
-- Dimensión 1 (Tiempo de procesamiento, en SEGUNDOS). Logs inalterables.
-- =====================================================================
CREATE TABLE registro_tiempo (
    id_registro   SERIAL PRIMARY KEY,
    id_reporte    INTEGER NOT NULL
                  REFERENCES reporte(id_reporte) ON DELETE CASCADE,
    etapa         VARCHAR(20) NOT NULL
                  CHECK (etapa IN (
                      'INICIO_CAPTURA', 'ENVIO_SERVIDOR',
                      'PROCESAMIENTO_IA', 'GEOPROCESAMIENTO', 'CONSOLIDACION'
                  )),
    "timestamp"   TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT unica_etapa_por_reporte UNIQUE (id_reporte, etapa)
);

CREATE INDEX idx_registro_tiempo_reporte_etapa ON registro_tiempo (id_reporte, etapa);

-- RNF11: impedir UPDATE y DELETE manual sobre los logs -> tabla append-only
CREATE OR REPLACE FUNCTION bloquear_modificacion_log()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'registro_tiempo es una tabla inalterable (RNF11): no se permite % ', TG_OP;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_bloquear_update_registro_tiempo
    BEFORE UPDATE ON registro_tiempo
    FOR EACH ROW EXECUTE FUNCTION bloquear_modificacion_log();

CREATE TRIGGER trg_bloquear_delete_registro_tiempo
    BEFORE DELETE ON registro_tiempo
    FOR EACH ROW EXECUTE FUNCTION bloquear_modificacion_log();

-- =====================================================================
-- 7. TIPO_NOTIFICACION / NOTIFICACION
-- No forman parte de la matriz de operacionalización; se mantienen tal
-- como las diseñó el equipo, sin cambios, por extensibilidad (RNF12).
-- =====================================================================
CREATE TABLE tipo_notificacion (
    id_tipo  SERIAL PRIMARY KEY,
    nombre   VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE notificacion (
    id_notificacion  SERIAL PRIMARY KEY,
    id_tipo          INTEGER NOT NULL REFERENCES tipo_notificacion(id_tipo),
    mensaje          TEXT NOT NULL,
    fecha_creacion   TIMESTAMPTZ NOT NULL DEFAULT now(),
    dirigido_a       VARCHAR(15) NOT NULL
                     CHECK (dirigido_a IN ('ADMIN', 'CIUDADANO', 'INVESTIGADOR'))
);

-- =====================================================================
-- Vista de apoyo: tiempo de procesamiento por reporte (Dimensión 1)
-- Evita recalcular en cada exportación a SPSS.
-- =====================================================================
CREATE OR REPLACE VIEW vw_tiempo_procesamiento AS
SELECT
    r.id_reporte,
    r.es_preprueba,
    EXTRACT(EPOCH FROM (fin."timestamp" - inicio."timestamp")) AS tiempo_procesamiento_segundos
FROM reporte r
LEFT JOIN registro_tiempo inicio
       ON inicio.id_reporte = r.id_reporte AND inicio.etapa = 'INICIO_CAPTURA'
LEFT JOIN registro_tiempo fin
       ON fin.id_reporte = r.id_reporte AND fin.etapa = 'CONSOLIDACION';

-- =====================================================================
-- Vista de apoyo: dataset consolidado para exportar a SPSS
-- (las 3 dimensiones + fase, en una sola fila por reporte)
-- =====================================================================
CREATE OR REPLACE VIEW vw_dataset_spss AS
SELECT
    r.id_reporte,
    r.es_preprueba,
    t.tiempo_procesamiento_segundos,
    c.error_metros,
    cl.severidad
FROM reporte r
LEFT JOIN vw_tiempo_procesamiento t ON t.id_reporte = r.id_reporte
LEFT JOIN coordenada c              ON c.id_reporte = r.id_reporte
LEFT JOIN clasificacion cl          ON cl.id_reporte = r.id_reporte
WHERE r.estado <> 'DUPLICADO';

-- =====================================================================
-- Datos demo para pruebas rápidas del visor de tablas
-- =====================================================================
CREATE OR REPLACE FUNCTION seed_demo_data()
RETURNS void
LANGUAGE plpgsql
AS $$
DECLARE
    v_id_tipo INTEGER;
    v_id_usuario INTEGER;
    v_id_via INTEGER;
    v_id_reporte INTEGER;
BEGIN
    INSERT INTO tipo_notificacion (nombre)
    VALUES ('RECLAMO'), ('SEGUIMIENTO')
    ON CONFLICT (nombre) DO NOTHING;

    SELECT id_tipo INTO v_id_tipo
    FROM tipo_notificacion
    WHERE nombre = 'RECLAMO'
    LIMIT 1;

    INSERT INTO usuario (dni, nombre, apellidos, correo_electronico, contrasena_hash, rol, reportes_hoy)
    VALUES ('12345678', 'Ana', 'García', 'ana.garcia@test.com', 'demo_hash_123', 'CIUDADANO', 1)
    ON CONFLICT (dni) DO NOTHING;

    SELECT id_usuario INTO v_id_usuario
    FROM usuario
    WHERE dni = '12345678'
    LIMIT 1;

    INSERT INTO via_segmento (codigo_segmento, nombre_via, geom)
    VALUES (
        'VIA-001',
        'Av. España',
        ST_GeomFromText('LINESTRING(-79.0498 -8.1167, -79.0480 -8.1182, -79.0465 -8.1200)', 4326)
    )
    ON CONFLICT (codigo_segmento) DO NOTHING;

    SELECT id_via INTO v_id_via
    FROM via_segmento
    WHERE codigo_segmento = 'VIA-001'
    LIMIT 1;

    SELECT id_reporte INTO v_id_reporte
    FROM reporte
    WHERE id_usuario = v_id_usuario
      AND descripcion = 'Bache en calzada frente a la avenida principal'
    LIMIT 1;

    IF v_id_reporte IS NULL THEN
        INSERT INTO reporte (id_usuario, id_via, es_preprueba, imagen_url, descripcion, estado)
        VALUES (
            v_id_usuario,
            v_id_via,
            FALSE,
            'https://images.example.com/reporte-demo.jpg',
            'Bache en calzada frente a la avenida principal',
            'PENDIENTE'
        )
        RETURNING id_reporte INTO v_id_reporte;
    END IF;

    INSERT INTO coordenada (id_reporte, punto_capturado, punto_control, error_metros)
    VALUES (
       v_id_reporte,
       ST_GeomFromText('POINT(-79.0489 -8.1178)', 4326),
       ST_GeomFromText('POINT(-79.0487 -8.1176)', 4326),
       1.250
    )
    ON CONFLICT (id_reporte) DO NOTHING;

    INSERT INTO clasificacion (id_reporte, severidad, etiqueta_detectada, nivel_confianza, modelo_ia_utilizado)
    VALUES (
       v_id_reporte,
       2,
       'Bache',
       92.50,
       'Gemini 3.5 Flash'
    )
    ON CONFLICT (id_reporte) DO NOTHING;

    INSERT INTO registro_tiempo (id_reporte, etapa, "timestamp")
    VALUES
       (v_id_reporte, 'INICIO_CAPTURA', now()),
       (v_id_reporte, 'ENVIO_SERVIDOR', now()),
       (v_id_reporte, 'PROCESAMIENTO_IA', now()),
       (v_id_reporte, 'GEOPROCESAMIENTO', now()),
       (v_id_reporte, 'CONSOLIDACION', now())
    ON CONFLICT (id_reporte, etapa) DO NOTHING;

    INSERT INTO notificacion (id_tipo, mensaje, dirigido_a)
    VALUES (
      v_id_tipo,
      'Reporte de prueba cargado desde el visor de tablas.',
      'CIUDADANO'
    );
END;
$$;
