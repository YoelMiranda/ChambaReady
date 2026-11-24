-- Catálogos y configuración inicial (Flyway V2). Los precios se cambian aquí o con un UPDATE, no en el código.

INSERT INTO configuracion (clave, valor) VALUES
    ('precio_premium_centimos', '999'),        -- Premium del estudiante: S/ 9.99 al mes (IGV incluido)
    ('precio_plan_empresa_centimos', '14900'), -- Plan Empresa: S/ 149 al mes (IGV incluido)
    ('evaluaciones_gratis', '20');             -- Prueba gratis por empresa, una sola vez

INSERT INTO areas_interes (codigo, nombre) VALUES
    ('DATA_ANALYTICS', 'Data y Analytics'),
    ('DESARROLLO_SOFTWARE', 'Desarrollo de software'),
    ('MARKETING_DIGITAL', 'Marketing digital'),
    ('FINANZAS', 'Finanzas'),
    ('OPERACIONES_LOGISTICA', 'Operaciones y logística'),
    ('DISENO_UX_UI', 'Diseño UX/UI'),
    ('RECURSOS_HUMANOS', 'Recursos humanos'),
    ('VENTAS_COMERCIAL', 'Ventas y comercial');

-- Mismos ids que la app usa en sus datos de prueba.
INSERT INTO habilidades (id, nombre, tipo) VALUES
    ('sql', 'SQL', 'TECNICA'),
    ('excel', 'Excel', 'TECNICA'),
    ('powerbi', 'Power BI', 'TECNICA'),
    ('python', 'Python', 'TECNICA'),
    ('estadistica', 'Estadística', 'TECNICA'),
    ('kotlin', 'Kotlin', 'TECNICA'),
    ('git', 'Git', 'TECNICA'),
    ('marketing', 'Marketing digital', 'TECNICA'),
    ('ads', 'Google Ads', 'TECNICA'),
    ('finanzas', 'Finanzas', 'TECNICA'),
    ('comunicacion', 'Comunicación', 'BLANDA'),
    ('equipo', 'Trabajo en equipo', 'BLANDA');

-- Regla 8: cursos siempre externos (link + horas estimadas por nivel).
INSERT INTO cursos_externos (habilidad_id, titulo, proveedor, url, horas_por_nivel) VALUES
    ('sql', 'SQL para análisis de datos', 'Coursera', 'https://www.coursera.org/search?query=sql', 6),
    ('excel', 'Excel intermedio y avanzado', 'Coursera', 'https://www.coursera.org/search?query=excel', 5),
    ('powerbi', 'Introducción a Power BI', 'Coursera', 'https://www.coursera.org/search?query=power%20bi', 6),
    ('python', 'Python para todos', 'Coursera', 'https://www.coursera.org/search?query=python', 8),
    ('estadistica', 'Estadística básica', 'Coursera', 'https://www.coursera.org/search?query=statistics', 6),
    ('kotlin', 'Kotlin para Android', 'Coursera', 'https://www.coursera.org/search?query=kotlin', 10),
    ('git', 'Git y GitHub', 'Coursera', 'https://www.coursera.org/search?query=git', 3),
    ('marketing', 'Fundamentos de marketing digital', 'Coursera', 'https://www.coursera.org/search?query=digital%20marketing', 6),
    ('ads', 'Google Ads desde cero', 'Coursera', 'https://www.coursera.org/search?query=google%20ads', 4),
    ('finanzas', 'Finanzas para no financieros', 'Coursera', 'https://www.coursera.org/search?query=finance', 6),
    ('comunicacion', 'Comunicación efectiva', 'Coursera', 'https://www.coursera.org/search?query=communication', 4),
    ('equipo', 'Trabajo en equipo', 'Coursera', 'https://www.coursera.org/search?query=teamwork', 3);
