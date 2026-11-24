import { ButtonLink } from "../../design/componentes/Basicos";

/** Página 404 para rutas que no existen. */
export function NoEncontrada() {
  return (
    <section className="tarjeta pila">
      <h1>No encontramos esta página</h1>
      <p className="texto-secundario">Revisa la dirección o vuelve al panel.</p>
      <div>
        <ButtonLink to="/panel" variante="secundario" icono="dashboard">
          Ir al panel
        </ButtonLink>
      </div>
    </section>
  );
}
