# Modelo de cobro (reglas 6, 9 a 14)

> **Pagos simulados (decisión del equipo, F8):** no hay pasarela de pago y nadie paga de verdad.
> «Pagar» registra un pago de prueba con el precio de la tabla de configuración y activa el plan por
> `dias_por_pago` días (30). Pagar antes de que venza suma 30 días más. Cuando vence, el plan se apaga
> solo. El historial se ve en E06 (web) y en `GET /v1/yo/premium` (app).

Las reglas están en `android/core/domain/.../empresa/` (`ReglasEvaluaciones` y
`CalculadoraPrecioEmpresa`), con pruebas en `EvaluacionesEmpresaTest`. El backend usa el mismo módulo.
**Los precios no están en el código**: viven en la tabla de configuración (montos en céntimos de
sol, IGV incluido) y se cambian sin publicar una versión nueva.

## Precios (valores iniciales de la tabla de configuración)

| Quién | Plan | Precio mensual (IGV incluido) | Qué incluye |
|---|---|---|---|
| Estudiante | Premium | **S/ 9,99** | Detalle del diagnóstico, plan, seguimiento, Mis pendientes y editar datos y habilidades |
| Empresa | Plan Empresa | **S/ 149** | Test de preguntas filtro para **todos** los postulantes de **todas** sus vacantes, sin límite |

Publicar vacantes siempre es gratis. Pagar nunca mejora la posición de un estudiante ante las empresas.

## Empresa: cómo funciona

1. **Prueba gratis: 20 evaluaciones, una sola vez**, para todas sus vacantes. Sirve para que la empresa vea cómo funciona el test con sus primeros postulantes.
2. **Una evaluación** = un postulante que responde las 2 preguntas filtro (**opción múltiple**) de una vacante. Cuenta por postulación: si un estudiante postula a 2 vacantes, son 2 evaluaciones.
3. **Al acabarse las gratis**, los nuevos postulantes postulan **sin test**. Sus requisitos quedan como «Declarado» y la empresa los ve como «Sin evaluar»: no puede comprobar sus habilidades.
4. **Con el Plan Empresa**, todos los postulantes responden el test y la empresa puede enviárselo también a los que entraron «Sin evaluar». Mientras el plan está activo, las gratis que sobraban se conservan por si cancela.

El backend descuenta las gratis dentro de una transacción (`UPDATE … WHERE gratis_restantes > 0 RETURNING`)
para que dos postulaciones simultáneas no usen la misma evaluación.

## Por qué un precio fijo con evaluaciones ilimitadas no es riesgoso

- Las preguntas son de opción múltiple: **corregirlas no usa IA**, así que evaluar a un postulante cuesta prácticamente S/ 0.
- La IA solo **genera las 2 preguntas, una vez por vacante**. Con `gemini-3.5-flash-lite` (US$0,30 / US$2,50 por millón de tokens) eso cuesta menos de S/ 0,01 por vacante, postulen 10 o 1 000 estudiantes.
- Para evitar abusos se limita cuántas veces se pueden regenerar las preguntas de una vacante.

## Calculadora de E06

«¿Cuántos postulantes recibes al mes?» → precio por postulante evaluado = precio fijo ÷ postulantes.

| Postulantes al mes | Precio por evaluado (con S/ 149) |
|---|---|
| 100 | S/ 1,49 |
| 300 | S/ 0,50 |
| 1 000 | S/ 0,15 |

## Margen (regla 14, solo administración)

Un endpoint de administración muestra, por empresa y por mes:

```
margen = precio / 1,18 (sin IGV) − comisión de la pasarela − costo de IA registrado en uso_ia
```

Ejemplo con una comisión **de ejemplo** del 5 % (si algún día se usa una pasarela real; hoy los
pagos son simulados y la comisión es 0): S/ 126,27 − S/ 7,45 − S/ 0,50 = **S/ 118,32**.
