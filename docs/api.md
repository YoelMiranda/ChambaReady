# API de ChambaReady (`/v1`)

JSON en todas las respuestas. Fechas en `AAAA-MM-DD`, horas en `HH:MM` y montos en céntimos.
Los errores tienen la forma `{"codigo": "...", "mensaje": "..."}`.

## Autenticación

- Encabezado `Authorization: Bearer <ID token de Firebase>`. El backend lo verifica con Firebase Admin SDK y busca el **rol en su base** (`PRACTICANTE` o `EMPRESA`). Nunca confía en un rol que mande el cliente. Configuración: [firebase.md](firebase.md).
- Una cuenta de Firebase que aún no se registró recibe **403** con código `REGISTRO_PENDIENTE` en los endpoints de rol.
- **Solo en tu computadora** (`MODO_DESARROLLO=true`): si no hay token válido, acepta los encabezados `X-Demo-Usuario`, `X-Demo-Rol` y, opcionalmente, `X-Demo-Nombre`. En Heroku este modo está apagado.
- Una cuenta de un rol que llama a un endpoint del otro rol recibe **403**. Una empresa que pide datos de otra empresa recibe **404**: no se revela que existen.

## Públicos

| Método | Ruta | Qué hace |
|---|---|---|
| GET | `/v1/salud` | Estado de la API y de la base. |
| GET | `/v1/catalogo/areas` | Las 8 áreas de interés. |
| GET | `/v1/catalogo/habilidades` | Catálogo de habilidades. |
| GET | `/v1/configuracion/precios` | Precio de Premium, del Plan Empresa y evaluaciones gratis (tabla `configuracion`). |

## Sesión (token válido, sin rol todavía)

| Método | Ruta | Qué hace |
|---|---|---|
| GET | `/v1/sesion` | `{"registrado": true, "rol": "PRACTICANTE"}` o `{"registrado": false}`. Los clientes lo llaman tras iniciar sesión. |
| POST | `/v1/registro` | `{"nombre": "Ana", "rol": "PRACTICANTE"}` (la web manda `EMPRESA`). Fija el rol **una sola vez**: si ya está registrado responde **409**. |

## Practicante

| Método | Ruta | Qué hace |
|---|---|---|
| GET / PUT | `/v1/yo/perfil` | Datos, área, niveles declarados y demostrados, horas y si es Premium. |
| PUT | `/v1/yo/horas-libres` | `{"horas": {"MONDAY": 1.5, ...}}`. Valida de 0 a 8 h en pasos de 0.5. |
| GET | `/v1/vacantes` | Vacantes abiertas con la **probabilidad** de quien consulta, de mayor a menor. |
| GET | `/v1/vacantes/{id}` | Detalle de una vacante. |
| GET | `/v1/vacantes/{id}/preguntas-filtro` | Preguntas **sin** la respuesta correcta. Lista vacía si la empresa no evalúa o no tiene saldo. |
| GET | `/v1/yo/diagnosticos/{vacanteId}` | Resumen gratis; los cursos solo con Premium (`detalleBloqueado`). |
| POST | `/v1/yo/postulaciones` | Postula: bloquea solo si declara no cumplir un **filtro**, corrige las respuestas en el servidor y descuenta la evaluación de forma atómica. |
| GET | `/v1/yo/postulaciones` | Sus postulaciones, con el motivo de descarte y `preguntasPendientes` (regla 13). |
| POST | `/v1/yo/postulaciones/{id}/respuestas-filtro` | Regla 13: responde las preguntas que pidió la empresa. Gasta la evaluación y confirma los sellos acertados. |
| POST | `/v1/yo/cv` | `{"archivoId"}` de un CV ya subido: lo asigna al perfil y la IA detecta habilidades del catálogo y proyectos. |
| GET | `/v1/yo/mini-tests/{habilidadId}` | 3 preguntas **sin** respuesta para subir al siguiente nivel. Se generan una vez por habilidad y nivel. |
| POST | `/v1/yo/mini-tests/{id}/respuestas` | Corrige en el servidor. Solo aprobar (2 de 3) sube el nivel demostrado (regla 4). |
| POST | `/v1/yo/autoevaluacion` | Guarda el resultado y devuelve el **ranking privado** «Top X %» dentro del área. |
| GET / POST | `/v1/yo/evidencias` | Lista y sube evidencias (certificado con archivo o link). Quedan «En revisión». |
| GET | `/v1/yo/premium` | Estado de Premium y pagos simulados. |
| POST | `/v1/yo/premium/pago-simulado` | **Pago simulado** (no se cobra): Premium por 30 días. |

