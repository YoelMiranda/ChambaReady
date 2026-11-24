import { createBrowserRouter, Navigate } from "react-router";
import { AppLayout } from "./AppLayout";
import { PanelPage } from "../features/panel/PanelPage";
import { NuevaVacantePage } from "../features/vacantes/NuevaVacantePage";
import { CandidatosPage } from "../features/candidatos/CandidatosPage";
import { AgendarEntrevistaPage, EntrevistasPage } from "../features/entrevistas/EntrevistasPage";
import { PlanFacturacionPage } from "../features/plan/PlanFacturacionPage";
import { PerfilEmpresaPage } from "../features/perfil/PerfilEmpresaPage";
import { NotificacionesPage } from "../features/notificaciones/NotificacionesPage";
import { NoEncontrada } from "../features/comun/NoEncontrada";
import { LoginPage, RegistroPage, RequiereEmpresa } from "../auth/PaginasAuth";

/**
 * Rutas reales: los botones atrás/adelante del navegador funcionan y cada pantalla tiene su URL.
 * El detalle del candidato vive en la URL: /candidatos/:vacanteId/:postulanteId.
 */
export const router = createBrowserRouter([
  { path: "/login", element: <LoginPage /> },
  { path: "/registro", element: <RegistroPage /> },
  {
    path: "/",
    element: (
      <RequiereEmpresa>
        <AppLayout />
      </RequiereEmpresa>
    ),
    children: [
      { index: true, element: <Navigate to="/panel" replace /> },
      { path: "panel", element: <PanelPage /> },
      { path: "vacantes/nueva", element: <NuevaVacantePage /> },
      { path: "candidatos", element: <CandidatosPage /> },
      { path: "candidatos/:vacanteId", element: <CandidatosPage /> },
      { path: "candidatos/:vacanteId/:postulanteId", element: <CandidatosPage /> },
      { path: "entrevistas", element: <EntrevistasPage /> },
      { path: "entrevistas/agendar/:postulanteId", element: <AgendarEntrevistaPage /> },
      { path: "plan", element: <PlanFacturacionPage /> },
      { path: "perfil", element: <PerfilEmpresaPage /> },
      { path: "notificaciones", element: <NotificacionesPage /> },
      { path: "*", element: <NoEncontrada /> },
    ],
  },
]);
