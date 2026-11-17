package kz.alnazar.schedule

import android.app.Application
import kz.alnazar.schedule.data.Store
import kz.alnazar.schedule.widget.WidgetUpdater

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        kz.alnazar.schedule.data.Files.init(this)
        val st = Store.ensure(this)
        WidgetUpdater.scheduleNext(this)
        runCatching { kz.alnazar.schedule.widget.Notif.schedule(this) }
        kz.alnazar.schedule.data.Files.fetchAll(this, st)
    }
}
