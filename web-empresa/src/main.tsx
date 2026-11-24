import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { RouterProvider } from "react-router/dom";
import { router } from "./app/router";
import { SesionProvider } from "./auth/Sesion";
import "./design/tokens.css";
import "./design/global.css";
import "./design/componentes.css";

const clienteConsultas = new QueryClient({
  defaultOptions: { queries: { staleTime: 30_000, retry: 1 } },
});

const raiz = document.getElementById("root");
if (!raiz) throw new Error("No se encontró el elemento #root en index.html");

createRoot(raiz).render(
  <StrictMode>
    <QueryClientProvider client={clienteConsultas}>
      <SesionProvider>
        <RouterProvider router={router} />
      </SesionProvider>
    </QueryClientProvider>
  </StrictMode>,
);
