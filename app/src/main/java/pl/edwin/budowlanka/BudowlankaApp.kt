package pl.edwin.budowlanka

import android.app.Application
import pl.edwin.budowlanka.data.AppDatabase
import pl.edwin.budowlanka.util.PcSyncServer

class BudowlankaApp : Application() {
    val database by lazy { AppDatabase.get(this) }

    override fun onCreate() {
        super.onCreate()
        if (PcSyncServer.shouldAutoStart(this)) {
            PcSyncServer.start(this, database.dao())
        }
    }
}
