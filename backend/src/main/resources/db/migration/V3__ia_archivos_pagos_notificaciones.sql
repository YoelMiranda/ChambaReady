-- F6 a F9. Flyway la aplica sola al arrancar la API (nunca edites una migración ya aplicada:
-- crea una nueva V4__... para el próximo cambio).

-- ---------- F7: archivos (simulación de Cloudflare R2) ----------
-- Los archivos se guardan en la misma base (columna bytea). Para un proyecto de clase basta: el plan
-- gratis de Neon da 1 GB, y los límites (CV 5 MB, logo 1 MB) lo mantienen chico. En producción se
-- cambiaría por R2 sin tocar los endpoints (ver AlmacenArchivos en el backend).
CREATE TABLE archivos (
    id          text PRIMARY KEY DEFAULT gen_random_uuid()::text,
    usuario_id  text NOT NULL REFERENCES usuarios (id) ON DELETE CASCADE,
    tipo        text NOT NULL CHECK (tipo IN ('CV', 'EVIDENCIA', 'LOGO')),
    nombre      text NOT NULL,
    mime        text NOT NULL,
    tamano      integer NOT NULL CHECK (tamano > 0),
    contenido   bytea NOT NULL,
    creado_en   timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX archivos_usuario_idx ON archivos (usuario_id, tipo);

ALTER TABLE perfiles_practicante ADD COLUMN cv_archivo_id text REFERENCES archivos (id) ON DELETE SET NULL;
ALTER TABLE empresas ADD COLUMN logo_archivo_id text REFERENCES archivos (id) ON DELETE SET NULL;
ALTER TABLE evidencias ADD COLUMN archivo_id text REFERENCES archivos (id) ON DELETE SET NULL;
-- La columna r2_key ya no se usa (los archivos viven en la tabla archivos).
ALTER TABLE evidencias DROP COLUMN r2_key;

-- ---------- F6: IA ----------
-- Un mini-test por habilidad y nivel se genera una vez y se reutiliza (ahorra llamadas a Gemini).
CREATE INDEX mini_tests_habilidad_nivel_idx ON mini_tests (habilidad_id, nivel, creado_en DESC);

-- Ranking privado (regla 5): se guarda el puntaje para comparar a todos los del área.
ALTER TABLE snapshots_ranking ADD COLUMN puntaje numeric(4, 3) NOT NULL DEFAULT 0 CHECK (puntaje BETWEEN 0 AND 1);

-- Regla 13: la empresa con plan pide responder las preguntas a quien entró "Sin evaluar".
ALTER TABLE postulaciones ADD COLUMN preguntas_solicitadas boolean NOT NULL DEFAULT false;

-- ---------- F8: pagos simulados (sin Mercado Pago) ----------
DROP TABLE webhooks_mp;
ALTER TABLE suscripciones_empresa DROP COLUMN mp_preapproval_id;
ALTER TABLE suscripciones_estudiante DROP COLUMN mp_preapproval_id;

-- Historial de "pagos". Nadie paga de verdad: cada fila es un pago de prueba con el precio vigente.
CREATE TABLE pagos_simulados (
    id              text PRIMARY KEY DEFAULT gen_random_uuid()::text,
    usuario_id      text NOT NULL REFERENCES usuarios (id) ON DELETE CASCADE,
    concepto        text NOT NULL CHECK (concepto IN ('PLAN_EMPRESA', 'PREMIUM')),
    monto_centimos  integer NOT NULL CHECK (monto_centimos > 0),
    vence_en        timestamptz NOT NULL,
    creado_en       timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX pagos_simulados_usuario_idx ON pagos_simulados (usuario_id, creado_en DESC);

-- Días que dura cada pago simulado (en la tabla de configuración, como los precios).
INSERT INTO configuracion (clave, valor) VALUES ('dias_por_pago', '30');

-- ---------- F9: notificaciones dentro de la app y la web (sin FCM) ----------
ALTER TABLE notificaciones DROP CONSTRAINT notificaciones_tipo_check;
ALTER TABLE notificaciones ADD CONSTRAINT notificaciones_tipo_check CHECK (tipo IN (
    'CIERRE_CERCANO', 'CAMBIO_ESTADO', 'ENTREVISTA', 'PLAN_SEMANAL', 'NUEVA_VACANTE', 'RANKING',
    'NUEVO_POSTULANTE', 'PREGUNTAS_FILTRO', 'PAGO', 'EVIDENCIA'
));
-- Sin push, la tabla de dispositivos no se usa.
DROP TABLE dispositivos_fcm;
