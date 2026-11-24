import { useEffect, useState, type FormEvent } from "react";
import { usaBackendReal, useEmpresa, useGuardarEmpresa, useRestablecer, useSimularVerificacion, useSubirLogo, useUrlArchivo } from "../../api/consultas";
import type { Empresa, EstadoVerificacion } from "../../api/tipos";
import { Aviso, Button, Icon } from "../../design/componentes/Basicos";
import { AreaTexto, Campo, Selector } from "../../design/componentes/Formularios";
import { validarRuc } from "../../design/reglas/empresa";

const SECTORES = ["Banca y finanzas", "Retail", "Tecnología", "Consumo masivo", "Logística", "Minería y energía", "Salud", "Educación", "Otro"];
const TAMANOS = ["1 a 10 trabajadores", "11 a 50 trabajadores", "51 a 200 trabajadores", "201 a 500 trabajadores", "Más de 500 trabajadores"];
const LOGO_BYTES_MAXIMOS = 1024 * 1024;

const VERIFICACION: Record<EstadoVerificacion, { texto: string; clase: string; icono: string }> = {
  VERIFICADA: { texto: "Verificada", clase: "chip--verde", icono: "verified" },
  EN_REVISION: { texto: "En revisión", clase: "chip--ambar", icono: "hourglass_empty" },
  SIN_VERIFICAR: { texto: "Sin verificar", clase: "chip--rojo", icono: "error" },
};

type Errores = Partial<Record<keyof Empresa, string>>;

function validar(e: Empresa): Errores {
  const errores: Errores = {};
  if (e.razonSocial.trim().length < 3) errores.razonSocial = "Escribe la razón social.";
  const ruc = validarRuc(e.ruc);
  if (ruc) errores.ruc = ruc;
  if (e.web && !/^https?:\/\/\S+\.\S+/.test(e.web.trim())) errores.web = "Escribe la web completa, con https://";
  if (!/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(e.contactoCorreo.trim())) errores.contactoCorreo = "Escribe un correo válido.";
  if (e.descripcion.trim().length < 20) errores.descripcion = "Cuéntales a los estudiantes qué hace tu empresa (20 caracteres como mínimo).";
  return errores;
}

