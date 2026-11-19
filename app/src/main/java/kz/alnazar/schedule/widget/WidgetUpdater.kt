package kz.alnazar.schedule.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kz.alnazar.schedule.data.Store
import kz.alnazar.schedule.logic.Sched
import java.time.ZoneId

/**
 * Виджеты перерисовываются ровно на границах уроков: будильник ставится на ближайший звонок,
 * при срабатывании виджеты обновляются и ставится следующий.
 */
object WidgetUpdater {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    suspend fun updateAllNow(ctx: Context) {
        Store.ensure(ctx)
        Store.tick.value = System.currentTimeMillis()
        W22().updateAll(ctx)
        W21().updateAll(ctx)
        W12().updateAll(ctx)
        W24().updateAll(ctx)
        W42().updateAll(ctx)
    }

    /** Обновить виджеты и переставить будильник (после любого изменения данных). */
    fun refresh(ctx: Context) {
        val app = ctx.applicationContext
        scope.launch {
            runCatching { updateAllNow(app) }
            scheduleNext(app)
        }
    }

    fun canExact(ctx: Context): Boolean =
        (ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager).canScheduleExactAlarms()

    fun scheduleNext(ctx: Context) {
        val app = ctx.applicationContext
        val st = Store.ensure(app)
        val at = Sched.nextBoundary(st).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val am = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = PendingIntent.getBroadcast(
            app, 1, Intent(app, TickReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        // во время уроков и перемен минуты на виджете идут каждую минуту — но будильник «не будящий» (RTC):
        // при выключенном экране он ждёт включения и батарею не тратит
        val n = Sched.now(st)
        val liveNow = n.phase == kz.alnazar.schedule.logic.Phase.LESSON || n.phase == kz.alnazar.schedule.logic.Phase.BREAK ||
            (n.phase == kz.alnazar.schedule.logic.Phase.BEFORE && n.minutesLeft <= 60)
        if (liveNow) {
            val nextMin = (System.currentTimeMillis() / 60_000L + 1) * 60_000L
            if (nextMin < at) { am.setWindow(AlarmManager.RTC, nextMin, 5_000L, pi); return }
        }
        if (am.canScheduleExactAlarms()) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC, at, pi)
        } else {
            // без разрешения на точные будильники — в пределах минуты
            am.setWindow(AlarmManager.RTC, at, 60_000L, pi)
        }
    }
}

/** Срабатывает на звонке: обновить виджеты и поставить следующий будильник. */
class TickReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val app = context.applicationContext
        CoroutineScope(Dispatchers.Default).launch {
            try {
                runCatching { WidgetUpdater.updateAllNow(app) }
                WidgetUpdater.scheduleNext(app)
            } finally {
                pending.finish()
            }
        }
    }
}

/** Перезагрузка телефона, смена времени/даты/пояса — пересчитать и переставить будильник. */
class SystemReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val app = context.applicationContext
        CoroutineScope(Dispatchers.Default).launch {
            try {
                runCatching { WidgetUpdater.updateAllNow(app) }
                WidgetUpdater.scheduleNext(app)
                runCatching { Notif.schedule(app) }
            } finally {
                pending.finish()
            }
        }
    }
}
