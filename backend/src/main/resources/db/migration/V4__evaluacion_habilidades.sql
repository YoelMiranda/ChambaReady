-- Evaluación de habilidades (P05 y Perfil): el estudiante elige hasta 4 habilidades declaradas y
-- responde un mini-test de cada una, al nivel que declaró. Con cuenta gratis, una sola vez.
CREATE TABLE evaluaciones_habilidades (
    id           text PRIMARY KEY DEFAULT gen_random_uuid()::text,
    usuario_id   text NOT NULL REFERENCES usuarios (id) ON DELETE CASCADE,
    -- Ids de las habilidades evaluadas, separados por coma.
    habilidades  text NOT NULL,
    creada_en    timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX evaluaciones_habilidades_usuario_idx ON evaluaciones_habilidades (usuario_id);

-- Qué mini-test respondió cada intento: así quien repite recibe preguntas nuevas.
ALTER TABLE intentos_test ADD COLUMN mini_test_id text REFERENCES mini_tests (id) ON DELETE SET NULL;
