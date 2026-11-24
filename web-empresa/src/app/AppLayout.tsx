import { useState } from "react";
import { Outlet } from "react-router";
import { Icon } from "../design/componentes/Basicos";
import { Sidebar } from "../design/componentes/Sidebar";

/** Estructura de todas las páginas: menú lateral + contenido. En tablet y celular el menú se abre con un botón. */
export function AppLayout() {
  const [menuAbierto, setMenuAbierto] = useState(false);
  const cerrarMenu = () => setMenuAbierto(false);

  return (
    <div className="layout">
      <a href="#contenido" className="saltar">
        Saltar al contenido
      </a>
      <header className="barra-movil">
        <button
          type="button"
          className="boton-icono"
          aria-label="Abrir menú"
          aria-expanded={menuAbierto}
          aria-controls="menu-lateral"
          onClick={() => setMenuAbierto(true)}
        >
          <Icon nombre="menu" />
        </button>
        <strong>ChambaReady Empresas</strong>
      </header>
      <Sidebar id="menu-lateral" abierto={menuAbierto} onCerrar={cerrarMenu} />
      {menuAbierto && <div className="velo" aria-hidden="true" onClick={cerrarMenu} />}
      <main id="contenido" className="contenido" tabIndex={-1}>
        <Outlet />
      </main>
    </div>
  );
}
