package kz.alnazar.schedule.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.widget.RemoteViews
import kz.alnazar.schedule.R
import kz.alnazar.schedule.data.AppState
import kz.alnazar.schedule.data.Store
import kz.alnazar.schedule.logic.Now
import kz.alnazar.schedule.logic.Phase
import kz.alnazar.schedule.logic.Sched
import kz.alnazar.schedule.logic.Tr
import kz.alnazar.schedule.ui.MainActivity

/** Что показывать крупно: идущий урок или ближайший (с учётом настроек виджета). */
internal data class Pick(val pos: Int, val label: String)

internal fun pickSmall(s: AppState, n: Now, lang: String): Pick? = when (n.phase) {
    Phase.LESSON -> if (s.widget.onLesson == "next" && n.next >= 0) Pick(n.next, Tr.t(lang, "next"))
                    else Pick(n.cur, Tr.t(lang, "now"))
    Phase.BREAK -> if (s.widget.onBreak == "prev" && n.prev >= 0) Pick(n.prev, Tr.t(lang, "break") + " · " + Tr.t(lang, "prev").lowercase())
                   else Pick(n.next, Tr.t(lang, "break") + " · " + Tr.t(lang, "next").lowercase())
    Phase.BEFORE -> Pick(n.next, Tr.t(lang, "soon") + " · " + Tr.t(lang, "next").lowercase())
    else -> null
}

internal fun mainPos(n: Now): Int = when (n.phase) {
    Phase.LESSON -> n.cur
    Phase.BREAK, Phase.BEFORE -> n.next
    else -> -1
}

/** Виджет — одна картинка (см. WRender) и нажатие, которое открывает приложение. */
abstract class LessonProvider(private val size: String, private val defW: Int, private val defH: Int) : AppWidgetProvider() {

    override fun onUpdate(context: Context, mgr: AppWidgetManager, ids: IntArray) {
        Store.ensure(context)
        for (id in ids) push(context, mgr, id, size, defW, defH)
        WidgetUpdater.scheduleNext(context)
    }

    override fun onAppWidgetOptionsChanged(context: Context, mgr: AppWidgetManager, id: Int, newOptions: Bundle) {
        Store.ensure(context)
        push(context, mgr, id, size, defW, defH)
    }

    override fun onEnabled(context: Context) { WidgetUpdater.scheduleNext(context) }

    companion object {
        /** Перерисовать один виджет по его реальному размеру на рабочем столе. */
        fun push(ctx: Context, mgr: AppWidgetManager, id: Int, size: String, defW: Int, defH: Int) {
            val o = mgr.getAppWidgetOptions(id)
            val port = ctx.resources.configuration.orientation != Configuration.ORIENTATION_LANDSCAPE
            var w = (if (port) o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH) else o.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH)).toFloat()
            var h = (if (port) o.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT) else o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)).toFloat()
            if (w < 40f) w = defW.toFloat()
            if (h < 40f) h = defH.toFloat()
            val s = Store.state.value
            val n = Sched.now(s)
            val bmp = runCatching { WRender.render(ctx, s, n, size, w, h) }.getOrNull()
            val rv = RemoteViews(ctx.packageName, R.layout.widget_img)
            if (bmp != null) rv.setImageViewBitmap(R.id.wimg, bmp)
            rv.setContentDescription(R.id.wimg, WRender.describe(s, n))
            rv.setOnClickPendingIntent(
                R.id.wroot,
                PendingIntent.getActivity(ctx, 0, Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE),
            )
            mgr.updateAppWidget(id, rv)
        }

        fun updateAll(ctx: Context, cls: Class<*>, size: String, defW: Int, defH: Int) {
            val mgr = AppWidgetManager.getInstance(ctx)
            for (id in mgr.getAppWidgetIds(ComponentName(ctx, cls))) push(ctx, mgr, id, size, defW, defH)
        }
    }
}

class W22Receiver : LessonProvider("2x2", 170, 170)
class W21Receiver : LessonProvider("2x1", 170, 84)
class W12Receiver : LessonProvider("1x2", 80, 180)
class W24Receiver : LessonProvider("2x4", 170, 380)
class W42Receiver : LessonProvider("4x2", 350, 180)
