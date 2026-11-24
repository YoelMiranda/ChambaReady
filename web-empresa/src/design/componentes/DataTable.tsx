import type { ReactNode } from "react";

export interface Columna<T> {
  clave: string;
  titulo: string;
  /** Cómo pintar la celda. Si no se da, se muestra `fila[clave]`. */
  render?: (fila: T) => ReactNode;
  alinear?: "izquierda" | "derecha";
}

/**
 * Tabla de datos real (`<table>`), accesible y con scroll horizontal en pantallas chicas.
 * [titulo] se usa como `<caption>` oculto para lectores de pantalla.
 */
export function DataTable<T>({
  titulo,
  columnas,
  filas,
  claveFila,
  textoVacio = "No hay datos todavía.",
}: {
  titulo: string;
  columnas: Columna<T>[];
  filas: T[];
  claveFila: (fila: T) => string;
  textoVacio?: string;
}) {
  return (
    <div className="tabla-contenedor">
      <table className="tabla">
        <caption className="visualmente-oculto">{titulo}</caption>
        <thead>
          <tr>
            {columnas.map((columna) => (
              <th key={columna.clave} scope="col" className={columna.alinear === "derecha" ? "derecha" : undefined}>
                {columna.titulo}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {filas.length === 0 ? (
            <tr>
              <td colSpan={columnas.length} className="texto-secundario">
                {textoVacio}
              </td>
            </tr>
          ) : (
            filas.map((fila) => (
              <tr key={claveFila(fila)}>
                {columnas.map((columna) => (
                  <td key={columna.clave} className={columna.alinear === "derecha" ? "derecha" : undefined}>
                    {columna.render ? columna.render(fila) : String((fila as unknown as Record<string, unknown>)[columna.clave] ?? "")}
                  </td>
                ))}
              </tr>
            ))
          )}
        </tbody>
      </table>
    </div>
  );
}
