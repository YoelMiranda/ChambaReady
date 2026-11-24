package pe.edu.esan.chambaready.core.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import pe.edu.esan.chambaready.core.domain.reglas.ReglasRanking

class ReglasRankingTest {

    @Test
    fun `dos de diez te ganan y quedas en el top 30`() {
        val otros = listOf(0.9, 0.95) + List(7) { 0.2 }
        assertEquals(30, ReglasRanking.topPorcentaje(0.6, otros))
    }

    @Test
    fun `el mejor del area queda en el top mas bajo posible`() {
        assertEquals(10, ReglasRanking.topPorcentaje(1.0, List(9) { 0.5 }))
        assertEquals(1, ReglasRanking.topPorcentaje(1.0, List(199) { 0.5 }))
    }

    @Test
    fun `los empates no te bajan`() {
        assertEquals(25, ReglasRanking.topPorcentaje(0.5, listOf(0.5, 0.5, 0.5)))
    }

    @Test
    fun `solo en el area es top 100`() {
        assertEquals(100, ReglasRanking.topPorcentaje(0.8, emptyList()))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `un puntaje invalido se rechaza`() {
        ReglasRanking.puntaje(4, 3)
    }
}
