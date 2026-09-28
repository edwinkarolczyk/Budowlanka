package pl.edwin.budowlanka

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import pl.edwin.budowlanka.data.*
import pl.edwin.budowlanka.domain.EstimateCalculator
import pl.edwin.budowlanka.util.PcSyncServer
import pl.edwin.budowlanka.util.WorkTimerNotifications

class MainViewModel(app: Application) : AndroidViewModel(app) {
    val dao = (app as BudowlankaApp).database.dao()

    val works = dao.observeWorks().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val materials = dao.observeMaterials().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val tools = dao.observeTools().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val packages = dao.observePackages().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val packageWorks = dao.observePackageWorks().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val workMaterials = dao.observeWorkMaterials().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val workTools = dao.observeWorkTools().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val clients = dao.observeClients().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val sites = dao.observeSites().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val estimates = dao.observeEstimates().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val spaces = dao.observeSpaces().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val openings = dao.observeOpenings().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val estimateWorks = dao.observeEstimateWorks().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val crewMembers = dao.observeCrewMembers().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val estimateCrew = dao.observeEstimateCrew().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val extraCosts = dao.observeExtraCosts().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val photos = dao.observePhotos().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val shopping = dao.observeShopping().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val toolChecklist = dao.observeToolChecklist().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val priceHistory = dao.observePriceHistory().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val workSessions = dao.observeWorkSessions().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val workSessionEvents = dao.observeWorkSessionEvents().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val settings = dao.observeSettings().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val workTimerMutex = Mutex()
    private val _pcSyncStatus = MutableStateFlow(PcSyncServer.currentStatus())
    val pcSyncStatus = _pcSyncStatus.asStateFlow()

    init {
        viewModelScope.launch { dao.ensureSeedData() }
    }

    fun addEstimate(title: String, onCreated: (Long) -> Unit = {}) = viewModelScope.launch {
        val s = dao.getSettings() ?: AppSettingsEntity()
        val id = dao.upsertEstimate(
            EstimateEntity(
                title = title.ifBlank { "Nowa wycena" },
                monthlyTargetSnapshot = s.monthlyTarget,
                workDaysMonthSnapshot = s.workDaysMonth,
                hoursDaySnapshot = s.hoursPerDay,
                kmRate = s.defaultKmRate,
                fixedTravelFee = s.defaultFixedTravelFee
            )
        )
        onCreated(id)
    }

    fun saveEstimate(v: EstimateEntity) = viewModelScope.launch {
        dao.upsertEstimate(v)
        if (v.status == EstimateStatus.ACCEPTED || v.status == EstimateStatus.IN_PROGRESS) {
            syncFulfillment(v.id)
        }
    }

    fun duplicateEstimate(sourceId: Long, onCreated: (Long) -> Unit = {}) = viewModelScope.launch {
        val source = dao.getEstimate(sourceId) ?: return@launch
        val newId = dao.upsertEstimate(
            source.copy(
                id = 0,
                title = source.title + " — kopia",
                status = EstimateStatus.DRAFT,
                createdAt = System.currentTimeMillis(),
                startDate = "",
                endDate = "",
                signatureData = ""
            )
        )

        val spaceMap = mutableMapOf<Long, Long>()
        dao.allSpaces().filter { it.estimateId == sourceId }.forEach { old ->
            val copiedId = dao.upsertSpace(old.copy(id = 0, estimateId = newId))
            spaceMap[old.id] = copiedId
        }

        dao.allOpenings().filter { it.spaceId in spaceMap.keys }.forEach { old ->
            spaceMap[old.spaceId]?.let { newSpaceId ->
                dao.upsertOpening(old.copy(id = 0, spaceId = newSpaceId))
            }
        }

        dao.allEstimateWorks().filter { it.estimateId == sourceId }.forEach { old ->
            dao.upsertEstimateWork(
                old.copy(
                    id = 0,
                    estimateId = newId,
                    spaceId = old.spaceId?.let { spaceMap[it] }
                )
            )
        }

        dao.allEstimateCrew().filter { it.estimateId == sourceId }.forEach { old ->
            dao.upsertEstimateCrew(old.copy(estimateId = newId))
        }

        dao.allExtraCosts().filter { it.estimateId == sourceId }.forEach { old ->
            dao.upsertExtraCost(old.copy(id = 0, estimateId = newId))
        }

        dao.allPhotos().filter { it.estimateId == sourceId }.forEach { old ->
            dao.upsertPhoto(
                old.copy(
                    id = 0,
                    estimateId = newId,
                    spaceId = old.spaceId?.let { spaceMap[it] },
                    estimateWorkId = null
                )
            )
        }

        onCreated(newId)
    }

    fun addWork(v: WorkEntity) = viewModelScope.launch {
        dao.upsertWork(if (v.id == 0L) v.copy(isUserDefined = true) else v)
    }
    fun toggleWorkFavorite(v: WorkEntity) = viewModelScope.launch {
        dao.upsertWork(v.copy(isFavorite = !v.isFavorite))
    }
    fun deleteWork(v: WorkEntity) = viewModelScope.launch { dao.deleteWork(v) }