/** E01 Perfil de empresa. Sin verificación no se pueden publicar vacantes. */
export function PerfilEmpresaPage() {
  const { data: empresa } = useEmpresa();
  const guardar = useGuardarEmpresa();
  const verificar = useSimularVerificacion();
  const restablecer = useRestablecer();
  const subirLogo = useSubirLogo();
  // Con la API real el logo es un archivo protegido: se descarga con el token y se muestra como blob:.
  const { data: urlLogoReal } = useUrlArchivo(usaBackendReal ? empresa?.logoArchivoId : null);
  const [form, setForm] = useState<Empresa | null>(null);
  const [errores, setErrores] = useState<Errores>({});
  const [mensaje, setMensaje] = useState<string | null>(null);
  const [esError, setEsError] = useState(false);
  const avisar = (texto: string | null, error = false) => {
    setMensaje(texto);
    setEsError(error);
  };

  useEffect(() => {
    if (empresa) setForm(empresa);
  }, [empresa]);

  if (!form || !empresa) return <p className="texto-secundario">Cargando perfil…</p>;

  const cambiar = <K extends keyof Empresa>(campo: K, valor: Empresa[K]) => {
    setForm({ ...form, [campo]: valor });
    setErrores({ ...errores, [campo]: undefined });
    avisar(null);
  };

  const elegirLogo = (archivo: File | undefined) => {
    if (!archivo) return;
    if (!archivo.type.startsWith("image/")) return avisar("El logo debe ser una imagen (PNG o JPG).", true);
    if (archivo.size > LOGO_BYTES_MAXIMOS) return avisar("El logo pesa más de 1 MB.", true);
    if (usaBackendReal) {
      // F7: se sube al backend apenas se elige (no espera a "Guardar cambios").
      subirLogo.mutate(archivo, {
        onSuccess: () => avisar("Logo actualizado."),
        onError: (e) => avisar(e.message, true),
      });
      return;
    }
    const lector = new FileReader();
    lector.onload = () => cambiar("logo", String(lector.result));
    lector.readAsDataURL(archivo);
  };

  const enviar = (evento: FormEvent) => {
    evento.preventDefault();
    const encontrados = validar(form);
    setErrores(encontrados);
    if (Object.keys(encontrados).length > 0) {
      avisar("Revisa los campos marcados.", true);
      return;
    }
    guardar.mutate(form, {
      onSuccess: (guardada) =>
        avisar(guardada.verificacion === "EN_REVISION" ? "Guardado. Verificaremos tu empresa (1 a 2 días hábiles)." : "Cambios guardados."),
      onError: (e) => avisar(e.message, true),
    });
  };

  const verificacion = VERIFICACION[empresa.verificacion];

  return (
    <>
      <div className="encabezado-pagina">
        <div>
          <h1>Perfil de empresa</h1>
          <p>Esto es lo que ven los estudiantes en tus vacantes.</p>
        </div>
        <span className={`chip ${verificacion.clase}`}>
          <Icon nombre={verificacion.icono} />
          {verificacion.texto}
        </span>
      </div>

      {empresa.verificacion !== "VERIFICADA" && (
        <Aviso tipo="ambar">
          Tu empresa aún no está verificada: no puedes publicar vacantes. Revisamos el RUC y los datos de contacto en 1 a 2 días hábiles.{" "}
          {!usaBackendReal && (
            <button type="button" className="boton-texto" onClick={() => verificar.mutate()}>
              Simular verificación (demo)
            </button>
          )}
        </Aviso>
      )}

      <form className="pila" onSubmit={enviar} noValidate>
        <section className="tarjeta pila">
          <h2>Datos de la empresa</h2>
          <div className="fila-acciones">
            {(usaBackendReal ? urlLogoReal : form.logo) ? (
              <img src={(usaBackendReal ? urlLogoReal : form.logo) ?? undefined} alt={`Logo de ${form.razonSocial}`} className="logo-empresa" />
            ) : (
              <div className="logo-empresa" aria-hidden="true">
                {form.razonSocial.slice(0, 1)}
              </div>
            )}
            <div className="campo">
              <label htmlFor="logo" className="campo__titulo">
                Logo (PNG o JPG, máx. 1 MB)
              </label>
              <input id="logo" type="file" accept="image/png,image/jpeg" disabled={subirLogo.isPending} onChange={(e) => elegirLogo(e.target.files?.[0])} />
              {subirLogo.isPending && <span className="campo__ayuda">Subiendo logo…</span>}
            </div>
          </div>
          <div className="cuadricula-form">
            <Campo etiqueta="Razón social" value={form.razonSocial} onChange={(e) => cambiar("razonSocial", e.target.value)} error={errores.razonSocial} />
            <Campo
              etiqueta="RUC"
              inputMode="numeric"
              maxLength={11}
              value={form.ruc}
              onChange={(e) => cambiar("ruc", e.target.value.replace(/\D/g, ""))}
              error={errores.ruc}
              ayuda="11 dígitos. Si lo cambias, verificaremos tu empresa otra vez."
            />
            <Selector etiqueta="Sector" value={form.sector} onChange={(e) => cambiar("sector", e.target.value)} opciones={SECTORES.map((s) => ({ valor: s, texto: s }))} />
            <Selector etiqueta="Tamaño" value={form.tamano} onChange={(e) => cambiar("tamano", e.target.value)} opciones={TAMANOS.map((t) => ({ valor: t, texto: t }))} />
            <Campo etiqueta="Web" type="url" value={form.web} onChange={(e) => cambiar("web", e.target.value)} error={errores.web} placeholder="https://" />
          </div>
          <AreaTexto etiqueta="Descripción" value={form.descripcion} onChange={(e) => cambiar("descripcion", e.target.value)} error={errores.descripcion} />
        </section>

        <section className="tarjeta pila">
          <h2>Contacto</h2>
          <div className="cuadricula-form">
            <Campo etiqueta="Nombre" value={form.contactoNombre} onChange={(e) => cambiar("contactoNombre", e.target.value)} />
            <Campo etiqueta="Correo" type="email" value={form.contactoCorreo} onChange={(e) => cambiar("contactoCorreo", e.target.value)} error={errores.contactoCorreo} />
            <Campo etiqueta="Teléfono" type="tel" value={form.contactoTelefono} onChange={(e) => cambiar("contactoTelefono", e.target.value)} />
          </div>
        </section>

        <div className="fila-acciones">
          <Button type="submit" icono="save" disabled={guardar.isPending}>
            {guardar.isPending ? "Guardando…" : "Guardar cambios"}
          </Button>
          {!usaBackendReal && (
            <Button variante="secundario" onClick={() => restablecer.mutate()}>
              Restablecer datos de prueba
            </Button>
          )}
          <span role="status" className={esError ? "campo__error" : "estado-guardado"}>
            {mensaje}
          </span>
        </div>
      </form>
    </>
  );
}
