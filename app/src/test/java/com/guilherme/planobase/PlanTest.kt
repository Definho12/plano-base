package com.guilherme.planobase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class PlanTest {
    @Test fun duracaoDosTreinos() {
        assertEquals(31, Plan.totalMinutes("A", 1))
        assertTrue(Plan.totalMinutes("C", 3) in 38..45)
    }

    @Test fun todosOsExerciciosTemDadosEAnimacao() {
        Plan.workouts.flatMap { it.exercises }.plus(Plan.FINISHER).forEach {
            assertTrue("sem texto: $it", Plan.exercises.containsKey(it))
            assertTrue("sem animação: $it", Anims.map.containsKey(it))
        }
    }

    @Test fun filaDeTreinos() {
        assertEquals("A", Plan.nextKey(emptyList()))
        val log = listOf(LogEntry("2026-11-02", "A", 1, 31, 3, false), LogEntry("2026-11-04", "B", 1, 10, null, true))
        assertEquals("B", Plan.nextKey(log))
        assertEquals("A", Plan.nextKey(listOf(LogEntry("2026-11-06", "C", 1, 31, 3, false))))
    }

    @Test fun proximoLembrete() {
        val cfg = ReminderCfg(true, 19, 0, setOf(1, 3, 5))
        // sexta, 06/11/2026, 20h -> próxima é segunda 09/11 às 19h
        assertEquals(LocalDateTime.of(2026, 11, 9, 19, 0), Plan.nextReminder(cfg, LocalDateTime.of(2026, 11, 6, 20, 0)))
        // sexta 10h -> hoje às 19h
        assertEquals(LocalDateTime.of(2026, 11, 6, 19, 0), Plan.nextReminder(cfg, LocalDateTime.of(2026, 11, 6, 10, 0)))
        assertEquals(null, Plan.nextReminder(cfg.copy(days = emptySet()), LocalDateTime.of(2026, 11, 6, 10, 0)))
    }

    @Test fun inicioDaSemana() {
        assertEquals(LocalDate.of(2026, 11, 2), Plan.weekStart(LocalDate.of(2026, 11, 6)))
        assertEquals(LocalDate.of(2026, 11, 2), Plan.weekStart(LocalDate.of(2026, 11, 8)))
    }
}
