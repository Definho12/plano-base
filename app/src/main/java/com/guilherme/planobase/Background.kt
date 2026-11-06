package com.guilherme.planobase

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

object Notif {
    const val CH_WORKOUT = "treino"
    const val CH_REMINDER = "lembretes"
    const val ID_WORKOUT = 1
    const val ID_REMINDER = 2

    fun channels(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CH_WORKOUT, "Treino em andamento", NotificationManager.IMPORTANCE_LOW).apply {
            description = "Mostra o exercício atual e o tempo durante o treino"
            setShowBadge(false)
        })
        nm.createNotificationChannel(NotificationChannel(CH_REMINDER, "Lembretes de treino", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Avisa nos dias de treino"
        })
    }

    fun openApp(ctx: Context): PendingIntent = PendingIntent.getActivity(
        ctx, 0, Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
}

class WorkoutService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var watcher: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Notif.channels(this)
        val n = build(Engine.state.value)
        if (Build.VERSION.SDK_INT >= 34) startForeground(Notif.ID_WORKOUT, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        else startForeground(Notif.ID_WORKOUT, n)
        if (watcher == null) {
            val nm = getSystemService(NotificationManager::class.java)
            watcher = scope.launch {
                var last = ""
                Engine.state.collect { s ->
                    if (!s.active || s.finished) { stopSelf(); return@collect }
                    val key = "${s.index}-${s.remainingMs / 1000}-${s.paused}"
                    if (key != last) { last = key; nm.notify(Notif.ID_WORKOUT, build(s)) }
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun build(s: PlayerState) = NotificationCompat.Builder(this, Notif.CH_WORKOUT)
        .setSmallIcon(R.drawable.ic_stat)
        .setContentTitle(s.step?.name ?: "Treino")
        .setContentText(
            (s.step?.phase ?: "") + " · " + (if (s.paused) "pausado · " else "") + clock(s.remainingMs))
        .setOngoing(true).setOnlyAlertOnce(true).setSilent(true)
        .setContentIntent(Notif.openApp(this))
        .setCategory(NotificationCompat.CATEGORY_PROGRESS)
        .build()

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}

fun clock(ms: Long): String {
    val s = ((ms + 999) / 1000).coerceAtLeast(0)
    return "%d:%02d".format(s / 60, s % 60)
}

object Reminders {
    private fun pending(ctx: Context) = PendingIntent.getBroadcast(
        ctx, 1, Intent(ctx, ReminderReceiver::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

    fun schedule(ctx: Context) {
        val am = ctx.getSystemService(AlarmManager::class.java) ?: return
        val pi = pending(ctx)
        am.cancel(pi)
        val cfg = Store.reminder
        if (!cfg.enabled) return
        val at = Plan.nextReminder(cfg, LocalDateTime.now()) ?: return
        val ms = at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val exact = Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()
        runCatching {
            if (exact) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, ms, pi)
            else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, ms, pi)
        }
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        Store.init(ctx)
        Notif.channels(ctx)
        val today = LocalDate.now().toString()
        val doneToday = Store.log.any { it.date == today && !it.partial }
        if (!doneToday) {
            val k = Plan.nextKey(Store.log)
            val w = Plan.workout(k)
            val n = NotificationCompat.Builder(ctx, Notif.CH_REMINDER)
                .setSmallIcon(R.drawable.ic_stat)
                .setContentTitle("Hora do treino $k")
                .setContentText("${w.name} · ${Plan.totalMinutes(k, Store.level)} min")
                .setAutoCancel(true)
                .setContentIntent(Notif.openApp(ctx))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()
            runCatching { ctx.getSystemService(NotificationManager::class.java).notify(Notif.ID_REMINDER, n) }
        }
        Reminders.schedule(ctx)
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Store.init(ctx)
            Reminders.schedule(ctx)
        }
    }
}
