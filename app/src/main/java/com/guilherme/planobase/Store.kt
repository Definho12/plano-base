package com.guilherme.planobase

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/** Dados do usuário, guardados no próprio celular. */
object Store {
    private var prefs: SharedPreferences? = null

    var level by mutableIntStateOf(1)
    val log = mutableStateListOf<LogEntry>()
    val weights = mutableStateListOf<WeightEntry>()
    var reminder by mutableStateOf(ReminderCfg(enabled = false, hour = 19, minute = 0, days = setOf(1, 3, 5)))

    fun init(ctx: Context) {
        if (prefs != null) return
        val p = ctx.applicationContext.getSharedPreferences("plano_base", Context.MODE_PRIVATE)
        prefs = p
        level = p.getInt("level", 1)
        log.clear(); weights.clear()
        runCatching {
            val a = JSONArray(p.getString("log", "[]"))
            for (i in 0 until a.length()) {
                val o = a.getJSONObject(i)
                log += LogEntry(o.getString("d"), o.getString("w"), o.getInt("level"), o.getInt("min"),
                    if (o.has("rpe")) o.getInt("rpe") else null, o.optBoolean("partial"))
            }
        }
        runCatching {
            val a = JSONArray(p.getString("weights", "[]"))
            for (i in 0 until a.length()) {
                val o = a.getJSONObject(i)
                weights += WeightEntry(o.getString("d"), o.getDouble("kg"), o.optBoolean("start"))
            }
        }
        if (weights.isEmpty()) weights += WeightEntry(LocalDate.now().toString(), 100.0, start = true)
        runCatching {
            val o = JSONObject(p.getString("reminder", "{}"))
            if (o.has("hour")) {
                val d = o.getJSONArray("days")
                reminder = ReminderCfg(o.getBoolean("enabled"), o.getInt("hour"), o.getInt("minute"),
                    (0 until d.length()).map { d.getInt(it) }.toSet())
            }
        }
        save()
    }

    fun save() {
        val p = prefs ?: return
        val la = JSONArray()
        log.forEach { e ->
            la.put(JSONObject().apply {
                put("d", e.date); put("w", e.workout); put("level", e.level); put("min", e.minutes)
                e.rpe?.let { put("rpe", it) }; put("partial", e.partial)
            })
        }
        val wa = JSONArray()
        weights.forEach { w -> wa.put(JSONObject().apply { put("d", w.date); put("kg", w.kg); put("start", w.start) }) }
        val r = reminder
        val ro = JSONObject().apply {
            put("enabled", r.enabled); put("hour", r.hour); put("minute", r.minute)
            put("days", JSONArray(r.days.sorted()))
        }
        p.edit().putInt("level", level).putString("log", la.toString())
            .putString("weights", wa.toString()).putString("reminder", ro.toString()).apply()
    }

    fun addLog(e: LogEntry) { log += e; save() }

    fun addWeight(kg: Double) {
        val today = LocalDate.now().toString()
        val i = weights.indexOfFirst { it.date == today && !it.start }
        if (i >= 0) weights[i] = WeightEntry(today, kg) else weights += WeightEntry(today, kg)
        save()
    }

    fun setLevelTo(n: Int) { level = n; save() }

    fun setReminderTo(ctx: Context, cfg: ReminderCfg) {
        reminder = cfg; save(); Reminders.schedule(ctx)
    }
}
