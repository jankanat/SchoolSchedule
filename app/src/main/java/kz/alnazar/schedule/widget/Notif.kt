package kz.alnazar.schedule.widget

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import kz.alnazar.schedule.R
import kz.alnazar.schedule.data.AppState
import kz.alnazar.schedule.data.Store
import kz.alnazar.schedule.logic.Sched
import kz.alnazar.schedule.logic.Tr
import kz.alnazar.schedule.ui.MainActivity
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Напоминания о домашке:
 *  • вечером накануне — список заданий на следующий учебный день;
 *  • перед уроком — за N минут до урока, к которому задано.
 * Ставится один будильник на ближайшее событие; при срабатывании — уведомления и следующий будильник.
 */
object Notif {
    private const val CH = "hw"

    data class Ev(val at: LocalDateTime, val id: Int, val title: String, val text: String)

    fun allowed(ctx: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun events(st: AppState, from: LocalDate = LocalDate.now()): List<Ev> {
        val lang = st.appLang
        val out = ArrayList<Ev>()
        val open = st.hw.filter { it.kind == "hw" && !it.done && it.due != null && it.subj != null }
        if (open.isEmpty()) return out
        if (st.notif.evening) {
            for (k in 0..7) {
                val d = from.plusDays(k.toLong())
                val nd = Sched.nextSchoolDay(st, d) ?: continue
                val list = open.filter { it.due == nd.toString() }
                if (list.isEmpty()) continue
                out += Ev(
                    d.atStartOfDay().plusMinutes(st.notif.eveningAt.toLong()), 1000 + k,
                    Tr.t(lang, "n_t_evening", Sched.dayLabel(lang, d, nd).lowercase()),
                    list.joinToString("\n") { Sched.subj(st, it.subj!!, lang).name + ": " + it.text.ifBlank { "—" } },
                )
            }
        }
        if (st.notif.before) {
            open.groupBy { it.subj!! to it.due!! }.forEach { (key, list) ->
                val d = runCatching { LocalDate.parse(key.second) }.getOrNull() ?: return@forEach
                if (Sched.holiday(st, d) != null) return@forEach
                val slot = Sched.dayLessons(st, d).firstOrNull { it.lesson.subj == key.first } ?: return@forEach
                val name = Sched.subj(st, key.first, lang).name
                out += Ev(
                    d.atStartOfDay().plusMinutes((slot.start - st.notif.beforeMin).toLong()),
                    (key.first + key.second).hashCode() and 0xFFFFF,
                    Tr.t(lang, "n_t_before", name, st.notif.beforeMin),
                    list.joinToString("\n") { it.text.ifBlank { "📝" } },
                )
            }
        }
        return out.sortedBy { it.at }
    }

    fun schedule(ctx: Context) {
        val app = ctx.applicationContext
        val st = Store.ensure(app)
        val am = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = PendingIntent.getBroadcast(app, 2, Intent(app, HwReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val now = LocalDateTime.now()
        val next = if (st.notif.evening || st.notif.before) events(st).firstOrNull { it.at.isAfter(now) } else null
        if (next == null) { am.cancel(pi); return }
        val at = next.at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        if (am.canScheduleExactAlarms()) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        else am.setWindow(AlarmManager.RTC_WAKEUP, at, 5 * 60_000L, pi)
    }

    fun channel(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        val lang = Store.ensure(ctx).appLang
        nm.createNotificationChannel(NotificationChannel(CH, Tr.t(lang, "n_ch"), NotificationManager.IMPORTANCE_DEFAULT))
    }

    fun post(ctx: Context, e: Ev) {
        if (!allowed(ctx)) return
        channel(ctx)
        val open = PendingIntent.getActivity(ctx, 3, Intent(ctx, MainActivity::class.java).putExtra("tab", 3)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val n = NotificationCompat.Builder(ctx, CH)
            .setSmallIcon(R.drawable.ic_notif)
            .setContentTitle(e.title)
            .setContentText(e.text.lineSequence().first())
            .setStyle(NotificationCompat.BigTextStyle().bigText(e.text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        runCatching { NotificationManagerCompat.from(ctx).notify(e.id, n) }
    }

    /** Будильник сработал: показать всё, что пришлось на эту минуту (± пара минут из-за неточного режима). */
    fun fire(ctx: Context) {
        val st = Store.ensure(ctx)
        val now = LocalDateTime.now()
        events(st, now.toLocalDate().minusDays(1))
            .filter { !it.at.isAfter(now.plusMinutes(1)) && it.at.isAfter(now.minusMinutes(6)) }
            .forEach { post(ctx, it) }
        schedule(ctx)
    }
}

class HwReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = Notif.fire(context.applicationContext)
}
