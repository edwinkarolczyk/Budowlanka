package pl.edwin.budowlanka.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        WorkEntity::class, MaterialEntity::class, WorkMaterialEntity::class,
        ToolEntity::class, WorkToolEntity::class, PackageEntity::class, PackageWorkEntity::class,
        ClientEntity::class, SiteEntity::class, EstimateEntity::class, SpaceEntity::class,
        EstimateWorkEntity::class, CrewMemberEntity::class, EstimateCrewEntity::class,
        ExtraCostEntity::class, PhotoEntity::class, MaterialPriceHistoryEntity::class,
        ShoppingItemEntity::class, ToolChecklistEntity::class, AppSettingsEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): AppDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE works ADD COLUMN laborRateLow REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE works ADD COLUMN laborRateHigh REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE works ADD COLUMN priceRegion TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE works ADD COLUMN priceYear INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE works ADD COLUMN priceSource TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE works ADD COLUMN includesMaterial INTEGER NOT NULL DEFAULT 0")
            }
        }

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
