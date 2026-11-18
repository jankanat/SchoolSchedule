package kz.alnazar.schedule

import android.app.Application
import kz.alnazar.schedule.data.Store
import kz.alnazar.schedule.widget.WidgetUpdater

class App : Application() {
    companion object {
        /** Контекст приложения — для действий из лямбд, где нет LocalContext. */
        lateinit var ctx: android.content.Context
    }

    override fun onCreate() {
        super.onCreate()
        ctx = applicationContext
        kz.alnazar.schedule.data.Files.init(this)
        val st = Store.ensure(this)
        WidgetUpdater.scheduleNext(this)
        runCatching { kz.alnazar.schedule.widget.Notif.schedule(this) }
        kz.alnazar.schedule.data.Files.fetchAll(this, st)
    }
}
