# Fórmula de probabilidad (regla 3)

ChambaReady **no** usa un modelo de machine learning: la probabilidad es una fórmula transparente
que cualquiera puede revisar. Está implementada en
`android/core/domain/.../reglas/ReglasProbabilidad.kt` y tiene pruebas en `ReglasProbabilidadTest`.
El backend usa el mismo módulo.

## Cálculo

```
probabilidad = 100 × Σ(peso × puntaje) / Σ(peso)
```

| Tipo de requisito | Peso |
|---|---|
| Filtro | 3 |
| Obligatorio | 2 |
| Deseable | 1 |

| Cumplimiento | Puntaje |
|---|---|
| Cumple (nivel igual o mayor al exigido) | 1 |
| Parcial (un nivel por debajo) | 0.5 |
| Falta | 0 |

- Si **falta** (no "parcial") algún requisito **filtro**, el resultado se limita a **39 %**, es decir, siempre queda en la zona Baja.
- Una vacante sin requisitos da 100 %.
- El resultado se redondea al entero más cercano.

## Zonas

| Zona | Rango |
|---|---|
| Baja | menos de 40 % |
| Media | 40 % a 69 % |
| Alta | 70 % o más |

## Ejemplo

Filtro cumplido (3 × 1) + obligatorio parcial (2 × 0.5) + deseable que falta (1 × 0) = 4 de 6 → **67 %, zona Media**.

## Lo que NO cambia la probabilidad

- Pagar Premium (regla 8).
- Terminar un curso: el nivel demostrado solo sube con un mini-test aprobado o una evidencia validada (regla 4).