    fun addMaterial(v: MaterialEntity) = viewModelScope.launch {
        val id = dao.upsertMaterial(v)
        dao.upsertPriceHistory(
            MaterialPriceHistoryEntity(
                materialId = if (v.id == 0L) id else v.id,
                priceBudget = v.priceBudget,
                priceStandard = v.priceStandard,
                pricePremium = v.pricePremium
            )
        )
    }
    fun deleteMaterial(v: MaterialEntity) = viewModelScope.launch { dao.deleteMaterial(v) }

    fun linkMaterial(workId: Long, materialId: Long, qty: Double) = viewModelScope.launch {
        dao.upsertWorkMaterial(WorkMaterialEntity(workId, materialId, qty))
    }

    fun addTool(v: ToolEntity) = viewModelScope.launch { dao.upsertTool(v) }
    fun deleteTool(v: ToolEntity) = viewModelScope.launch { dao.deleteTool(v) }
    fun linkTool(workId: Long, toolId: Long, qty: Int = 1) = viewModelScope.launch {
        dao.upsertWorkTool(WorkToolEntity(workId, toolId, qty))
    }

    fun addPackage(v: PackageEntity) = viewModelScope.launch { dao.upsertPackage(v) }
    fun addWorkToPackage(packageId: Long, workId: Long, position: Int) = viewModelScope.launch {
        dao.upsertPackageWork(PackageWorkEntity(packageId, workId, position))
    }

    fun addClient(v: ClientEntity) = viewModelScope.launch { dao.upsertClient(v) }
    fun addClientWithSite(
        client: ClientEntity,
        siteName: String,
        address: String,
        onCreated: (Long, Long?) -> Unit = { _, _ -> }
    ) = viewModelScope.launch {
        val clientId = dao.upsertClient(client)
        val siteId = if (address.isNotBlank()) {
            dao.upsertSite(
                SiteEntity(
                    clientId = clientId,
                    name = siteName.ifBlank { "Adres główny" },
                    address = address
                )
            )
        } else null
        onCreated(clientId, siteId)
    }
    fun addSite(v: SiteEntity) = viewModelScope.launch { dao.upsertSite(v) }

    fun addSpace(v: SpaceEntity) = viewModelScope.launch { dao.upsertSpace(v) }
    fun deleteSpace(v: SpaceEntity) = viewModelScope.launch { dao.deleteSpaceSafely(v.id) }

    private suspend fun refreshOpeningArea(spaceId: Long) {
        val space = dao.getSpace(spaceId) ?: return
        val total = dao.allOpenings().filter { it.spaceId == spaceId }.sumOf { it.area() }
        dao.upsertSpace(space.copy(openingsArea = total))
    }

    fun addOpening(v: OpeningEntity) = viewModelScope.launch {
        dao.upsertOpening(v)
        refreshOpeningArea(v.spaceId)
    }

    fun deleteOpening(v: OpeningEntity) = viewModelScope.launch {
        dao.deleteOpening(v)
        refreshOpeningArea(v.spaceId)
    }

    fun duplicateSpace(v: SpaceEntity) = viewModelScope.launch {
        val newId = dao.upsertSpace(v.copy(id = 0, name = v.name + " — kopia"))
        dao.allOpenings().filter { it.spaceId == v.id }.forEach { opening ->
            dao.upsertOpening(opening.copy(id = 0, spaceId = newId))
        }
        refreshOpeningArea(newId)
    }

    fun addEstimateWork(v: EstimateWorkEntity) = viewModelScope.launch { dao.upsertEstimateWork(v) }
    fun deleteEstimateWork(v: EstimateWorkEntity) = viewModelScope.launch { dao.deleteEstimateWork(v) }
    fun duplicateEstimateWork(v: EstimateWorkEntity) = viewModelScope.launch {
        dao.upsertEstimateWork(v.copy(id = 0))
    }
    fun deleteEstimateWorks(items: List<EstimateWorkEntity>) = viewModelScope.launch {
        items.forEach { dao.deleteEstimateWork(it) }
    }

    fun addPackageToEstimate(estimateId: Long, packageId: Long) = viewModelScope.launch {
        val existing = dao.allEstimateWorks().filter { it.estimateId == estimateId }.map { it.workId }.toSet()
        dao.allPackageWorks().filter { it.packageId == packageId }.sortedBy { it.position }.forEach { pw ->
            if (pw.workId !in existing) {
                dao.upsertEstimateWork(
                    EstimateWorkEntity(
                        estimateId = estimateId,
                        workId = pw.workId,
                        quantity = 1.0,
                        quantitySource = QuantitySource.MANUAL
                    )
                )
            }
        }
    }

    fun addCrewMember(v: CrewMemberEntity) = viewModelScope.launch { dao.upsertCrewMember(v) }

    fun assignCrew(v: EstimateCrewEntity) = viewModelScope.launch {
        val current = dao.allEstimateCrew().filter { it.estimateId == v.estimateId }
        if (current.any { it.crewMemberId == v.crewMemberId } || current.size < 6) {
            dao.upsertEstimateCrew(v)
        }
    }

