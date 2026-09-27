package pl.edwin.budowlanka.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        WorkEntity::class, MaterialEntity::class, WorkMaterialEntity::class,
        ToolEntity::class, WorkToolEntity::class, PackageEntity::class, PackageWorkEntity::class,
        ClientEntity::class, SiteEntity::class, EstimateEntity::class, SpaceEntity::class,
        EstimateWorkEntity::class, CrewMemberEntity::class, EstimateCrewEntity::class,
        ExtraCostEntity::class, PhotoEntity::class, MaterialPriceHistoryEntity::class,
        ShoppingItemEntity::class, ToolChecklistEntity::class, AppSettingsEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): AppDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "budowlanka.db"
                ).build().also { INSTANCE = it }
            }
    }
}
