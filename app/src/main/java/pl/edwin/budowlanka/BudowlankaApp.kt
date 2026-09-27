package pl.edwin.budowlanka

import android.app.Application
import pl.edwin.budowlanka.data.AppDatabase

class BudowlankaApp : Application() {
    val database by lazy { AppDatabase.get(this) }
}