## Empresa

| Método | Ruta | Qué hace |
|---|---|---|
| GET / PUT | `/v1/empresa/perfil` | Valida el RUC (dígito verificador). Una empresa nueva o con RUC cambiado queda «En revisión». |
| GET | `/v1/empresa/saldo` | Evaluaciones gratis restantes y si tiene el plan. |
| GET / POST | `/v1/empresa/vacantes` | Lista y publica (gratis, pero exige estar **verificada**). Acepta `preguntasFiltro`; si evalúa y no manda ninguna, se generan solas. |
| POST | `/v1/empresa/preguntas-filtro/sugerir` | Preguntas de opción múltiple sugeridas por la IA (una por requisito, máximo 2) para revisarlas antes de publicar. |
| GET | `/v1/empresa/vacantes/{id}/preguntas-filtro` | Preguntas de una vacante propia, **con** la respuesta. |
| PUT | `/v1/empresa/logo` | `{"archivoId"}` de un logo ya subido. |
| GET | `/v1/empresa/plan` | Estado del Plan Empresa, vencimiento y pagos simulados. |
| POST | `/v1/empresa/plan/pago-simulado` | **Pago simulado** (no se cobra): plan por 30 días; pagar antes de que venza suma 30 días. |
| GET | `/v1/empresa/vacantes/{id}/postulantes` | Solo de sus vacantes, ordenados por **% de ajuste**. |
| PATCH | `/v1/empresa/postulaciones/{id}` | Cambia el estado. `DESCARTADA` exige un motivo de 10 caracteres como mínimo. |
| GET | `/v1/empresa/postulaciones/{id}` | Detalle de un postulante (solo si postuló a una vacante suya). |
| POST | `/v1/empresa/postulaciones/{id}/enviar-preguntas` | Con el Plan Empresa, envía las preguntas filtro a quien entró «Sin evaluar» (regla 13). |
| POST | `/v1/empresa/entrevistas` | Agenda: no en días pasados ni domingos, de 9:00 a 18:00 sin cruzar el almuerzo y sin cruces. Pasa la postulación a «Entrevista» y crea la notificación. |
| GET | `/v1/empresa/entrevistas` | Entrevistas agendadas, con el nombre del postulante y el puesto. |
| GET | `/v1/empresa/actividad` | Últimos movimientos para el panel (E02). |

## Compartidas (cualquier cuenta registrada)

| Método | Ruta | Qué hace |
|---|---|---|
| POST | `/v1/archivos?tipo=CV\|EVIDENCIA\|LOGO` | Sube un archivo **tal cual** (no multipart) con su `Content-Type`; nombre en `X-Nombre-Archivo`. Se revisan formato, tamaño (CV y evidencia 5 MB, logo 1 MB), rol y la firma de los primeros bytes. |
| GET | `/v1/archivos/{id}` | Descarga. Lo ve su dueño; un logo, cualquiera; un CV o evidencia, solo la empresa a la que ese practicante postuló. Si no, **404**. |
| GET | `/v1/notificaciones` | Las 50 más nuevas del usuario. |
| GET | `/v1/notificaciones/resumen` | `{"noLeidas": n}`. |
| POST | `/v1/notificaciones/{id}/leida`, `/v1/notificaciones/leer-todas` | Marca como leídas (solo las propias). |

## Administración (encabezado `X-Admin-Token`)

| Método | Ruta | Qué hace |
|---|---|---|
| GET | `/v1/admin/costos-ia` | Tokens y costo de Gemini por operación y modelo (regla 14). |
| POST | `/v1/admin/empresas/{ruc}/verificar` | Marca la empresa como verificada. |
| POST | `/v1/admin/empresas/{ruc}/plan-de-prueba` | Activa el Plan Empresa **sin vencimiento** (demostraciones del equipo). |
| POST | `/v1/admin/evidencias/{id}/validar` | Valida una evidencia: sube el nivel demostrado y avisa al practicante. |
| GET | `/v1/admin/metricas` | Eventos de los últimos 30 días, totales, % evaluadas, % de mini-tests aprobados y costo de IA. |
