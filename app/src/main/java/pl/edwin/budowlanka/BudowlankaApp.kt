package pl.edwin.budowlanka

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import pl.edwin.budowlanka.data.AppDatabase
import pl.edwin.budowlanka.util.PcSyncServer
import pl.edwin.budowlanka.util.WorkTimerNotifications

class BudowlankaApp : Application() {
    val database by lazy { AppDatabase.get(this) }
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        val dao = database.dao()

        // Room jest źródłem prawdy dla licznika. Po odtworzeniu procesu Androida
        // odbudowujemy powiadomienia dla wszystkich nadal aktywnych sesji.
        appScope.launch {
            WorkTimerNotifications.restoreActive(this@BudowlankaApp, dao)
        }

        if (PcSyncServer.shouldAutoStart(this)) {
            PcSyncServer.start(this, dao)
        }
    }
}
