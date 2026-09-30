package pl.edwin.budowlanka.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class DatabaseRegressionTest {
    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var dao: AppDao

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.dao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun migrationChainIsContinuousFrom1ToCurrentSchema() {
        val path = AppDatabase.ALL_MIGRATIONS.map { it.startVersion to it.endVersion }

        assertEquals(
            listOf(
                1 to 2,
                2 to 3,
                3 to 4,
                4 to 5,
                5 to 6,
                6 to 7,
                7 to 8
            ),
            path
        )
    }

    @Test
    fun migration7To8PreservesExistingDataAndCreatesTimeTables() = runBlocking {
        db.close()

        val name = "migration_7_8_${System.nanoTime()}.db"
        context.deleteDatabase(name)

        var fileDb = Room.databaseBuilder(context, AppDatabase::class.java, name)
            .allowMainThreadQueries()
            .addMigrations(*AppDatabase.ALL_MIGRATIONS)
            .build()

        val oldClientId = fileDb.dao().upsertClient(ClientEntity(name = "Klient przed aktualizacją"))
        val oldEstimateId = fileDb.dao().upsertEstimate(
            EstimateEntity(title = "Zlecenie przed aktualizacją", clientId = oldClientId)
        )
        fileDb.close()

        val path = context.getDatabasePath(name).absolutePath
        SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.OPEN_READWRITE).use { sql ->
            sql.execSQL("DROP TABLE work_session_events")
            sql.execSQL("DROP TABLE work_sessions")
            sql.version = 7
        }

        fileDb = Room.databaseBuilder(context, AppDatabase::class.java, name)
            .allowMainThreadQueries()
            .addMigrations(*AppDatabase.ALL_MIGRATIONS)
            .build()

        assertNotNull(fileDb.dao().getClient(oldClientId))
        assertNotNull(fileDb.dao().getEstimate(oldEstimateId))
        assertTrue(fileDb.dao().allWorkSessions().isEmpty())
        assertTrue(fileDb.dao().allWorkSessionEvents().isEmpty())

        val sessionId = fileDb.dao().upsertWorkSession(
            WorkSessionEntity(
                estimateId = oldEstimateId,
                startAt = 1000L,
                createdAt = 1000L,
                updatedAt = 1000L
            )
        )
        assertTrue(sessionId > 0L)

        fileDb.close()
        context.deleteDatabase(name)

        // Odtwórz bazę in-memory dla @After.
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.dao()
    }

    @Test
    fun deletingSpaceRemovesItsScopeButKeepsPhotoOnEstimate() = runBlocking {
        val estimateId = dao.upsertEstimate(EstimateEntity(title = "Test pomieszczenia"))
        val workId = dao.upsertWork(WorkEntity(name = "Malowanie"))
        val spaceId = dao.upsertSpace(
            SpaceEntity(
                estimateId = estimateId,
                name = "Salon",
                length = 5.0,
                width = 4.0
            )
        )
        val lineId = dao.upsertEstimateWork(
            EstimateWorkEntity(
                estimateId = estimateId,
                workId = workId,
                spaceId = spaceId,
                quantitySource = QuantitySource.WALLS
            )
        )
        dao.upsertOpening(
            OpeningEntity(
                spaceId = spaceId,
                type = OpeningType.WINDOW,
                name = "Okno",
                width = 1.5,
                height = 1.2
            )
        )
        val photoId = dao.upsertPhoto(
            PhotoEntity(
                estimateId = estimateId,
                spaceId = spaceId,
                estimateWorkId = lineId,
                uri = "content://regression/photo"
            )
        )

        dao.deleteSpaceSafely(spaceId)

        assertTrue(dao.allSpaces().none { it.id == spaceId })
        assertTrue(dao.allOpenings().none { it.spaceId == spaceId })
        assertTrue(dao.allEstimateWorks().none { it.spaceId == spaceId })

        val photo = dao.allPhotos().single { it.id == photoId }
        assertEquals(estimateId, photo.estimateId)
        assertNull(photo.spaceId)
        assertNull(photo.estimateWorkId)
    }

    @Test
    fun startStopStartIsIdempotentAndDoesNotDuplicateEvents() = runBlocking {
        val crewA = dao.upsertCrewMember(CrewMemberEntity(name = "A"))
        val crewB = dao.upsertCrewMember(CrewMemberEntity(name = "B"))
        val estimateId = dao.upsertEstimate(
            EstimateEntity(title = "Timer", status = EstimateStatus.ACCEPTED)
        )

        val firstStart = dao.startWorkSessionsSafely(
            estimateId = estimateId,
            type = WorkTimeType.WORK,
            crewIds = listOf(crewA, crewB),
            now = 1_000L
        )
        val duplicateStart = dao.startWorkSessionsSafely(
            estimateId = estimateId,
            type = WorkTimeType.WORK,
            crewIds = listOf(crewA, crewB),
            now = 1_001L
        )

        assertTrue(firstStart)
        assertFalse(duplicateStart)
        assertEquals(2, dao.activeWorkSessions(estimateId).size)
        assertEquals(
            2,
            dao.allWorkSessionEvents().count { it.eventType == WorkSessionEventType.START }
        )
        assertEquals(EstimateStatus.IN_PROGRESS, dao.getEstimate(estimateId)?.status)

        val firstStop = dao.stopWorkSessionsSafely(
            estimateId = estimateId,
            endAt = 2_000L,
            endReason = WorkEndReason.STOP
        )
        val duplicateStop = dao.stopWorkSessionsSafely(
            estimateId = estimateId,
            endAt = 2_001L,
            endReason = WorkEndReason.STOP
        )

        assertTrue(firstStop)
        assertFalse(duplicateStop)
        assertTrue(dao.activeWorkSessions(estimateId).isEmpty())
        assertEquals(
            2,
            dao.allWorkSessionEvents().count { it.eventType == WorkSessionEventType.STOP }
        )

        val restart = dao.startWorkSessionsSafely(
            estimateId = estimateId,
            type = WorkTimeType.WORK,
            crewIds = listOf(crewA, crewB),
            now = 3_000L
        )

        assertTrue(restart)
        assertEquals(2, dao.activeWorkSessions(estimateId).size)
        assertEquals(4, dao.allWorkSessions().size)
        assertEquals(
            4,
            dao.allWorkSessionEvents().count { it.eventType == WorkSessionEventType.START }
        )
    }

    @Test
    fun dataSurvivesDatabaseCloseAndReopen() {
        runBlocking {
        val name = "reopen_${System.nanoTime()}.db"
        context.deleteDatabase(name)

        var fileDb = Room.databaseBuilder(context, AppDatabase::class.java, name)
            .allowMainThreadQueries()
            .addMigrations(*AppDatabase.ALL_MIGRATIONS)
            .build()

        val clientId = fileDb.dao().upsertClient(
            ClientEntity(name = "Klient trwały", phone = "123456789")
        )
        val siteId = fileDb.dao().upsertSite(
            SiteEntity(clientId = clientId, address = "Testowa 1")
        )
        val estimateId = fileDb.dao().upsertEstimate(
            EstimateEntity(
                title = "Dane po aktualizacji",
                clientId = clientId,
                siteId = siteId,
                notes = "Nie zgubić"
            )
        )
        fileDb.close()

        fileDb = Room.databaseBuilder(context, AppDatabase::class.java, name)
            .allowMainThreadQueries()
            .addMigrations(*AppDatabase.ALL_MIGRATIONS)
            .build()

        val client = fileDb.dao().getClient(clientId)
        val estimate = fileDb.dao().getEstimate(estimateId)

        assertEquals("Klient trwały", client?.name)
        assertEquals("123456789", client?.phone)
        assertEquals("Dane po aktualizacji", estimate?.title)
        assertEquals("Nie zgubić", estimate?.notes)
        assertEquals(siteId, estimate?.siteId)

            fileDb.close()
            context.deleteDatabase(name)
        }
    }
}
