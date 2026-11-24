import { createContext, useContext, useEffect, useState, type ReactNode } from "react";
import {
  createUserWithEmailAndPassword,
  onAuthStateChanged,
  sendPasswordResetEmail,
  signInWithEmailAndPassword,
  signOut,
  updateProfile,
} from "@firebase/auth";
import { pedir } from "../api/http";
import { auth, usaApiReal } from "./firebase";

/**
 * Estado de la sesión de la web de empresa:
 * - demo: sin backend configurado, se usa la API simulada y no hay login.
 * - anonimo: hay backend, pero nadie inició sesión.
 * - empresa: sesión de una cuenta de empresa (puede usar la web).
 * - otroRol: la cuenta es de practicante; debe usar la app.
 */
export type EstadoSesion =
  | { tipo: "demo" }
  | { tipo: "cargando" }
  | { tipo: "anonimo" }
  | { tipo: "empresa"; correo: string }
  | { tipo: "otroRol"; correo: string };

interface ContextoSesion {
  estado: EstadoSesion;
  iniciarSesion(correo: string, clave: string): Promise<void>;
  registrarse(nombre: string, correo: string, clave: string): Promise<void>;
  recuperarClave(correo: string): Promise<void>;
  cerrarSesion(): Promise<void>;
}

const Contexto = createContext<ContextoSesion | null>(null);

interface SesionApi {
  registrado: boolean;
  rol: string | null;
}

/** Mensajes en español para los errores más comunes de Firebase Auth. */
export function mensajeDeErrorAuth(error: unknown): string {
  const codigo = (error as { code?: string })?.code ?? "";
  const mensajes: Record<string, string> = {
    "auth/invalid-credential": "Correo o contraseña incorrectos.",
    "auth/wrong-password": "Correo o contraseña incorrectos.",
    "auth/user-not-found": "Correo o contraseña incorrectos.",
    "auth/email-already-in-use": "Ya existe una cuenta con ese correo. Inicia sesión.",
    "auth/weak-password": "La contraseña debe tener 6 caracteres como mínimo.",
    "auth/invalid-email": "Escribe un correo válido.",
    "auth/too-many-requests": "Demasiados intentos. Espera unos minutos.",
    "auth/network-request-failed": "No pudimos conectarnos. Revisa tu internet.",
  };
  if (mensajes[codigo]) return mensajes[codigo];
  return error instanceof Error && !codigo ? error.message : "No pudimos completar la acción. Inténtalo de nuevo.";
}

export function SesionProvider({ children }: { children: ReactNode }) {
  const [estado, setEstado] = useState<EstadoSesion>(usaApiReal ? { tipo: "cargando" } : { tipo: "demo" });

  useEffect(() => {
    if (!usaApiReal || !auth) return;
    return onAuthStateChanged(auth, async (usuario) => {
      if (!usuario) {
        setEstado({ tipo: "anonimo" });
        return;
      }
      const correo = usuario.email ?? "";
      try {
        const sesion = await pedir<SesionApi>("GET", "sesion");
        if (!sesion.registrado) {
          // Toda cuenta nueva que entra a la web de empresas se registra como EMPRESA.
          await pedir("POST", "registro", { rol: "EMPRESA", nombre: usuario.displayName ?? "" });
          setEstado({ tipo: "empresa", correo });
        } else {
          setEstado(sesion.rol === "EMPRESA" ? { tipo: "empresa", correo } : { tipo: "otroRol", correo });
        }
      } catch {
        // Sin backend no se puede confirmar el rol: se vuelve al login.
        setEstado({ tipo: "anonimo" });
      }
    });
  }, []);

  const exigirAuth = () => {
    if (!auth) throw new Error("Firebase no está configurado.");
    return auth;
  };

  const valor: ContextoSesion = {
    estado,
    async iniciarSesion(correo, clave) {
      await signInWithEmailAndPassword(exigirAuth(), correo.trim(), clave);
    },
    async registrarse(nombre, correo, clave) {
      const credencial = await createUserWithEmailAndPassword(exigirAuth(), correo.trim(), clave);
      if (nombre.trim()) await updateProfile(credencial.user, { displayName: nombre.trim() });
    },
    async recuperarClave(correo) {
      await sendPasswordResetEmail(exigirAuth(), correo.trim());
    },
    async cerrarSesion() {
      if (auth) await signOut(auth);
    },
  };

  return <Contexto.Provider value={valor}>{children}</Contexto.Provider>;
}

export function useSesion(): ContextoSesion {
  const valor = useContext(Contexto);
  if (!valor) throw new Error("useSesion debe usarse dentro de <SesionProvider>");
  return valor;
}
