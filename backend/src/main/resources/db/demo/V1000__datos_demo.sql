-- Datos de prueba SOLO para desarrollo (se cargan con CARGAR_DATOS_DEMO=true). Todo es ficticio.
-- Usa los mismos ids de vacante que los datos de prueba de la app (v1 … v5).
-- Para entrar como esta empresa en local: encabezados X-Demo-Usuario: demo-empresa y X-Demo-Rol: EMPRESA.

INSERT INTO usuarios (id, firebase_uid, rol, nombre, correo) VALUES
    ('usuario-demo-empresa', 'demo-empresa', 'EMPRESA', 'Equipo de Talento', 'talento@ejemplo.com');

INSERT INTO empresas (id, usuario_id, razon_social, ruc, sector, descripcion, contacto_correo, verificacion) VALUES
    ('empresa-demo', 'usuario-demo-empresa', 'Banco Andino S.A.A. (ficticia)', '20131312955', 'Banca y finanzas',
     'Empresa ficticia para probar ChambaReady.', 'talento@ejemplo.com', 'VERIFICADA');

INSERT INTO saldo_evaluaciones (empresa_id, gratis_restantes, plan_activo) VALUES ('empresa-demo', 4, false);

INSERT INTO vacantes (id, empresa_id, puesto, area_interes, modalidad, sueldo, fecha_cierre, descripcion, que_hara, evaluacion_activa) VALUES
    ('v1', 'empresa-demo', 'Practicante de Analítica de Datos', 'DATA_ANALYTICS', 'HIBRIDA', 'S/ 1,300', current_date + 12,
     'Apoyo al equipo de Inteligencia Comercial en reportes y análisis de clientes.',
     E'Preparar reportes semanales de ventas con SQL y Excel.\nActualizar tableros de seguimiento comercial.', true),
    ('v5', 'empresa-demo', 'Practicante de Datos Junior', 'DATA_ANALYTICS', 'PRESENCIAL', NULL, current_date + 2,
     'Control de indicadores de despacho en el centro de distribución.',
     E'Consolidar indicadores diarios en Excel.\nHacer consultas simples a la base de datos.', true),
    ('v4', 'empresa-demo', 'Practicante de Finanzas Corporativas', 'FINANZAS', 'HIBRIDA', 'S/ 1,400', current_date + 25,
     'Soporte al área de Planeamiento Financiero.', E'Armar modelos de presupuesto en Excel.', true);

INSERT INTO requisitos_vacante (id, vacante_id, habilidad_id, nivel, tipo, orden) VALUES
    ('v1r1', 'v1', 'sql', 'INTERMEDIO', 'FILTRO', 0),
    ('v1r2', 'v1', 'excel', 'AVANZADO', 'OBLIGATORIO', 1),
    ('v1r3', 'v1', 'powerbi', 'BASICO', 'DESEABLE', 2),
    ('v1r4', 'v1', 'comunicacion', 'INTERMEDIO', 'DESEABLE', 3),
    ('v5r1', 'v5', 'sql', 'BASICO', 'FILTRO', 0),
    ('v5r2', 'v5', 'excel', 'INTERMEDIO', 'OBLIGATORIO', 1),
    ('v5r3', 'v5', 'python', 'BASICO', 'DESEABLE', 2),
    ('v4r1', 'v4', 'excel', 'INTERMEDIO', 'OBLIGATORIO', 0),
    ('v4r2', 'v4', 'finanzas', 'BASICO', 'OBLIGATORIO', 1),
    ('v4r3', 'v4', 'estadistica', 'BASICO', 'DESEABLE', 2);

-- En F6 estas preguntas las genera Gemini. Opciones separadas por salto de línea.
INSERT INTO preguntas_filtro (id, vacante_id, requisito_id, enunciado, opciones, indice_correcto, orden) VALUES
    ('f-v1r1', 'v1', 'v1r1', '¿Qué devuelve SELECT region, SUM(monto) FROM ventas GROUP BY region?',
     E'Una fila por venta\nEl total de ventas por región\nSolo la región con más ventas\nNo lo sé', 1, 0),
    ('f-v1r2', 'v1', 'v1r2', '¿Qué hace el signo $ en =A$1?',
     E'Convierte a soles\nFija la fila al copiar la fórmula\nFija la columna\nNo lo sé', 1, 1),
    ('f-v5r1', 'v5', 'v5r1', '¿Qué cláusula filtra filas antes de agrupar?',
     E'HAVING\nWHERE\nORDER BY\nNo lo sé', 1, 0);