    fun removeCrew(estimateId: Long, crewId: Long) = viewModelScope.launch {
        dao.removeCrewFromEstimate(estimateId, crewId)
    }

    fun addExtra(v: ExtraCostEntity) = viewModelScope.launch { dao.upsertExtraCost(v) }
    fun deleteExtra(v: ExtraCostEntity) = viewModelScope.launch { dao.deleteExtraCost(v) }

    fun addPhoto(v: PhotoEntity) = viewModelScope.launch { dao.upsertPhoto(v) }
    fun deletePhoto(v: PhotoEntity) = viewModelScope.launch { dao.deletePhoto(v) }

    fun saveSettings(v: AppSettingsEntity) = viewModelScope.launch { dao.upsertSettings(v) }

    fun startPcSync() {
        _pcSyncStatus.value = PcSyncServer.start(getApplication(), dao)
    }

    fun stopPcSync() {
        PcSyncServer.stop(getApplication())
        _pcSyncStatus.value = PcSyncServer.currentStatus()
    }

    fun setShoppingStatus(item: ShoppingItemEntity, status: String) = viewModelScope.launch {
        dao.upsertShopping(item.copy(status = status))
    }

    fun setToolPacked(item: ToolChecklistEntity, packed: Boolean) = viewModelScope.launch {
        dao.upsertToolChecklist(item.copy(packed = packed))
    }

    fun startWork(
        estimateId: Long,
        type: String = WorkTimeType.WORK,
        crewIds: List<Long>? = null
    ) = viewModelScope.launch {
        workTimerMutex.withLock {
            if (dao.activeWorkSessions(estimateId).isNotEmpty()) return@withLock

            val estimate = dao.getEstimate(estimateId) ?: return@withLock
            val assigned = crewIds ?: dao.allEstimateCrew()
                .filter { it.estimateId == estimateId }
                .map { it.crewMemberId }

            val workers: List<Long?> = assigned.distinct().map { it as Long? }
                .ifEmpty { listOf(null) }
            val now = System.currentTimeMillis()

            workers.forEach { crewId ->
                val sessionId = dao.upsertWorkSession(
                    WorkSessionEntity(
                        estimateId = estimateId,
                        crewMemberId = crewId,
                        type = type,
                        startAt = now,
                        createdAt = now,
                        updatedAt = now
                    )
                )
                dao.upsertWorkSessionEvent(
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
                dao.upsertEstimate(estimate.copy(status = EstimateStatus.IN_PROGRESS))
            }

            WorkTimerNotifications.show(
                context = getApplication(),
                estimateId = estimateId,
                estimateTitle = estimate.title,
                startAt = now,
                crewCount = workers.size,
                workType = type
            )
        }
    }

    fun stopWork(estimateId: Long, reason: String = WorkEndReason.STOP) = viewModelScope.launch {
        workTimerMutex.withLock {
            val active = dao.activeWorkSessions(estimateId)
            if (active.isEmpty()) {
                WorkTimerNotifications.cancel(getApplication(), estimateId)
                return@withLock
            }

            val now = System.currentTimeMillis()
            dao.stopActiveWorkSessions(estimateId, now, reason)
            val eventType = if (reason == WorkEndReason.PAUSE) WorkSessionEventType.PAUSE else WorkSessionEventType.STOP
            active.forEach { session ->
                dao.upsertWorkSessionEvent(
                    WorkSessionEventEntity(
                        estimateId = estimateId,
                        sessionId = session.id,
                        crewMemberId = session.crewMemberId,
                        eventType = eventType,
                        at = now,
                        note = WorkTimeType.label(session.type)
                    )
                )
            }
            WorkTimerNotifications.cancel(getApplication(), estimateId)
        }
    }

    suspend fun syncFulfillment(estimateId: Long) {
        val estimate = dao.getEstimate(estimateId) ?: return
        val result = EstimateCalculator.calculate(
            estimate = estimate,
            works = dao.allWorks(),
            estimateWorks = dao.allEstimateWorks(),
            materials = dao.allMaterials(),
            workMaterials = dao.allWorkMaterials(),
            tools = dao.allTools(),
            workTools = dao.allWorkTools(),
            spaces = dao.allSpaces(),
            crew = dao.allEstimateCrew(),
            extraCosts = dao.allExtraCosts()
        )

        val oldShopping = dao.allShopping().filter { it.estimateId == estimateId }.associateBy { it.materialId }
        dao.clearShopping(estimateId)
        result.materials.forEach { need ->
            dao.upsertShopping(
                ShoppingItemEntity(
                    estimateId = estimateId,
                    materialId = need.materialId,
                    requiredQty = need.quantity,
                    status = oldShopping[need.materialId]?.status ?: FulfillmentStatus.TO_BUY
                )
            )
        }

        val oldTools = dao.allToolChecklist().filter { it.estimateId == estimateId }.associateBy { it.toolId }
        dao.clearToolChecklist(estimateId)
        result.tools.forEach { need ->
            dao.upsertToolChecklist(
                ToolChecklistEntity(
                    estimateId = estimateId,
                    toolId = need.toolId,
                    qty = need.qty,
                    packed = oldTools[need.toolId]?.packed ?: false
                )
            )
        }
    }
}
