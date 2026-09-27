package pl.edwin.budowlanka.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Database(
    entities = [
        WorkEntity::class, MaterialEntity::class, WorkMaterialEntity::class,
        ToolEntity::class, WorkToolEntity::class, PackageEntity::class, PackageWorkEntity::class,
        ClientEntity::class, SiteEntity::class, EstimateEntity::class, SpaceEntity::class,
        EstimateWorkEntity::class, CrewMemberEntity::class, EstimateCrewEntity::class,
        ExtraCostEntity::class, PhotoEntity::class, OpeningEntity::class, MaterialPriceHistoryEntity::class,
        ShoppingItemEntity::class, ToolChecklistEntity::class, AppSettingsEntity::class,
        WorkSessionEntity::class, WorkSessionEventEntity::class
    ],
    version = 8,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): AppDao

    companion object {
        private const val DB_NAME = "budowlanka.db"

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

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE estimate_works ADD COLUMN laborRateOverride REAL")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE estimate_works ADD COLUMN laborPriceMode TEXT NOT NULL DEFAULT 'KATALOG'")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE works ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE works ADD COLUMN isUserDefined INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS openings (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "spaceId INTEGER NOT NULL, " +
                        "type TEXT NOT NULL, " +
                        "name TEXT NOT NULL, " +
                        "width REAL NOT NULL, " +
                        "height REAL NOT NULL, " +
                        "quantity INTEGER NOT NULL)"
                )
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE works ADD COLUMN defaultQuantitySource TEXT NOT NULL DEFAULT 'MANUAL'")

                // Sensowne wartości startowe dla istniejącego cennika.
                db.execSQL("UPDATE works SET defaultQuantitySource='PODLOGA' WHERE name LIKE '%podłog%' OR name LIKE '%posadzk%' OR name LIKE '%wylewk%' OR name LIKE '%panel%'")
                db.execSQL("UPDATE works SET defaultQuantitySource='SUFIT' WHERE name LIKE '%sufit%'")
                db.execSQL("UPDATE works SET defaultQuantitySource='SCIANY' WHERE name LIKE '%ścian%' OR name LIKE '%tynk%' OR name LIKE '%gład%' OR name LIKE '%malowan%' OR name LIKE '%elewac%'")
                db.execSQL("UPDATE works SET defaultQuantitySource='OBWOD' WHERE name LIKE '%listw%' OR name LIKE '%cokoł%' OR name LIKE '%cokół%'")
                db.execSQL("UPDATE works SET defaultQuantitySource='SZTUKI' WHERE unit LIKE '%szt%'")
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS work_sessions (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "estimateId INTEGER NOT NULL, " +
                        "crewMemberId INTEGER, " +
                        "type TEXT NOT NULL, " +
                        "startAt INTEGER NOT NULL, " +
                        "endAt INTEGER, " +
                        "endReason TEXT NOT NULL, " +
                        "createdAt INTEGER NOT NULL, " +
                        "updatedAt INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS work_session_events (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "estimateId INTEGER NOT NULL, " +
                        "sessionId INTEGER, " +
                        "crewMemberId INTEGER, " +
                        "eventType TEXT NOT NULL, " +
                        "at INTEGER NOT NULL, " +
                        "note TEXT NOT NULL)"
                )
            }
        }

        @Volatile private var INSTANCE: AppDatabase? = null

        private fun backupBeforeUpgrade(context: Context, targetVersion: Int) {
            val source = context.getDatabasePath(DB_NAME)
            if (!source.exists()) return

            val prefs = context.getSharedPreferences("db_upgrade_backups", Context.MODE_PRIVATE)
            val key = "backup_before_v$targetVersion"
            if (prefs.getBoolean(key, false)) return

            runCatching {
                val dir = File(context.filesDir, "automatic-db-backups").apply { mkdirs() }
                val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                val base = File(dir, "budowlanka_before_v${targetVersion}_$stamp.db")
                source.copyTo(base, overwrite = true)

                listOf("-wal", "-shm").forEach { suffix ->
                    val extra = File(source.absolutePath + suffix)
                    if (extra.exists()) extra.copyTo(File(base.absolutePath + suffix), overwrite = true)
                }
                prefs.edit().putBoolean(key, true).apply()
            }
        }

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: run {
                    backupBeforeUpgrade(context.applicationContext, 8)
                    Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        DB_NAME
                    )
                        .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
                        .build()
                        .also { INSTANCE = it }
                }
            }
    }
}
