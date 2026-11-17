package kz.alnazar.schedule

import android.app.Application
import kz.alnazar.schedule.data.Store
import kz.alnazar.schedule.widget.WidgetUpdater

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Store.ensure(this)
        WidgetUpdater.scheduleNext(this)
    }
}
