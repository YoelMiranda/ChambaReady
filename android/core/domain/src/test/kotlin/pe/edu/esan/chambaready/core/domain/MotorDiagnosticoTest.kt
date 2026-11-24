package pe.edu.esan.chambaready.core.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.edu.esan.chambaready.core.domain.modelo.AreaInteres
import pe.edu.esan.chambaready.core.domain.modelo.CumplimientoRequisito
import pe.edu.esan.chambaready.core.domain.modelo.CursoExterno
import pe.edu.esan.chambaready.core.domain.modelo.HorasLibres
import pe.edu.esan.chambaready.core.domain.modelo.Modalidad
import pe.edu.esan.chambaready.core.domain.modelo.NivelHabilidad
import pe.edu.esan.chambaready.core.domain.modelo.NivelesUsuario
import pe.edu.esan.chambaready.core.domain.modelo.RequisitoVacante
import pe.edu.esan.chambaready.core.domain.modelo.TipoRequisito
import pe.edu.esan.chambaready.core.domain.modelo.VacanteDetalle
import pe.edu.esan.chambaready.core.domain.modelo.VacanteResumen
import pe.edu.esan.chambaready.core.domain.reglas.MotorDiagnostico
import java.time.DayOfWeek
import java.time.LocalDate

/** Diagnóstico (P10) y cálculo del plan por semanas (P12). */
class MotorDiagnosticoTest {

    // Miércoles 18/11/2026; la vacante cierra el miércoles 9/12 (3 semanas exactas).
    private val hoy = LocalDate.of(2026, 11, 18)
    private val cierre = LocalDate.of(2026, 12, 9)

    private val sql = RequisitoVacante("r1", "sql", "SQL", NivelHabilidad.INTERMEDIO, TipoRequisito.FILTRO)
    private val excel = RequisitoVacante("r2", "excel", "Excel", NivelHabilidad.AVANZADO, TipoRequisito.OBLIGATORIO)
    private val bi = RequisitoVacante("r3", "powerbi", "Power BI", NivelHabilidad.BASICO, TipoRequisito.DESEABLE)

    private val vacante = VacanteDetalle(
        resumen = VacanteResumen("v1", "Analista", "Empresa", Modalidad.REMOTA, null, cierre, 0),
        area = AreaInteres.DATA_ANALYTICS,
        descripcion = "",
        queHaras = emptyList(),
        requisitos = listOf(bi, excel, sql),
        evaluacionActiva = true,
        empresaConEvaluaciones = true,
    )

    private val cursos = mapOf(
        "sql" to CursoExterno("sql", "SQL", "Proveedor", "https://ejemplo.com/sql", horasPorNivel = 6.0),
        "excel" to CursoExterno("excel", "Excel", "Proveedor", "https://ejemplo.com/excel", horasPorNivel = 5.0),
    )

    // SQL básico (le falta 1 nivel), Excel avanzado (cumple), Power BI nada (le falta 1 nivel, sin curso).
    private val niveles = NivelesUsuario(
        declarados = mapOf("sql" to NivelHabilidad.BASICO, "excel" to NivelHabilidad.AVANZADO),
    )

    @Test
    fun cumplimiento_cumpleParcialYFalta() {
        assertEquals(CumplimientoRequisito.PARCIAL, MotorDiagnostico.cumplimiento(sql, niveles))
        assertEquals(CumplimientoRequisito.CUMPLE, MotorDiagnostico.cumplimiento(excel, niveles))
        assertEquals(CumplimientoRequisito.FALTA, MotorDiagnostico.cumplimiento(bi, niveles))
    }

    @Test
    fun cumplimiento_usaElMayorEntreDeclaradoYDemostrado() {
        val conMiniTest = niveles.copy(demostrados = mapOf("sql" to NivelHabilidad.INTERMEDIO))
        assertEquals(CumplimientoRequisito.CUMPLE, MotorDiagnostico.cumplimiento(sql, conMiniTest))
    }

