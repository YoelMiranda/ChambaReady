import { useState, type FormEvent, type ReactNode } from "react";
import { Link, Navigate, useLocation, useNavigate } from "react-router";
import { Button } from "../design/componentes/Basicos";
import { Campo } from "../design/componentes/Formularios";
import { mensajeDeErrorAuth, useSesion } from "./Sesion";

/** Marco de las pantallas de acceso (sin menú lateral). */
function MarcoAcceso({ titulo, children }: { titulo: string; children: ReactNode }) {
  return (
    <main className="acceso">
      <section className="tarjeta pila acceso__tarjeta">
        <div className="menu__logo acceso__logo">
          Chamba<span>Ready</span>
          <small>Empresas</small>
        </div>
        <h1>{titulo}</h1>
        {children}
      </section>
    </main>
  );
}

/** Lleva al panel (o a la página que pedía) si ya hay sesión. */
function useDestinoTrasLogin(): string {
  const ubicacion = useLocation();
  return (ubicacion.state as { desde?: string } | null)?.desde ?? "/panel";
}

export function LoginPage() {
  const { estado, iniciarSesion, recuperarClave } = useSesion();
  const destino = useDestinoTrasLogin();
  const [correo, setCorreo] = useState("");
  const [clave, setClave] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [aviso, setAviso] = useState<string | null>(null);
  const [enviando, setEnviando] = useState(false);

  if (estado.tipo === "empresa" || estado.tipo === "demo") return <Navigate to={destino} replace />;

  const ejecutar = async (accion: () => Promise<void>) => {
    setError(null);
    setAviso(null);
    setEnviando(true);
    try {
      await accion();
    } catch (e) {
      setError(mensajeDeErrorAuth(e));
    } finally {
      setEnviando(false);
    }
  };

  const enviar = (evento: FormEvent) => {
    evento.preventDefault();
    if (!correo.trim() || !clave) return setError("Escribe tu correo y tu contraseña.");
    void ejecutar(() => iniciarSesion(correo, clave));
  };

  return (
    <MarcoAcceso titulo="Inicia sesión">
      {estado.tipo === "anonimo" && estado.aviso && (
        <div className="aviso aviso--rojo" role="alert">
          {estado.aviso}
        </div>
      )}
      {estado.tipo === "otroRol" && (
        <div className="aviso aviso--ambar" role="status">
          La cuenta {estado.correo} es de practicante. Los estudiantes usan la app de ChambaReady; para la web de empresas usa otra cuenta.
        </div>
      )}
      <form className="pila" onSubmit={enviar} noValidate>
        <Campo etiqueta="Correo" type="email" autoComplete="email" value={correo} onChange={(e) => setCorreo(e.target.value)} />
        <Campo etiqueta="Contraseña" type="password" autoComplete="current-password" value={clave} onChange={(e) => setClave(e.target.value)} />
        {error && <span className="campo__error" role="alert">{error}</span>}
        {aviso && <span className="estado-guardado" role="status">{aviso}</span>}
        <Button type="submit" disabled={enviando || estado.tipo === "cargando"}>
          {enviando ? "Entrando…" : "Iniciar sesión"}
        </Button>
      </form>
      <div className="fila-acciones">
        <button
          type="button"
          className="boton-texto"
          onClick={() => {
            if (!correo.trim()) return setError("Escribe tu correo para recuperar tu contraseña.");
            void ejecutar(async () => {
              await recuperarClave(correo);
              setAviso("Te enviamos un correo para crear una contraseña nueva.");
            });
          }}
        >
          Olvidé mi contraseña
        </button>
        <span className="texto-secundario">
          ¿Tu empresa es nueva? <Link to="/registro">Crea una cuenta</Link>
        </span>
      </div>
    </MarcoAcceso>
  );
}

export function RegistroPage() {
  const { estado, registrarse } = useSesion();
  const navigate = useNavigate();
  const [nombre, setNombre] = useState("");
  const [correo, setCorreo] = useState("");
  const [clave, setClave] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [enviando, setEnviando] = useState(false);

  // Una cuenta nueva entra sin perfil de empresa: la llevamos a completarlo (E01).
  if (estado.tipo === "empresa" || estado.tipo === "demo") return <Navigate to="/perfil" replace />;

  const ejecutar = async (accion: () => Promise<void>) => {
    setError(null);
    setEnviando(true);
    try {
      await accion();
      navigate("/perfil", { replace: true });
    } catch (e) {
      setError(mensajeDeErrorAuth(e));
    } finally {
      setEnviando(false);
    }
  };

  const enviar = (evento: FormEvent) => {
    evento.preventDefault();
    if (nombre.trim().length < 2) return setError("Escribe tu nombre.");
    if (clave.length < 8) return setError("Usa una contraseña de 8 caracteres como mínimo.");
    void ejecutar(() => registrarse(nombre, correo, clave));
  };

  return (
    <MarcoAcceso titulo="Crea la cuenta de tu empresa">
      <p className="texto-secundario">Publicar vacantes es gratis. Después completarás el perfil de la empresa para que lo verifiquemos.</p>
      <form className="pila" onSubmit={enviar} noValidate>
        <Campo etiqueta="Tu nombre" autoComplete="name" value={nombre} onChange={(e) => setNombre(e.target.value)} />
        <Campo etiqueta="Correo de trabajo" type="email" autoComplete="email" value={correo} onChange={(e) => setCorreo(e.target.value)} />
        <Campo etiqueta="Contraseña" type="password" autoComplete="new-password" value={clave} onChange={(e) => setClave(e.target.value)} ayuda="8 caracteres como mínimo." />
        {error && <span className="campo__error" role="alert">{error}</span>}
        <Button type="submit" disabled={enviando}>
          {enviando ? "Creando cuenta…" : "Crear cuenta"}
        </Button>
      </form>
      <span className="texto-secundario">
        ¿Ya tienes cuenta? <Link to="/login">Inicia sesión</Link>
      </span>
    </MarcoAcceso>
  );
}

/**
 * Protege las páginas de la empresa: sin sesión lleva al login (recordando a dónde quería ir).
 * En modo demo deja pasar siempre.
 */
export function RequiereEmpresa({ children }: { children: ReactNode }) {
  const { estado } = useSesion();
  const ubicacion = useLocation();
  if (estado.tipo === "cargando") return <p className="texto-secundario" style={{ padding: 24 }}>Cargando…</p>;
  if (estado.tipo === "empresa" || estado.tipo === "demo") return <>{children}</>;
  return <Navigate to="/login" replace state={{ desde: ubicacion.pathname }} />;
}
