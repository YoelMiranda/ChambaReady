import { NavLink } from "react-router";
import { Icon } from "./Basicos";
import { useQueryClient } from "@tanstack/react-query";
import { useNotificaciones, useSaldoEvaluaciones } from "../../api/consultas";
import { useSesion } from "../../auth/Sesion";

const SECCIONES = [
  { ruta: "/panel", texto: "Panel", icono: "dashboard" },
  { ruta: "/vacantes/nueva", texto: "Nueva vacante", icono: "add_circle" },
  { ruta: "/candidatos", texto: "Candidatos", icono: "groups" },
  { ruta: "/entrevistas", texto: "Entrevistas", icono: "event" },
  { ruta: "/plan", texto: "Plan y facturación", icono: "credit_card" },
  { ruta: "/perfil", texto: "Perfil de empresa", icono: "business" },
  { ruta: "/notificaciones", texto: "Notificaciones", icono: "notifications" },
];

/**
 * Menú lateral de la web de empresa. Marca la sección activa (NavLink) y en el pie muestra el
 * medidor "Evaluaciones gratis X de 20". En pantallas chicas se abre como panel.
 */
export function Sidebar({ id, abierto, onCerrar }: { id: string; abierto: boolean; onCerrar: () => void }) {
  const { data: saldo } = useSaldoEvaluaciones();
  const { data: notificaciones } = useNotificaciones();
  const noLeidas = notificaciones?.filter((n) => !n.leida).length ?? 0;
  const { estado, cerrarSesion } = useSesion();
  const clienteConsultas = useQueryClient();

  return (
    <aside id={id} className={`menu ${abierto ? "menu--abierto" : ""}`} aria-label="Menú principal">
      <div className="menu__logo">
        Chamba<span>Ready</span>
        <small>Empresas</small>
      </div>
      <button type="button" className="boton-icono menu__cerrar" aria-label="Cerrar menú" onClick={onCerrar}>
        <Icon nombre="close" />
      </button>
      <nav>
        <ul>
          {SECCIONES.map((seccion) => (
            <li key={seccion.ruta}>
              <NavLink to={seccion.ruta} className={({ isActive }) => (isActive ? "activo" : undefined)} onClick={onCerrar}>
                <Icon nombre={seccion.icono} />
                {seccion.texto}
                {seccion.ruta === "/notificaciones" && noLeidas > 0 && (
                  <span className="insignia" aria-label={`${noLeidas} sin leer`}>
                    {noLeidas > 9 ? "9+" : noLeidas}
                  </span>
                )}
              </NavLink>
            </li>
          ))}
        </ul>
      </nav>
      {saldo && (
        <div className="menu__medidor">
          {saldo.planActivo ? (
            <div>
              <Icon nombre="verified" /> Plan Empresa activo
              <div style={{ opacity: 0.8 }}>Evaluaciones ilimitadas</div>
            </div>
          ) : (
            <>
              <div id="medidor-evaluaciones">
                Evaluaciones gratis {saldo.gratisRestantes} de {saldo.gratisTotales}
              </div>
              <div
                className="medidor__barra"
                role="progressbar"
                aria-labelledby="medidor-evaluaciones"
                aria-valuemin={0}
                aria-valuemax={saldo.gratisTotales}
                aria-valuenow={saldo.gratisRestantes}
              >
                <span style={{ width: `${(saldo.gratisRestantes / saldo.gratisTotales) * 100}%` }} />
              </div>
            </>
          )}
          <NavLink to="/plan" onClick={onCerrar}>
            {saldo.planActivo ? "Ver mi plan" : "Ver planes"}
          </NavLink>
        </div>
      )}
      {estado.tipo === "empresa" && (
        <div className="menu__sesion">
          <span title={estado.correo}>{estado.correo}</span>
          <button
            type="button"
            className="boton-texto"
            onClick={async () => {
              await cerrarSesion();
              clienteConsultas.clear(); // que otra cuenta no vea datos de esta
            }}
          >
            Cerrar sesión
          </button>
        </div>
      )}
      {estado.tipo === "demo" && <div className="menu__sesion">Modo demo: datos de prueba</div>}
    </aside>
  );
}
