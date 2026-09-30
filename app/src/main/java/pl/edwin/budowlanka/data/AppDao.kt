package pl.edwin.budowlanka.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    @Query("SELECT * FROM works ORDER BY category,name")
    fun observeWorks(): Flow<List<WorkEntity>>
    @Query("SELECT * FROM materials ORDER BY name")
    fun observeMaterials(): Flow<List<MaterialEntity>>
    @Query("SELECT * FROM tools ORDER BY name")
    fun observeTools(): Flow<List<ToolEntity>>
    @Query("SELECT * FROM packages ORDER BY name")
    fun observePackages(): Flow<List<PackageEntity>>
    @Query("SELECT * FROM package_works ORDER BY packageId,position")
    fun observePackageWorks(): Flow<List<PackageWorkEntity>>
    @Query("SELECT * FROM work_materials")
    fun observeWorkMaterials(): Flow<List<WorkMaterialEntity>>
    @Query("SELECT * FROM work_tools")
    fun observeWorkTools(): Flow<List<WorkToolEntity>>
    @Query("SELECT * FROM clients ORDER BY name")
    fun observeClients(): Flow<List<ClientEntity>>
    @Query("SELECT * FROM sites ORDER BY address")
    fun observeSites(): Flow<List<SiteEntity>>
    @Query("SELECT * FROM estimates ORDER BY createdAt DESC")
    fun observeEstimates(): Flow<List<EstimateEntity>>
    @Query("SELECT * FROM spaces")
    fun observeSpaces(): Flow<List<SpaceEntity>>
    @Query("SELECT * FROM estimate_works")
    fun observeEstimateWorks(): Flow<List<EstimateWorkEntity>>
    @Query("SELECT * FROM openings")
    fun observeOpenings(): Flow<List<OpeningEntity>>
    @Query("SELECT * FROM crew_members ORDER BY name")
    fun observeCrewMembers(): Flow<List<CrewMemberEntity>>
    @Query("SELECT * FROM estimate_crew")
    fun observeEstimateCrew(): Flow<List<EstimateCrewEntity>>
    @Query("SELECT * FROM extra_costs")
    fun observeExtraCosts(): Flow<List<ExtraCostEntity>>
    @Query("SELECT * FROM photos")
    fun observePhotos(): Flow<List<PhotoEntity>>
    @Query("SELECT * FROM shopping_items")
    fun observeShopping(): Flow<List<ShoppingItemEntity>>
    @Query("SELECT * FROM tool_checklist")
    fun observeToolChecklist(): Flow<List<ToolChecklistEntity>>
    @Query("SELECT * FROM material_price_history ORDER BY changedAt DESC")
    fun observePriceHistory(): Flow<List<MaterialPriceHistoryEntity>>
    @Query("SELECT * FROM app_settings WHERE id=1")
    fun observeSettings(): Flow<AppSettingsEntity?>
    @Query("SELECT * FROM work_sessions ORDER BY startAt DESC")
    fun observeWorkSessions(): Flow<List<WorkSessionEntity>>
    @Query("SELECT * FROM work_session_events ORDER BY at DESC")
    fun observeWorkSessionEvents(): Flow<List<WorkSessionEventEntity>>

    @Query("SELECT COUNT(*) FROM works")
    suspend fun countWorks(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertWork(v: WorkEntity): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertMaterial(v: MaterialEntity): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertTool(v: ToolEntity): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertPackage(v: PackageEntity): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertPackageWork(v: PackageWorkEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertWorkMaterial(v: WorkMaterialEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertWorkTool(v: WorkToolEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertClient(v: ClientEntity): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertSite(v: SiteEntity): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertEstimate(v: EstimateEntity): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertSpace(v: SpaceEntity): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertEstimateWork(v: EstimateWorkEntity): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertOpening(v: OpeningEntity): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertCrewMember(v: CrewMemberEntity): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertEstimateCrew(v: EstimateCrewEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertExtraCost(v: ExtraCostEntity): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertPhoto(v: PhotoEntity): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertShopping(v: ShoppingItemEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertToolChecklist(v: ToolChecklistEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertPriceHistory(v: MaterialPriceHistoryEntity): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertSettings(v: AppSettingsEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertWorkSession(v: WorkSessionEntity): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertWorkSessionEvent(v: WorkSessionEventEntity): Long

    @Delete suspend fun deleteWork(v: WorkEntity)
    @Delete suspend fun deleteMaterial(v: MaterialEntity)
    @Delete suspend fun deleteTool(v: ToolEntity)
    @Query("DELETE FROM clients WHERE id = :id")
    suspend fun deleteClientById(id: Long)

    @Query("DELETE FROM sites WHERE id = :id")
    suspend fun deleteSiteById(id: Long)

    @Query("DELETE FROM estimates WHERE id = :id")
    suspend fun deleteEstimateById(id: Long)

    @Query("SELECT COUNT(*) FROM estimates WHERE clientId = :clientId")
    suspend fun countEstimatesForClient(clientId: Long): Int

    @Query("SELECT COUNT(*) FROM sites WHERE clientId = :clientId")
    suspend fun countSitesForClient(clientId: Long): Int

    @Query("SELECT COUNT(*) FROM estimates WHERE siteId = :siteId")
    suspend fun countEstimatesForSite(siteId: Long): Int
    @Delete suspend fun deleteSpace(v: SpaceEntity)
    @Delete suspend fun deleteEstimateWork(v: EstimateWorkEntity)
    @Delete suspend fun deleteOpening(v: OpeningEntity)
    @Delete suspend fun deleteCrewMember(v: CrewMemberEntity)
    @Delete suspend fun deleteExtraCost(v: ExtraCostEntity)
    @Delete suspend fun deletePhoto(v: PhotoEntity)

    @Query("UPDATE works SET active = 0 WHERE id = :id")
    suspend fun archiveWork(id: Long)

    @Query("UPDATE materials SET active = 0 WHERE id = :id")
    suspend fun archiveMaterial(id: Long)

    @Query("UPDATE tools SET active = 0 WHERE id = :id")
    suspend fun archiveTool(id: Long)

    @Query("""
        UPDATE photos
        SET spaceId = NULL, estimateWorkId = NULL
        WHERE spaceId = :spaceId
           OR estimateWorkId IN (SELECT id FROM estimate_works WHERE spaceId = :spaceId)
    """)
    suspend fun detachPhotosFromSpace(spaceId: Long)

    @Query("DELETE FROM openings WHERE spaceId = :spaceId")
    suspend fun deleteOpeningsForSpace(spaceId: Long)

    @Query("DELETE FROM estimate_works WHERE spaceId = :spaceId")
    suspend fun deleteEstimateWorksForSpace(spaceId: Long)

    @Query("DELETE FROM spaces WHERE id = :spaceId")
    suspend fun deleteSpaceById(spaceId: Long)

    @Query("DELETE FROM openings WHERE spaceId IN (SELECT id FROM spaces WHERE estimateId = :estimateId)")
    suspend fun deleteOpeningsForEstimate(estimateId: Long)

    @Query("DELETE FROM estimate_works WHERE estimateId = :estimateId")
    suspend fun deleteEstimateWorksForEstimate(estimateId: Long)

    @Query("DELETE FROM estimate_crew WHERE estimateId = :estimateId")
    suspend fun deleteCrewForEstimate(estimateId: Long)

    @Query("DELETE FROM extra_costs WHERE estimateId = :estimateId")
    suspend fun deleteExtraCostsForEstimate(estimateId: Long)

    @Query("DELETE FROM photos WHERE estimateId = :estimateId")
    suspend fun deletePhotosForEstimate(estimateId: Long)

    @Query("DELETE FROM shopping_items WHERE estimateId = :estimateId")
    suspend fun deleteShoppingForEstimate(estimateId: Long)

    @Query("DELETE FROM tool_checklist WHERE estimateId = :estimateId")
    suspend fun deleteToolsForEstimate(estimateId: Long)

    @Query("DELETE FROM work_session_events WHERE estimateId = :estimateId")
    suspend fun deleteWorkSessionEventsForEstimate(estimateId: Long)

    @Query("DELETE FROM work_sessions WHERE estimateId = :estimateId")
    suspend fun deleteWorkSessionsForEstimate(estimateId: Long)

    @Query("DELETE FROM spaces WHERE estimateId = :estimateId")
    suspend fun deleteSpacesForEstimate(estimateId: Long)

    @Transaction
    suspend fun deleteSpaceSafely(spaceId: Long) {
        detachPhotosFromSpace(spaceId)
        deleteOpeningsForSpace(spaceId)
        deleteEstimateWorksForSpace(spaceId)
        deleteSpaceById(spaceId)
    }

    @Transaction
    suspend fun deleteEstimateSafely(estimateId: Long): Boolean {
        val estimate = getEstimate(estimateId) ?: return false

        // Stabilizacja: nie kasujemy historii realizacji. Fizyczne usunięcie
        // jest dozwolone wyłącznie dla szkicu, który nie ma uruchomionych sesji.
        if (estimate.status != EstimateStatus.DRAFT) return false
        if (activeWorkSessions(estimateId).isNotEmpty()) return false

        deleteOpeningsForEstimate(estimateId)
        deleteEstimateWorksForEstimate(estimateId)
        deleteCrewForEstimate(estimateId)
        deleteExtraCostsForEstimate(estimateId)
        deletePhotosForEstimate(estimateId)
        deleteShoppingForEstimate(estimateId)
        deleteToolsForEstimate(estimateId)
        deleteWorkSessionEventsForEstimate(estimateId)
        deleteWorkSessionsForEstimate(estimateId)
        deleteSpacesForEstimate(estimateId)
        deleteEstimateById(estimateId)
        return true
    }

    @Transaction
    suspend fun deleteSiteSafely(siteId: Long): Boolean {
        if (countEstimatesForSite(siteId) > 0) return false
        deleteSiteById(siteId)
        return true
    }

    @Transaction
    suspend fun deleteClientSafely(clientId: Long): Boolean {
        // Klient z historią lub adresami nie może zostać fizycznie usunięty.
        if (countEstimatesForClient(clientId) > 0) return false
        if (countSitesForClient(clientId) > 0) return false
        deleteClientById(clientId)
        return true
    }

    @Query("DELETE FROM shopping_items WHERE estimateId=:estimateId")
    suspend fun clearShopping(estimateId: Long)
    @Query("DELETE FROM tool_checklist WHERE estimateId=:estimateId")
    suspend fun clearToolChecklist(estimateId: Long)
    @Query("DELETE FROM estimate_crew WHERE estimateId=:estimateId AND crewMemberId=:crewMemberId")
    suspend fun removeCrewFromEstimate(estimateId: Long, crewMemberId: Long)

    @Query("SELECT * FROM work_sessions WHERE estimateId=:estimateId AND endAt IS NULL ORDER BY startAt")
    suspend fun activeWorkSessions(estimateId: Long): List<WorkSessionEntity>

    @Query("SELECT * FROM work_sessions WHERE endAt IS NULL ORDER BY startAt")
    suspend fun allActiveWorkSessions(): List<WorkSessionEntity>

    @Query("UPDATE work_sessions SET endAt=:endAt, endReason=:endReason, updatedAt=:endAt WHERE estimateId=:estimateId AND endAt IS NULL")
    suspend fun stopActiveWorkSessions(estimateId: Long, endAt: Long, endReason: String)

    @Transaction
    suspend fun startWorkSessionsSafely(
        estimateId: Long,
        type: String,
        crewIds: List<Long?>,
        now: Long
    ): Boolean {
        if (activeWorkSessions(estimateId).isNotEmpty()) return false

        val estimate = getEstimate(estimateId) ?: return false
        val workers = crewIds.distinct().ifEmpty { listOf(null) }

        workers.forEach { crewId ->
            val sessionId = upsertWorkSession(
                WorkSessionEntity(
                    estimateId = estimateId,
                    crewMemberId = crewId,
                    type = type,
                    startAt = now,
                    createdAt = now,
                    updatedAt = now
                )
            )
            upsertWorkSessionEvent(
                WorkSessionEventEntity(
                    estimateId = estimateId,
                    sessionId = sessionId,
                    crewMemberId = crewId,
                    eventType = WorkSessionEventType.START,
                    at = now,
                    note = WorkTimeType.label(type)
                )
            )
        }

        if (estimate.status == EstimateStatus.ACCEPTED) {
            upsertEstimate(estimate.copy(status = EstimateStatus.IN_PROGRESS))
        }
        return true
    }

    @Transaction
    suspend fun stopWorkSessionsSafely(
        estimateId: Long,
        endAt: Long,
        endReason: String
    ): Boolean {
        val active = activeWorkSessions(estimateId)
        if (active.isEmpty()) return false

        stopActiveWorkSessions(estimateId, endAt, endReason)
        val eventType = if (endReason == WorkEndReason.PAUSE) {
            WorkSessionEventType.PAUSE
        } else {
            WorkSessionEventType.STOP
        }

        active.forEach { session ->
            upsertWorkSessionEvent(
                WorkSessionEventEntity(
                    estimateId = estimateId,
                    sessionId = session.id,
                    crewMemberId = session.crewMemberId,
                    eventType = eventType,
                    at = endAt,
                    note = WorkTimeType.label(session.type)
                )
            )
        }
        return true
    }

    @Query("SELECT * FROM estimates WHERE id=:id LIMIT 1")
    suspend fun getEstimate(id: Long): EstimateEntity?
    @Query("SELECT * FROM app_settings WHERE id=1 LIMIT 1")
    suspend fun getSettings(): AppSettingsEntity?
    @Query("SELECT * FROM spaces WHERE id=:id LIMIT 1")
    suspend fun getSpace(id: Long): SpaceEntity?
    @Query("SELECT * FROM clients WHERE id=:id LIMIT 1")
    suspend fun getClient(id: Long): ClientEntity?
    @Query("SELECT * FROM sites WHERE id=:id LIMIT 1")
    suspend fun getSite(id: Long): SiteEntity?

    @Query("SELECT * FROM works") suspend fun allWorks(): List<WorkEntity>
    @Query("SELECT * FROM materials") suspend fun allMaterials(): List<MaterialEntity>
    @Query("SELECT * FROM tools") suspend fun allTools(): List<ToolEntity>
    @Query("SELECT * FROM packages") suspend fun allPackages(): List<PackageEntity>
    @Query("SELECT * FROM package_works") suspend fun allPackageWorks(): List<PackageWorkEntity>
    @Query("SELECT * FROM work_materials") suspend fun allWorkMaterials(): List<WorkMaterialEntity>
    @Query("SELECT * FROM work_tools") suspend fun allWorkTools(): List<WorkToolEntity>
    @Query("SELECT * FROM clients") suspend fun allClients(): List<ClientEntity>
    @Query("SELECT * FROM sites") suspend fun allSites(): List<SiteEntity>
    @Query("SELECT * FROM estimates") suspend fun allEstimates(): List<EstimateEntity>
    @Query("SELECT * FROM spaces") suspend fun allSpaces(): List<SpaceEntity>
    @Query("SELECT * FROM estimate_works") suspend fun allEstimateWorks(): List<EstimateWorkEntity>
    @Query("SELECT * FROM openings") suspend fun allOpenings(): List<OpeningEntity>
    @Query("SELECT * FROM crew_members") suspend fun allCrewMembers(): List<CrewMemberEntity>
    @Query("SELECT * FROM estimate_crew") suspend fun allEstimateCrew(): List<EstimateCrewEntity>
    @Query("SELECT * FROM extra_costs") suspend fun allExtraCosts(): List<ExtraCostEntity>
    @Query("SELECT * FROM photos") suspend fun allPhotos(): List<PhotoEntity>
    @Query("SELECT * FROM shopping_items") suspend fun allShopping(): List<ShoppingItemEntity>
    @Query("SELECT * FROM tool_checklist") suspend fun allToolChecklist(): List<ToolChecklistEntity>
    @Query("SELECT * FROM material_price_history") suspend fun allPriceHistory(): List<MaterialPriceHistoryEntity>
    @Query("SELECT * FROM work_sessions") suspend fun allWorkSessions(): List<WorkSessionEntity>
    @Query("SELECT * FROM work_session_events") suspend fun allWorkSessionEvents(): List<WorkSessionEventEntity>
}
