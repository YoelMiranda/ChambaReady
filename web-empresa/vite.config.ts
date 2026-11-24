import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

// Configuración de Vite. El puerto 5173 coincide con WEB_EMPRESA_URL de la app Android (C01).
export default defineConfig({
  plugins: [react()],
  server: { port: 5173 },
  build: {
    rolldownOptions: {
      output: {
        // Firebase y React en archivos aparte: el código de la app cambia seguido, estos no,
        // así el navegador los reutiliza de su caché entre despliegues.
        codeSplitting: {
          groups: [
            { name: "firebase", test: /node_modules[\\/]@firebase/ },
            { name: "react", test: /node_modules[\\/](react|react-dom|react-router|scheduler|@tanstack)[\\/]/ },
          ],
        },
      },
    },
  },
});
