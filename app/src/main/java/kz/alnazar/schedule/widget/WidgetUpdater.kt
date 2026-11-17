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
            } finally {
                pending.finish()
            }
        }
    }
}
