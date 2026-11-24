import { useState } from "react";
import { useConfiguracion, useEstadoPlan, usePagarPlan, useSaldoEvaluaciones } from "../../api/consultas";
import { Aviso, Button, Icon } from "../../design/componentes/Basicos";
import { DataTable } from "../../design/componentes/DataTable";
import { Campo, Dialogo } from "../../design/componentes/Formularios";
import { formatoSoles, precioPorEvaluadoCentimos } from "../../design/reglas/empresa";

/** "2026-12-24" → "24/12/2026". */
function fechaCorta(iso: string): string {
  const [a, m, d] = iso.slice(0, 10).split("-");
  return `${d}/${m}/${a}`;
}

/**
 * E06 Plan y facturación. Prueba gratis de 20 evaluaciones (una sola vez) y Plan Empresa de precio
 * fijo con evaluaciones ilimitadas. El precio sale de la tabla de configuración.
 *
 * F8 SIMULADO (decisión del equipo): no hay pasarela de pago. "Pagar" registra un pago de prueba y
 * activa el plan por 30 días; pagar de nuevo suma 30 días más. Nunca se pide una tarjeta.
 */
export function PlanFacturacionPage() {
  const { data: saldo } = useSaldoEvaluaciones();
  const { data: configuracion } = useConfiguracion();
  const { data: plan } = useEstadoPlan();
  const pagar = usePagarPlan();
  const [postulantes, setPostulantes] = useState("100");
  const [confirmando, setConfirmando] = useState(false);

  if (!saldo || !configuracion || !plan) return <p className="texto-secundario">Cargando…</p>;

  const precio = configuracion.precioPlanCentimos;
  const cantidad = Number(postulantes);
  const cantidadValida = Number.isInteger(cantidad) && cantidad > 0 && cantidad <= 100_000;
  const usadas = saldo.gratisTotales - saldo.gratisRestantes;

  const confirmarPago = () =>
    pagar.mutate(undefined, {
      onSettled: () => setConfirmando(false),
    });

  return (
    <>
      <div className="encabezado-pagina">
        <div>
          <h1>Plan y facturación</h1>
          <p>Publicar vacantes siempre es gratis. Pagas para evaluar a todos tus postulantes.</p>
        </div>
      </div>

      <Aviso tipo="ambar">
        <strong>Pagos simulados:</strong> en esta versión no se cobra nada ni se pide tarjeta. El botón registra un pago de prueba y activa el plan por 30 días.
      </Aviso>
      {pagar.isError && <Aviso tipo="rojo">{pagar.error.message}</Aviso>}

      <section className="tarjeta pila" style={{ marginBottom: 24 }}>
        <h2>Tu uso</h2>
        {plan.activo ? (
          <p>
            <Icon nombre="verified" /> Plan Empresa: evaluaciones <strong>ilimitadas</strong> para todas tus vacantes
            {plan.venceEn && (
              <>
                {" "}
                hasta el <strong>{fechaCorta(plan.venceEn)}</strong>
              </>
            )}
            .
          </p>
        ) : (
          <>
            <p>
              Usaste <strong>{usadas} de {saldo.gratisTotales}</strong> evaluaciones gratis. {saldo.gratisRestantes === 0 ? "Ya no te quedan: los nuevos postulantes entran «Sin evaluar»." : `Te quedan ${saldo.gratisRestantes}.`}
            </p>
            <div
              className="medidor__barra"
              style={{ background: "var(--cr-azul-tinte)" }}
              role="progressbar"
              aria-label="Evaluaciones gratis usadas"
              aria-valuemin={0}
              aria-valuemax={saldo.gratisTotales}
              aria-valuenow={usadas}
            >
              <span style={{ width: `${(usadas / saldo.gratisTotales) * 100}%`, background: "var(--cr-azul)" }} />
            </div>
          </>
        )}
      </section>

      <section className="tarjeta pila" style={{ marginBottom: 24 }}>
        <h2>¿Cuántos postulantes recibes al mes?</h2>
        <div className="cuadricula-form">
          <Campo
            etiqueta="Postulantes al mes (todas tus vacantes)"
            type="number"
            min={1}
            inputMode="numeric"
            value={postulantes}
            onChange={(e) => setPostulantes(e.target.value)}
            error={postulantes !== "" && !cantidadValida ? "Escribe un número mayor que 0." : null}
          />
          <div aria-live="polite">
            {cantidadValida && (
              <>
                <div className="campo__titulo">Te recomendamos el Plan Empresa</div>
                <div className="precio-grande">{formatoSoles(precioPorEvaluadoCentimos(precio, cantidad))}</div>
                <div className="texto-secundario">por postulante evaluado ({formatoSoles(precio)} al mes ÷ {cantidad} postulantes)</div>
              </>
            )}
          </div>
        </div>
      </section>

      <div className="cuadricula-planes" style={{ marginBottom: 24 }}>
        <section className="tarjeta pila">
          <h2>Prueba gratis</h2>
          <div className="precio-grande">S/ 0</div>
          <ul className="lista-viñetas">
            <li>{saldo.gratisTotales} evaluaciones, una sola vez</li>
            <li>Para todas tus vacantes</li>
            <li>Después, los postulantes entran sin evaluar</li>
          </ul>
          {!plan.activo && <span className="chip chip--neutro">Tu plan actual</span>}
        </section>

        <section className="tarjeta pila tarjeta-plan--destacada">
          <h2>Plan Empresa</h2>
          <div>
            <span className="precio-grande">{formatoSoles(precio)}</span> <span className="texto-secundario">al mes, IGV incluido</span>
          </div>
          <ul className="lista-viñetas">
            <li>Evaluaciones ilimitadas en todas tus vacantes</li>
            <li>Envía las preguntas filtro a quienes entraron sin evaluar</li>
            <li>Sellos confirmados para comparar con datos reales</li>
          </ul>
          {plan.activo && (
            <span className="chip chip--verde">
              <Icon nombre="check_circle" /> Activo
            </span>
          )}
          <Button variante="cta" onClick={() => setConfirmando(true)} disabled={pagar.isPending}>
            {plan.activo ? "Sumar 30 días más" : "Evalúa a todos tus postulantes"}
          </Button>
        </section>
      </div>

      {plan.pagos.length > 0 && (
        <section className="tarjeta pila">
          <h2>Pagos simulados</h2>
          <DataTable
            titulo="Pagos simulados del Plan Empresa"
            filas={plan.pagos}
            claveFila={(p) => p.id}
            columnas={[
              { clave: "fecha", titulo: "Fecha", render: (p) => fechaCorta(p.fecha) },
              { clave: "concepto", titulo: "Concepto", render: () => "Plan Empresa (30 días)" },
              { clave: "monto", titulo: "Monto", render: (p) => formatoSoles(p.montoCentimos), alinear: "derecha" },
              { clave: "vence", titulo: "Vigente hasta", render: (p) => fechaCorta(p.venceEn) },
            ]}
          />
        </section>
      )}

      <Dialogo
        abierto={confirmando}
        titulo="Confirmar pago simulado"
        onCerrar={() => setConfirmando(false)}
        acciones={
          <>
            <Button variante="secundario" onClick={() => setConfirmando(false)}>
              Cancelar
            </Button>
            <Button onClick={confirmarPago} disabled={pagar.isPending}>
              {pagar.isPending ? "Registrando…" : `Simular pago de ${formatoSoles(precio)}`}
            </Button>
          </>
        }
      >
        <p>
          Se registrará un pago de prueba de <strong>{formatoSoles(precio)}</strong> y el Plan Empresa quedará activo por 30 días
          {plan.activo && plan.venceEn ? `, sumados a tu vencimiento actual (${fechaCorta(plan.venceEn)})` : ""}.
        </p>
        <p className="texto-secundario">No se cobra nada: es una simulación para probar ChambaReady.</p>
      </Dialogo>
    </>
  );
}
