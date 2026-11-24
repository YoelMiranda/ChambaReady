import { useSugerirPreguntas } from "../../api/consultas";
import type { PreguntaFiltroNueva, Requisito } from "../../api/tipos";
import { Button } from "../../design/componentes/Basicos";
import { Campo } from "../../design/componentes/Formularios";
import { MAX_PREGUNTAS_FILTRO } from "../../design/reglas/empresa";

const preguntaVacia = (requisitoIndice: number): PreguntaFiltroNueva => ({ requisitoIndice, enunciado: "", opciones: ["", "", "", ""], indiceCorrecto: 0 });

/**
 * E03 (F6): preguntas filtro de opción múltiple. La IA las sugiere a partir del puesto y los
 * requisitos; la empresa las revisa, corrige y marca la respuesta correcta. Si las deja vacías, el
 * backend las genera solas al publicar.
 */
export function EditorPreguntasFiltro({
  puesto,
  requisitos,
  preguntas,
  onCambio,
  error,
}: {
  puesto: string;
  requisitos: Requisito[];
  preguntas: PreguntaFiltroNueva[];
  onCambio: (preguntas: PreguntaFiltroNueva[]) => void;
  error: string | null;
}) {
  const sugerir = useSugerirPreguntas();
  const requisitosConNombre = requisitos.map((r, i) => ({ r, i })).filter(({ r }) => r.habilidad.trim());

  const cambiar = (i: number, cambios: Partial<PreguntaFiltroNueva>) => onCambio(preguntas.map((p, j) => (j === i ? { ...p, ...cambios } : p)));

  const pedirSugerencias = () =>
    sugerir.mutate(
      { puesto: puesto.trim() || "Practicante", requisitos: requisitosConNombre.map(({ r }) => r) },
      {
        // La API numera los requisitos que le mandamos (solo los que tienen nombre): se traducen a la lista completa.
        onSuccess: (s) => onCambio(s.preguntas.map((p) => ({ ...p, requisitoIndice: requisitosConNombre[p.requisitoIndice]?.i ?? 0 }))),
      },
    );

  return (
    <div className="pila">
      <h3>Preguntas filtro</h3>
      <p className="texto-secundario">
        Se envían a cada postulante (45 s por pregunta) y confirman el requisito si acierta. Si no agregas ninguna, las generamos al publicar.
      </p>
      <div className="fila-acciones">
        <Button variante="secundario" icono="auto_awesome" onClick={pedirSugerencias} disabled={sugerir.isPending || requisitosConNombre.length === 0}>
          {sugerir.isPending ? "Generando…" : "Sugerir preguntas con IA"}
        </Button>
        {preguntas.length < MAX_PREGUNTAS_FILTRO && (
          <Button variante="secundario" icono="add" onClick={() => onCambio([...preguntas, preguntaVacia(requisitosConNombre[0]?.i ?? 0)])}>
            Escribir una pregunta
          </Button>
        )}
      </div>
      {sugerir.data && !sugerir.data.conIa && (
        <span className="campo__ayuda" role="status">
          La IA no está configurada en el servidor: son preguntas de ejemplo. Revísalas y corrígelas.
        </span>
      )}
      {sugerir.isError && <span className="campo__error" role="alert">{sugerir.error.message}</span>}

      {preguntas.map((p, i) => (
        <fieldset key={i} className="tarjeta pila">
          <legend className="campo__titulo">Pregunta {i + 1}</legend>
          <label className="campo">
            <span className="campo__titulo">Requisito que confirma</span>
            <select value={p.requisitoIndice} onChange={(e) => cambiar(i, { requisitoIndice: Number(e.target.value) })}>
              {requisitosConNombre.map(({ r, i: indice }) => (
                <option key={r.id} value={indice}>
                  {r.habilidad} {r.filtro ? "(filtro)" : ""}
                </option>
              ))}
            </select>
          </label>
          <Campo etiqueta="Enunciado" value={p.enunciado} onChange={(e) => cambiar(i, { enunciado: e.target.value })} />
          <div role="radiogroup" aria-label={`Opciones de la pregunta ${i + 1}`} className="pila">
            {p.opciones.map((opcion, k) => (
              <div key={k} className="fila-acciones">
                <input
                  type="radio"
                  name={`correcta-${i}`}
                  checked={p.indiceCorrecto === k}
                  onChange={() => cambiar(i, { indiceCorrecto: k })}
                  aria-label={`La opción ${k + 1} es la correcta`}
                  style={{ width: 22, height: 22, minWidth: 0 }}
                />
                <div style={{ flex: 1 }}>
                  <Campo
                    etiqueta={`Opción ${k + 1}${p.indiceCorrecto === k ? " (correcta)" : ""}`}
                    value={opcion}
                    onChange={(e) => cambiar(i, { opciones: p.opciones.map((o, j) => (j === k ? e.target.value : o)) })}
                  />
                </div>
              </div>
            ))}
          </div>
          <div>
            <Button variante="secundario" icono="delete" onClick={() => onCambio(preguntas.filter((_, j) => j !== i))}>
              Quitar pregunta
            </Button>
          </div>
        </fieldset>
      ))}
      {error && <span className="campo__error" role="alert">{error}</span>}
    </div>
  );
}
