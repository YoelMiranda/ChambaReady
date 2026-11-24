import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "./fuente";
import type { Empresa, Entrevista, EstadoPostulacion, Requisito, Vacante } from "./tipos";

/**
 * Consultas y mutaciones con TanStack Query. Las pantallas solo usan estos hooks: en F4 cambia la
 * API de abajo (fetch a /v1 con el token de Firebase desde F5) y las pantallas no se tocan.
 * Después de cada cambio se invalidan las consultas afectadas para que todo se vea actualizado.
 */
export const claves = {
  empresa: ["empresa"] as const,
  saldo: ["saldo"] as const,
  configuracion: ["configuracion"] as const,
  vacantes: ["vacantes"] as const,
  postulantes: (vacanteId: string) => ["postulantes", vacanteId] as const,
  postulante: (id: string) => ["postulante", id] as const,
  entrevistas: ["entrevistas"] as const,
  actividad: ["actividad"] as const,
  plan: ["plan"] as const,
  notificaciones: ["notificaciones"] as const,
  archivo: (id: string) => ["archivo", id] as const,
};

export const useEmpresa = () => useQuery({ queryKey: claves.empresa, queryFn: api.empresa });
export const useSaldoEvaluaciones = () => useQuery({ queryKey: claves.saldo, queryFn: api.saldo });
export const useConfiguracion = () => useQuery({ queryKey: claves.configuracion, queryFn: api.configuracion });
export const useVacantesEmpresa = () => useQuery({ queryKey: claves.vacantes, queryFn: api.vacantes });
export const useEntrevistas = () => useQuery({ queryKey: claves.entrevistas, queryFn: api.entrevistas });
export const useActividad = () => useQuery({ queryKey: claves.actividad, queryFn: api.actividad });
export const useEstadoPlan = () => useQuery({ queryKey: claves.plan, queryFn: api.estadoPlan });

/** F9: la campanita se actualiza sola cada minuto (sin push, se consulta de vez en cuando). */
export const useNotificaciones = () =>
  useQuery({ queryKey: claves.notificaciones, queryFn: api.notificaciones, refetchInterval: 60_000 });

/**
 * F7: URL local (blob:) de un archivo protegido, para <img> o para abrirlo. La URL se libera
 * cuando la consulta se descarta (gcTime), así no se acumulan en memoria.
 */
export function useUrlArchivo(archivoId: string | null | undefined) {
  return useQuery({
    queryKey: claves.archivo(archivoId ?? ""),
    queryFn: () => api.urlArchivo(archivoId ?? ""),
    enabled: Boolean(archivoId),
    staleTime: Infinity,
  });
}
export function usePostulante(postulanteId: string | undefined) {
  return useQuery({
    queryKey: claves.postulante(postulanteId ?? ""),
    queryFn: () => api.postulante(postulanteId ?? ""),
    enabled: Boolean(postulanteId),
  });
}

/** True si se usa el backend real (algunas acciones de demostración se ocultan). */
export const usaBackendReal = api.esReal;

export function usePostulantes(vacanteId: string | undefined) {
  return useQuery({
    queryKey: claves.postulantes(vacanteId ?? ""),
    queryFn: () => api.postulantes(vacanteId ?? ""),
    enabled: Boolean(vacanteId),
  });
}

/** Invalida todo: la API simulada es chica y así ninguna pantalla queda con datos viejos. */
function useRefrescar() {
  const cliente = useQueryClient();
  return () => cliente.invalidateQueries();
}

export function useGuardarEmpresa() {
  const refrescar = useRefrescar();
  return useMutation({ mutationFn: (datos: Empresa) => api.guardarEmpresa(datos), onSuccess: refrescar });
}

export function useSimularVerificacion() {
  const refrescar = useRefrescar();
  return useMutation({ mutationFn: () => api.simularVerificacion(), onSuccess: refrescar });
}

export function useCrearVacante() {
  const refrescar = useRefrescar();
  return useMutation({ mutationFn: (v: Omit<Vacante, "id" | "creada">) => api.crearVacante(v), onSuccess: refrescar });
}

export function useCambiarEstado() {
  const refrescar = useRefrescar();
  return useMutation({
    mutationFn: (p: { postulanteId: string; estado: EstadoPostulacion; motivo?: string }) =>
      api.cambiarEstado(p.postulanteId, p.estado, p.motivo ?? null),
    onSuccess: refrescar,
  });
}

export function useEnviarPreguntas() {
  const refrescar = useRefrescar();
  return useMutation({ mutationFn: (postulanteId: string) => api.enviarPreguntas(postulanteId), onSuccess: refrescar });
}

export function useAgendarEntrevista() {
  const refrescar = useRefrescar();
  return useMutation({ mutationFn: (e: Omit<Entrevista, "id" | "nombrePostulante" | "puesto">) => api.agendarEntrevista(e), onSuccess: refrescar });
}

export function usePagarPlan() {
  const refrescar = useRefrescar();
  return useMutation({ mutationFn: () => api.pagarPlan(), onSuccess: refrescar });
}

export function useSubirLogo() {
  const refrescar = useRefrescar();
  return useMutation({ mutationFn: (archivo: File) => api.subirLogo(archivo), onSuccess: refrescar });
}

export function useSugerirPreguntas() {
  return useMutation({ mutationFn: (p: { puesto: string; requisitos: Requisito[] }) => api.sugerirPreguntas(p.puesto, p.requisitos) });
}

export function useMarcarLeida() {
  const cliente = useQueryClient();
  return useMutation({ mutationFn: (id: string) => api.marcarLeida(id), onSuccess: () => cliente.invalidateQueries({ queryKey: claves.notificaciones }) });
}

export function useMarcarTodas() {
  const cliente = useQueryClient();
  return useMutation({ mutationFn: () => api.marcarTodas(), onSuccess: () => cliente.invalidateQueries({ queryKey: claves.notificaciones }) });
}

export function useRestablecer() {
  const refrescar = useRefrescar();
  return useMutation({ mutationFn: () => api.restablecer(), onSuccess: refrescar });
}
