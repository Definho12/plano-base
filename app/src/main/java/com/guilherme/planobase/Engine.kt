package com.guilherme.planobase

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

data class PlayerState(
    val active: Boolean = false,
    val workout: String = "A",
    val level: Int = 1,
    val steps: List<Step> = emptyList(),
    val index: Int = 0,
    val remainingMs: Long = 0,
    val paused: Boolean = false,
    val finished: Boolean = false,
    val spentMs: Long = 0,
) {
    val step: Step? get() = steps.getOrNull(index)
    val totalMs: Long get() = steps.sumOf { it.seconds * 1000L }
    val leftMs: Long get() = steps.drop(index + 1).sumOf { it.seconds * 1000L } + max(0L, remainingMs)
    val elapsedMs: Long get() = spentMs + ((step?.seconds ?: 0) * 1000L - max(0L, remainingMs))
}

/** Relógio do treino. Fica num serviço em primeiro plano, então segue contando com a tela bloqueada. */
object Engine {
    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var job: Job? = null
    private var endAt = 0L
    private var lastBeep = -1
    private var app: Context? = null
    private var tone: ToneGenerator? = null

    private fun now() = SystemClock.elapsedRealtime()

    fun start(ctx: Context, key: String, level: Int) {
        val c = ctx.applicationContext
        app = c
        val steps = Plan.buildSteps(key, level)
        endAt = now() + steps[0].seconds * 1000L
        lastBeep = -1
        _state.value = PlayerState(active = true, workout = key, level = level, steps = steps, remainingMs = steps[0].seconds * 1000L)
        runCatching { c.startForegroundService(Intent(c, WorkoutService::class.java)) }
        beep(ToneGenerator.TONE_PROP_BEEP2, 300); vibrate(200)
        ticker()
    }

    private fun ticker() {
        job?.cancel()
        job = scope.launch {
            while (isActive) { tick(); delay(200) }
        }
    }

    private fun tick() {
        val s = _state.value
        if (!s.active || s.paused || s.finished) return
        val rem = endAt - now()
        val sec = ceil(rem / 1000.0).toInt()
        if (sec in 1..3 && sec != lastBeep) { lastBeep = sec; beep(ToneGenerator.TONE_PROP_BEEP, 120) }
        if (rem <= 0) {
            val spent = s.spentMs + s.step!!.seconds * 1000L
            if (s.index + 1 < s.steps.size) {
                goTo(s.index + 1, spent)
                beep(ToneGenerator.TONE_PROP_BEEP2, 300); vibrate(350)
            } else finish(spent)
        } else {
            _state.value = s.copy(remainingMs = rem)
        }
    }

    private fun goTo(i: Int, spent: Long) {
        val s = _state.value
        val ms = s.steps[i].seconds * 1000L
        endAt = now() + ms
        lastBeep = -1
        _state.value = s.copy(index = i, remainingMs = ms, spentMs = spent)
    }

    fun togglePause() {
        val s = _state.value
        if (!s.active || s.finished) return
        if (s.paused) {
            endAt = now() + s.remainingMs
            _state.value = s.copy(paused = false)
        } else {
            _state.value = s.copy(paused = true, remainingMs = max(0L, endAt - now()))
        }
    }

    fun pause() { if (!_state.value.paused) togglePause() }

    fun next() {
        val s = _state.value
        if (!s.active || s.finished) return
        val rem = if (s.paused) s.remainingMs else endAt - now()
        val spent = s.spentMs + s.step!!.seconds * 1000L - max(0L, rem)
        if (s.index + 1 < s.steps.size) goTo(s.index + 1, spent) else finish(spent)
    }

    fun prev() {
        val s = _state.value
        if (!s.active || s.finished) return
        val i = max(0, s.index - 1)
        // o tempo já gasto no passo anterior volta a contar quando ele for refeito
        val back = if (i < s.index) s.steps[i].seconds * 1000L else 0L
        goTo(i, max(0L, s.spentMs - back))
    }

    private fun finish(spent: Long) {
        job?.cancel()
        _state.value = _state.value.copy(finished = true, spentMs = spent, remainingMs = 0, paused = false)
        beep(ToneGenerator.TONE_PROP_ACK, 600); vibrate(600)
    }

    /** Salva no histórico. rpe = null quando é treino parcial. */
    fun save(rpe: Int?, partial: Boolean) {
        val s = _state.value
        val elapsed = if (s.finished) s.spentMs else s.elapsedMs
        Store.addLog(LogEntry(LocalDate.now().toString(), s.workout, s.level,
            max(1, (elapsed / 60000.0).roundToInt()), rpe, partial))
    }

    fun close() {
        job?.cancel()
        _state.value = PlayerState()
        app?.let { runCatching { it.stopService(Intent(it, WorkoutService::class.java)) } }
    }

    private fun beep(t: Int, ms: Int) {
        runCatching {
            val g = tone ?: ToneGenerator(AudioManager.STREAM_MUSIC, 90).also { tone = it }
            g.startTone(t, ms)
        }
    }

    private fun vibrate(ms: Long) {
        val c = app ?: return
        runCatching {
            val v: Vibrator? = if (Build.VERSION.SDK_INT >= 31)
                c.getSystemService(VibratorManager::class.java)?.defaultVibrator
            else @Suppress("DEPRECATION") c.getSystemService(Vibrator::class.java)
            v?.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }
}
