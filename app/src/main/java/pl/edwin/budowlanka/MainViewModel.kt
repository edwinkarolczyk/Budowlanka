package pl.edwin.budowlanka

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pl.edwin.budowlanka.data.*
import pl.edwin.budowlanka.domain.EstimateCalculator

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
    val estimateWorks = dao.observeEstimateWorks().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val crewMembers = dao.observeCrewMembers().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val estimateCrew = dao.observeEstimateCrew().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val extraCosts = dao.observeExtraCosts().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val photos = dao.observePhotos().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val shopping = dao.observeShopping().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val toolChecklist = dao.observeToolChecklist().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val priceHistory = dao.observePriceHistory().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val settings = dao.observeSettings().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

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
    fun addSite(v: SiteEntity) = viewModelScope.launch { dao.upsertSite(v) }

    fun addSpace(v: SpaceEntity) = viewModelScope.launch { dao.upsertSpace(v) }
    fun deleteSpace(v: SpaceEntity) = viewModelScope.launch { dao.deleteSpace(v) }
    fun duplicateSpace(v: SpaceEntity) = viewModelScope.launch {
        dao.upsertSpace(v.copy(id = 0, name = v.name + " — kopia"))
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

    fun setShoppingStatus(item: ShoppingItemEntity, status: String) = viewModelScope.launch {
        dao.upsertShopping(item.copy(status = status))
    }

    fun setToolPacked(item: ToolChecklistEntity, packed: Boolean) = viewModelScope.launch {
        dao.upsertToolChecklist(item.copy(packed = packed))
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
