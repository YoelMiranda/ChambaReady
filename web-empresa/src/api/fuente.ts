import { usaApiReal } from "../auth/firebase";
import type { ApiEmpresa } from "./contrato";
import { apiReal } from "./real";
import { apiSimulada } from "./simulada";

/**
 * API que usan las pantallas: la real si están configurados VITE_API_BASE_URL y Firebase; si no, la
 * simulada (datos de prueba en el navegador), para poder trabajar en la web sin backend.
 */
export const api: ApiEmpresa = usaApiReal ? apiReal : apiSimulada;
