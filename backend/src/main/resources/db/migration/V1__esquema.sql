-- =====================================================================================
-- ChambaReady · esquema inicial (Flyway V1). PostgreSQL 15+ (Neon).
-- Reglas para el equipo:
--   * NUNCA edites una migración ya aplicada: crea una nueva (V2__..., V3__...).
--   * Los identificadores son text con un UUID generado por la base (gen_random_uuid()).
--   * Los valores de "enum" se guardan como texto con CHECK: más fácil de leer y de migrar.
-- =====================================================================================

-- ---------- Usuarios y catálogos ----------
CREATE TABLE usuarios (
    id            text PRIMARY KEY DEFAULT gen_random_uuid()::text,
    firebase_uid  text NOT NULL UNIQUE,
    rol           text NOT NULL CHECK (rol IN ('PRACTICANTE', 'EMPRESA')),
    nombre        text NOT NULL DEFAULT '',
    correo        text NOT NULL DEFAULT '',
    creado_en     timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE areas_interes (
    codigo  text PRIMARY KEY,
    nombre  text NOT NULL
);

CREATE TABLE habilidades (
    id      text PRIMARY KEY,
    nombre  text NOT NULL UNIQUE,
    tipo    text NOT NULL CHECK (tipo IN ('TECNICA', 'BLANDA'))
);

-- Precios y límites (regla: los precios NO van en el código).
CREATE TABLE configuracion (
    clave  text PRIMARY KEY,
    valor  text NOT NULL
);

-- ---------- Practicante ----------
CREATE TABLE perfiles_practicante (
    usuario_id          text PRIMARY KEY REFERENCES usuarios (id) ON DELETE CASCADE,
    universidad         text NOT NULL DEFAULT '',
    carrera             text NOT NULL DEFAULT '',
    ciclo               integer CHECK (ciclo BETWEEN 1 AND 14),
    area_interes        text REFERENCES areas_interes (codigo),
    cv_r2_key           text,
    onboarding_completo boolean NOT NULL DEFAULT false,
    actualizado_en      timestamptz NOT NULL DEFAULT now()
);

-- Regla 1: horas distintas por día, de 0 a 8 en pasos de 0.5. dia_semana: 1 = lunes … 7 = domingo.
CREATE TABLE horas_libres (
    usuario_id  text NOT NULL REFERENCES usuarios (id) ON DELETE CASCADE,
    dia_semana  smallint NOT NULL CHECK (dia_semana BETWEEN 1 AND 7),
    horas       numeric(3, 1) NOT NULL CHECK (horas >= 0 AND horas <= 8 AND horas * 2 = trunc(horas * 2)),
    PRIMARY KEY (usuario_id, dia_semana)
);

CREATE TABLE habilidades_usuario (
    usuario_id         text NOT NULL REFERENCES usuarios (id) ON DELETE CASCADE,
    habilidad_id       text NOT NULL REFERENCES habilidades (id),
    nivel_declarado    text NOT NULL DEFAULT 'NO_LO_HE_USADO'
        CHECK (nivel_declarado IN ('NO_LO_HE_USADO', 'BASICO', 'INTERMEDIO', 'AVANZADO', 'NO_LO_SE')),
    -- Regla 4: solo sube con mini-test aprobado o evidencia validada.
    nivel_demostrado   text NOT NULL DEFAULT 'NO_LO_HE_USADO'
        CHECK (nivel_demostrado IN ('NO_LO_HE_USADO', 'BASICO', 'INTERMEDIO', 'AVANZADO')),
    fuente_demostrado  text CHECK (fuente_demostrado IN ('MINI_TEST', 'EVIDENCIA')),
    actualizado_en     timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (usuario_id, habilidad_id)
);

CREATE TABLE evidencias (
    id            text PRIMARY KEY DEFAULT gen_random_uuid()::text,
    usuario_id    text NOT NULL REFERENCES usuarios (id) ON DELETE CASCADE,
    habilidad_id  text NOT NULL REFERENCES habilidades (id),
    titulo        text NOT NULL,
    tipo          text NOT NULL CHECK (tipo IN ('CERTIFICADO', 'LINK')),
    estado        text NOT NULL DEFAULT 'EN_REVISION' CHECK (estado IN ('VALIDADA', 'EN_REVISION', 'LEIDO')),
    nivel         text NOT NULL CHECK (nivel IN ('BASICO', 'INTERMEDIO', 'AVANZADO')),
    r2_key        text,
    url           text,
    creado_en     timestamptz NOT NULL DEFAULT now()
);

-- Ranking privado por área (regla 5): nunca se expone a empresas.
CREATE TABLE snapshots_ranking (
    id              text PRIMARY KEY DEFAULT gen_random_uuid()::text,
    usuario_id      text NOT NULL REFERENCES usuarios (id) ON DELETE CASCADE,
    area_interes    text NOT NULL REFERENCES areas_interes (codigo),
    top_porcentaje  integer NOT NULL CHECK (top_porcentaje BETWEEN 1 AND 100),
    creado_en       timestamptz NOT NULL DEFAULT now()
);

-- ---------- Empresa ----------
CREATE TABLE empresas (
    id                text PRIMARY KEY DEFAULT gen_random_uuid()::text,
    usuario_id        text NOT NULL UNIQUE REFERENCES usuarios (id) ON DELETE CASCADE,
    razon_social      text NOT NULL,
    ruc               char(11) NOT NULL UNIQUE CHECK (ruc ~ '^[0-9]{11}$'),
    sector            text NOT NULL DEFAULT '',
    web               text NOT NULL DEFAULT '',
    tamano            text NOT NULL DEFAULT '',
    contacto_nombre   text NOT NULL DEFAULT '',
    contacto_correo   text NOT NULL DEFAULT '',
    contacto_telefono text NOT NULL DEFAULT '',
    descripcion       text NOT NULL DEFAULT '',
    logo_r2_key       text,
    verificacion      text NOT NULL DEFAULT 'EN_REVISION' CHECK (verificacion IN ('SIN_VERIFICAR', 'EN_REVISION', 'VERIFICADA')),
    creado_en         timestamptz NOT NULL DEFAULT now()
);

-- Reglas 9 a 13: 20 evaluaciones gratis una sola vez + Plan Empresa (precio fijo, ilimitadas).
CREATE TABLE saldo_evaluaciones (
    empresa_id        text PRIMARY KEY REFERENCES empresas (id) ON DELETE CASCADE,
    gratis_restantes  integer NOT NULL CHECK (gratis_restantes >= 0),
    plan_activo       boolean NOT NULL DEFAULT false,
    actualizado_en    timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE suscripciones_empresa (
    id                 text PRIMARY KEY DEFAULT gen_random_uuid()::text,
    empresa_id         text NOT NULL REFERENCES empresas (id) ON DELETE CASCADE,
    mp_preapproval_id  text UNIQUE,
    estado             text NOT NULL CHECK (estado IN ('PENDIENTE', 'ACTIVA', 'CANCELADA', 'VENCIDA')),
    precio_centimos    integer NOT NULL CHECK (precio_centimos > 0),
    vence_en           timestamptz,
    creada_en          timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE suscripciones_estudiante (
    id                 text PRIMARY KEY DEFAULT gen_random_uuid()::text,
    usuario_id         text NOT NULL REFERENCES usuarios (id) ON DELETE CASCADE,
    mp_preapproval_id  text UNIQUE,
    estado             text NOT NULL CHECK (estado IN ('PENDIENTE', 'ACTIVA', 'CANCELADA', 'VENCIDA')),
    precio_centimos    integer NOT NULL CHECK (precio_centimos > 0),
    vence_en           timestamptz,
    creada_en          timestamptz NOT NULL DEFAULT now()
);

-- Cada aviso de Mercado Pago se procesa una sola vez (idempotencia).
CREATE TABLE webhooks_mp (
    id           text PRIMARY KEY,
    tipo         text NOT NULL,
    recibido_en  timestamptz NOT NULL DEFAULT now(),
    procesado    boolean NOT NULL DEFAULT false
);

-- ---------- Vacantes ----------
CREATE TABLE vacantes (
    id                 text PRIMARY KEY DEFAULT gen_random_uuid()::text,
    empresa_id         text NOT NULL REFERENCES empresas (id) ON DELETE CASCADE,
    puesto             text NOT NULL,
    area_interes       text NOT NULL REFERENCES areas_interes (codigo),
    modalidad          text NOT NULL CHECK (modalidad IN ('PRESENCIAL', 'HIBRIDA', 'REMOTA')),
    sueldo             text,
    fecha_cierre       date NOT NULL,
    descripcion        text NOT NULL,
    -- Tareas separadas por saltos de línea (qué hará el practicante).
    que_hara           text NOT NULL DEFAULT '',
    evaluacion_activa  boolean NOT NULL DEFAULT true,
    creada_en          timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX vacantes_cierre_idx ON vacantes (fecha_cierre);

CREATE TABLE requisitos_vacante (
    id                text PRIMARY KEY DEFAULT gen_random_uuid()::text,
    vacante_id        text NOT NULL REFERENCES vacantes (id) ON DELETE CASCADE,
    habilidad_id      text NOT NULL REFERENCES habilidades (id),
    nivel             text NOT NULL CHECK (nivel IN ('BASICO', 'INTERMEDIO', 'AVANZADO')),
    -- Un requisito marcado como filtro en E03 se guarda como FILTRO (y es obligatorio).
    tipo              text NOT NULL CHECK (tipo IN ('FILTRO', 'OBLIGATORIO', 'DESEABLE')),
    orden             integer NOT NULL DEFAULT 0
);
CREATE INDEX requisitos_vacante_idx ON requisitos_vacante (vacante_id);

-- Preguntas filtro de opción múltiple (en F6 las genera Gemini una vez por vacante).
CREATE TABLE preguntas_filtro (
    id               text PRIMARY KEY DEFAULT gen_random_uuid()::text,
    vacante_id       text NOT NULL REFERENCES vacantes (id) ON DELETE CASCADE,
    requisito_id     text NOT NULL REFERENCES requisitos_vacante (id) ON DELETE CASCADE,
    enunciado        text NOT NULL,
    -- Opciones separadas por saltos de línea.
    opciones         text NOT NULL,
    indice_correcto  integer NOT NULL CHECK (indice_correcto >= 0),
    orden            integer NOT NULL DEFAULT 0
);

-- ---------- Preparación del practicante ----------
CREATE TABLE cursos_externos (
    habilidad_id     text PRIMARY KEY REFERENCES habilidades (id),
    titulo           text NOT NULL,
    proveedor        text NOT NULL,
    url              text NOT NULL,
    horas_por_nivel  numeric(5, 1) NOT NULL CHECK (horas_por_nivel > 0)
);

CREATE TABLE diagnosticos (
    id                   text PRIMARY KEY DEFAULT gen_random_uuid()::text,
    usuario_id           text NOT NULL REFERENCES usuarios (id) ON DELETE CASCADE,
    vacante_id           text NOT NULL REFERENCES vacantes (id) ON DELETE CASCADE,
    probabilidad_actual  integer NOT NULL,
    probabilidad_meta    integer NOT NULL,
    horas_necesarias     numeric(6, 1) NOT NULL,
    horas_disponibles    numeric(6, 1) NOT NULL,
    viable               boolean NOT NULL,
    creado_en            timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE planes (
    id          text PRIMARY KEY DEFAULT gen_random_uuid()::text,
    usuario_id  text NOT NULL REFERENCES usuarios (id) ON DELETE CASCADE,
    vacante_id  text NOT NULL REFERENCES vacantes (id) ON DELETE CASCADE,
    activo      boolean NOT NULL DEFAULT true,
    es_meta     boolean NOT NULL DEFAULT false,
    creado_en   timestamptz NOT NULL DEFAULT now(),
    UNIQUE (usuario_id, vacante_id)
);

-- Métricas: adherencia (completadas ÷ asignadas) y tiempo real vs. estimado (inicio y fin).
CREATE TABLE tareas_plan (
    id              text PRIMARY KEY DEFAULT gen_random_uuid()::text,
    plan_id         text NOT NULL REFERENCES planes (id) ON DELETE CASCADE,
    modulo_id       text NOT NULL REFERENCES habilidades (id),
    titulo          text NOT NULL,
    horas_estimadas numeric(4, 1) NOT NULL,
    semana          integer NOT NULL CHECK (semana >= 1),
    curso_url       text,
    iniciada_en     timestamptz,
    completada_en   timestamptz
);

CREATE TABLE mini_tests (
    id             text PRIMARY KEY DEFAULT gen_random_uuid()::text,
    habilidad_id   text NOT NULL REFERENCES habilidades (id),
    nivel          text NOT NULL CHECK (nivel IN ('BASICO', 'INTERMEDIO', 'AVANZADO')),
    -- Preguntas en JSON (generadas en F6 y validadas con esquema).
    preguntas_json text NOT NULL,
    creado_en      timestamptz NOT NULL DEFAULT now()
);

-- Métrica de precisión de reevaluación: nivel antes vs. resultado objetivo.
CREATE TABLE intentos_test (
    id             text PRIMARY KEY DEFAULT gen_random_uuid()::text,
    usuario_id     text NOT NULL REFERENCES usuarios (id) ON DELETE CASCADE,
    habilidad_id   text NOT NULL REFERENCES habilidades (id),
    vacante_id     text REFERENCES vacantes (id) ON DELETE SET NULL,
    correctas      integer NOT NULL,
    total          integer NOT NULL CHECK (total > 0),
    aprobado       boolean NOT NULL,
    nivel_antes    text NOT NULL,
    nivel_despues  text NOT NULL,
    creado_en      timestamptz NOT NULL DEFAULT now()
);

-- ---------- Postulaciones ----------
CREATE TABLE postulaciones (
    id                   text PRIMARY KEY DEFAULT gen_random_uuid()::text,
    vacante_id           text NOT NULL REFERENCES vacantes (id) ON DELETE CASCADE,
    usuario_id           text NOT NULL REFERENCES usuarios (id) ON DELETE CASCADE,
    estado               text NOT NULL DEFAULT 'NUEVA' CHECK (estado IN ('NUEVA', 'REVISION', 'ENTREVISTA', 'ELEGIDA', 'DESCARTADA')),
    -- El practicante ve el motivo: es obligatorio al descartar.
    motivo_descarte      text,
    evaluada             boolean NOT NULL DEFAULT false,
    preguntas_correctas  integer,
    creada_en            timestamptz NOT NULL DEFAULT now(),
    UNIQUE (vacante_id, usuario_id),
    CHECK (estado <> 'DESCARTADA' OR length(trim(coalesce(motivo_descarte, ''))) >= 10)
);
CREATE INDEX postulaciones_vacante_idx ON postulaciones (vacante_id);

CREATE TABLE sellos_postulacion (
    postulacion_id  text NOT NULL REFERENCES postulaciones (id) ON DELETE CASCADE,
    requisito_id    text NOT NULL REFERENCES requisitos_vacante (id) ON DELETE CASCADE,
    sello           text NOT NULL CHECK (sello IN ('CONFIRMADO', 'DECLARADO', 'NO_CUMPLE')),
    PRIMARY KEY (postulacion_id, requisito_id)
);

CREATE TABLE respuestas_filtro (
    id              text PRIMARY KEY DEFAULT gen_random_uuid()::text,
    postulacion_id  text NOT NULL REFERENCES postulaciones (id) ON DELETE CASCADE,
    pregunta_id     text NOT NULL REFERENCES preguntas_filtro (id) ON DELETE CASCADE,
    indice_elegido  integer,
    correcta        boolean NOT NULL
);

-- Libro de movimientos: cada evaluación descontada queda registrada (auditoría y métricas).
CREATE TABLE movimientos_evaluacion (
    id              text PRIMARY KEY DEFAULT gen_random_uuid()::text,
    empresa_id      text NOT NULL REFERENCES empresas (id) ON DELETE CASCADE,
    postulacion_id  text NOT NULL REFERENCES postulaciones (id) ON DELETE CASCADE,
    fuente          text NOT NULL CHECK (fuente IN ('PRUEBA_GRATIS', 'PLAN')),
    creado_en       timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE entrevistas (
    id              text PRIMARY KEY DEFAULT gen_random_uuid()::text,
    postulacion_id  text NOT NULL REFERENCES postulaciones (id) ON DELETE CASCADE,
    empresa_id      text NOT NULL REFERENCES empresas (id) ON DELETE CASCADE,
    fecha           date NOT NULL,
    hora            time NOT NULL,
    duracion_min    integer NOT NULL CHECK (duracion_min IN (30, 45, 60)),
    modalidad       text NOT NULL CHECK (modalidad IN ('VIDEOLLAMADA', 'PRESENCIAL')),
    mensaje         text NOT NULL DEFAULT '',
    creada_en       timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX entrevistas_empresa_fecha_idx ON entrevistas (empresa_id, fecha);

-- ---------- Notificaciones y métricas ----------
CREATE TABLE notificaciones (
    id          text PRIMARY KEY DEFAULT gen_random_uuid()::text,
    usuario_id  text NOT NULL REFERENCES usuarios (id) ON DELETE CASCADE,
    tipo        text NOT NULL CHECK (tipo IN ('CIERRE_CERCANO', 'CAMBIO_ESTADO', 'ENTREVISTA', 'PLAN_SEMANAL', 'NUEVA_VACANTE', 'RANKING')),
    titulo      text NOT NULL,
    cuerpo      text NOT NULL,
    vacante_id  text REFERENCES vacantes (id) ON DELETE SET NULL,
    leida       boolean NOT NULL DEFAULT false,
    creada_en   timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX notificaciones_usuario_idx ON notificaciones (usuario_id, creada_en DESC);

CREATE TABLE dispositivos_fcm (
    token       text PRIMARY KEY,
    usuario_id  text NOT NULL REFERENCES usuarios (id) ON DELETE CASCADE,
    creado_en   timestamptz NOT NULL DEFAULT now()
);

-- Regla 14: tokens y costo de cada llamada a Gemini (solo visible en administración).
CREATE TABLE uso_ia (
    id              text PRIMARY KEY DEFAULT gen_random_uuid()::text,
    usuario_id      text REFERENCES usuarios (id) ON DELETE SET NULL,
    empresa_id      text REFERENCES empresas (id) ON DELETE SET NULL,
    operacion       text NOT NULL,
    modelo          text NOT NULL,
    tokens_entrada  integer NOT NULL CHECK (tokens_entrada >= 0),
    tokens_salida   integer NOT NULL CHECK (tokens_salida >= 0),
    costo_usd       numeric(12, 6) NOT NULL DEFAULT 0,
    creado_en       timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE eventos_metricas (
    id          text PRIMARY KEY DEFAULT gen_random_uuid()::text,
    usuario_id  text REFERENCES usuarios (id) ON DELETE SET NULL,
    tipo        text NOT NULL,
    datos_json  text NOT NULL DEFAULT '{}',
    creado_en   timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX eventos_metricas_tipo_idx ON eventos_metricas (tipo, creado_en);