    @Test
    fun brechas_ordenadasPorImportanciaYConHoras() {
        val brechas = MotorDiagnostico.brechas(vacante, niveles, cursos)
        assertEquals(listOf("sql", "powerbi"), brechas.map { it.requisito.habilidadId })
        assertEquals(6.0, brechas[0].horas, 0.0) // 1 nivel × 6 h
        assertEquals(MotorDiagnostico.HORAS_POR_NIVEL_SIN_CURSO, brechas[1].horas, 0.0)
    }

    @Test
    fun diagnostico_probabilidadesYViabilidad() {
        // Pesos 1 + 2 + 3 = 6; obtiene 0 (bi) + 2 (excel) + 1.5 (sql parcial) = 3.5 → 58 %.
        val horas = HorasLibres(mapOf(DayOfWeek.SATURDAY to 4.0, DayOfWeek.SUNDAY to 2.0)) // 6 h por semana
        val d = MotorDiagnostico.diagnosticar(vacante, niveles, cursos, horas, hoy)
        assertEquals(58, d.probabilidadActual)
        assertEquals(100, d.probabilidadMeta)
        assertEquals(14.0, d.horasNecesarias, 0.0)
        assertEquals(18.0, d.viabilidad.horasDisponibles, 0.0) // 3 fines de semana
        assertTrue(d.viabilidad.esViable)
    }

    @Test
    fun plan_respetaLasHorasDeCadaSemanaYSesionesDeDosHoras() {
        val horas = HorasLibres(mapOf(DayOfWeek.SATURDAY to 4.0, DayOfWeek.SUNDAY to 2.0))
        val d = MotorDiagnostico.diagnosticar(vacante, niveles, cursos, horas, hoy)
        val plan = MotorDiagnostico.generarPlan(d, horas, hoy, cierre)

        assertEquals(14.0, plan.tareas.sumOf { it.horas }, 0.0)
        assertTrue(plan.tareas.all { it.horas <= MotorDiagnostico.HORAS_MAXIMAS_POR_SESION })
        assertTrue(plan.semanas.all { it.horas <= 6.0 })
        assertEquals(listOf(1, 2, 3), plan.semanas.map { it.numero })
        // Primero se trabaja el filtro (SQL).
        assertEquals("sql", plan.tareas.first().moduloId)
        assertEquals(plan.tareas.size, plan.tareas.map { it.id }.toSet().size)
    }

    @Test
    fun plan_sinHorasLibresEstaVacio() {
        val d = MotorDiagnostico.diagnosticar(vacante, niveles, cursos, HorasLibres(), hoy)
        assertFalse(d.viabilidad.esViable)
        assertTrue(MotorDiagnostico.generarPlan(d, HorasLibres(), hoy, cierre).semanas.isEmpty())
    }

    @Test
    fun plan_siNoAlcanzaSigueDespuesDelCierre() {
        val pocas = HorasLibres(mapOf(DayOfWeek.SUNDAY to 1.0)) // 1 h por semana, 14 h necesarias
        val d = MotorDiagnostico.diagnosticar(vacante, niveles, cursos, pocas, hoy)
        val plan = MotorDiagnostico.generarPlan(d, pocas, hoy, cierre)
        assertEquals(14.0, plan.tareas.sumOf { it.horas }, 0.0)
        assertEquals(14, plan.semanas.size)
    }

    @Test
    fun plan_avanceYSiguienteTarea() {
        val horas = HorasLibres(mapOf(DayOfWeek.SATURDAY to 4.0, DayOfWeek.SUNDAY to 2.0))
        val plan = MotorDiagnostico.generarPlan(MotorDiagnostico.diagnosticar(vacante, niveles, cursos, horas, hoy), horas, hoy, cierre)
        val primera = plan.tareas.first()
        assertEquals(0.0, plan.avance(emptySet()), 0.0)
        assertEquals(primera.horas / 14.0, plan.avance(setOf(primera.id)), 1e-9)
        assertEquals(plan.tareas[1], plan.siguienteTarea(setOf(primera.id)))
    }

    @Test
    fun filtrosNoCumplidos() {
        assertEquals(listOf(sql), MotorDiagnostico.filtrosNoCumplidos(vacante, niveles))
    }
}
