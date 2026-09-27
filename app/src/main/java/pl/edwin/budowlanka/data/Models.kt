package pl.edwin.budowlanka.data

import androidx.room.Entity
import androidx.room.PrimaryKey

object EstimateStatus {
    const val DRAFT = "SZKIC"
    const val SENT = "WYSLANA"
    const val ACCEPTED = "ZAAKCEPTOWANA"
    const val IN_PROGRESS = "REALIZACJA"
    const val DONE = "ZAKONCZONA"
    const val REJECTED = "ODRZUCONA"
    val all = listOf(DRAFT, SENT, ACCEPTED, IN_PROGRESS, DONE, REJECTED)
}

object QuantitySource {
    const val MANUAL = "MANUAL"
    const val FLOOR = "PODLOGA"
    const val WALLS = "SCIANY"
    const val CEILING = "SUFIT"
    const val PERIMETER = "OBWOD"
    const val OPENINGS = "OTWORY"
    const val PIECES = "SZTUKI"

    val all = listOf(MANUAL, FLOOR, WALLS, CEILING, PERIMETER, OPENINGS, PIECES)

    fun label(value: String): String = when (value) {
        FLOOR -> "Podłoga [m²]"
        WALLS -> "Ściany [m²]"
        CEILING -> "Sufit [m²]"
        PERIMETER -> "Obwód [mb]"
        OPENINGS -> "Otwory [m²]"
        PIECES -> "Sztuki"
        else -> "Ręcznie"
    }
}

object MaterialTier {
    const val BUDGET = "TANI"
    const val STANDARD = "STANDARD"
    const val PREMIUM = "PREMIUM"
    val all = listOf(BUDGET, STANDARD, PREMIUM)
}

object LaborPriceMode {
    const val CATALOG = "KATALOG"
    const val MIN = "MIN"
    const val MAX = "MAX"
    const val CUSTOM = "WLASNA"
    val all = listOf(CATALOG, MIN, MAX, CUSTOM)
}

object PayMode {
    const val PROFIT_PERCENT = "PROCENT_ZYSKU"
    const val HOURLY = "GODZINOWO"
    const val DAILY = "DZIENNIE"
    const val FLAT = "STALA_KWOTA"
    val all = listOf(PROFIT_PERCENT, HOURLY, DAILY, FLAT)
}

object OpeningType {
    const val WINDOW = "OKNO"
    const val DOOR = "DRZWI"
    const val OTHER = "INNY"
    val all = listOf(WINDOW, DOOR, OTHER)
}

object FulfillmentStatus {
    const val HAVE = "MAM"
    const val TO_BUY = "DO_KUPIENIA"
    const val ORDERED = "ZAMOWIONE"
    const val BOUGHT = "KUPIONE"
    const val DELIVERED = "DOSTARCZONE"
    val all = listOf(HAVE, TO_BUY, ORDERED, BOUGHT, DELIVERED)
}

@Entity(tableName = "works")
data class WorkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String = "Ogólne",
    val unit: String = "m²",
    val laborRate: Double = 0.0,
    val laborRateLow: Double = 0.0,
    val laborRateHigh: Double = 0.0,
    val laborHoursPerUnit: Double = 0.0,
    val defaultWastePct: Double = 10.0,
    val priceRegion: String = "",
    val priceYear: Int = 0,
    val priceSource: String = "",
    val includesMaterial: Boolean = false,
    val isFavorite: Boolean = false,
    val isUserDefined: Boolean = false,
    val active: Boolean = true,
    val defaultQuantitySource: String = QuantitySource.MANUAL
)

@Entity(tableName = "materials")
data class MaterialEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val unit: String = "szt.",
    val manufacturer: String = "",
    val priceBudget: Double = 0.0,
    val priceStandard: Double = 0.0,
    val pricePremium: Double = 0.0,
    val stockQty: Double = 0.0,
    val active: Boolean = true
)

@Entity(tableName = "work_materials", primaryKeys = ["workId", "materialId"])
data class WorkMaterialEntity(
    val workId: Long,
    val materialId: Long,
    val qtyPerWorkUnit: Double
)

@Entity(tableName = "tools")
data class ToolEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val code: String = "",
    val notes: String = "",
    val active: Boolean = true
)

@Entity(tableName = "work_tools", primaryKeys = ["workId", "toolId"])
data class WorkToolEntity(
    val workId: Long,
    val toolId: Long,
    val qty: Int = 1
)

@Entity(tableName = "packages")
data class PackageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = ""
)

@Entity(tableName = "package_works", primaryKeys = ["packageId", "workId"])
data class PackageWorkEntity(
    val packageId: Long,
    val workId: Long,
    val position: Int = 0
)

@Entity(tableName = "clients")
data class ClientEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = "",
    val email: String = "",
    val notes: String = "",
    val loyaltyDiscountPct: Double = 0.0
)

@Entity(tableName = "sites")
data class SiteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val clientId: Long,
    val name: String = "",
    val address: String = "",
    val notes: String = ""
)

@Entity(tableName = "estimates")
data class EstimateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val clientId: Long? = null,
    val siteId: Long? = null,
    val status: String = EstimateStatus.DRAFT,
    val createdAt: Long = System.currentTimeMillis(),
    val startDate: String = "",
    val endDate: String = "",
    val includeMaterials: Boolean = true,
    val marginLaborPct: Double = 0.0,
    val marginMaterialPct: Double = 0.0,
    val marginOverallPct: Double = 0.0,
    val discountPct: Double = 0.0,
    val oneWayKm: Double = 0.0,
    val travelDays: Int = 0,
    val kmRate: Double = 0.0,
    val fixedTravelFee: Double = 0.0,
    val monthlyTargetSnapshot: Double = 0.0,
    val workDaysMonthSnapshot: Int = 20,
    val hoursDaySnapshot: Double = 8.0,
    val notes: String = "",
    val signatureData: String = ""
)

@Entity(tableName = "spaces")
data class SpaceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val estimateId: Long,
    val level: String = "",
    val name: String,
    val length: Double = 0.0,
    val width: Double = 0.0,
    val height: Double = 2.6,
    val openingsArea: Double = 0.0
) {
    fun floorArea(): Double = (length * width).coerceAtLeast(0.0)
    fun ceilingArea(): Double = floorArea()
    fun perimeter(): Double = (2.0 * (length + width)).coerceAtLeast(0.0)
    fun wallArea(): Double = (perimeter() * height - openingsArea).coerceAtLeast(0.0)
}

@Entity(tableName = "openings")
data class OpeningEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val spaceId: Long,
    val type: String = OpeningType.WINDOW,
    val name: String = "",
    val width: Double = 0.0,
    val height: Double = 0.0,
    val quantity: Int = 1
) {
    fun area(): Double = (width.coerceAtLeast(0.0) * height.coerceAtLeast(0.0) * quantity.coerceAtLeast(1))
}

@Entity(tableName = "estimate_works")
data class EstimateWorkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val estimateId: Long,
    val workId: Long,
    val spaceId: Long? = null,
    val quantity: Double = 0.0,
    val quantitySource: String = QuantitySource.MANUAL,
    val materialTier: String = MaterialTier.STANDARD,
    val wastePctOverride: Double? = null,
    val laborRateOverride: Double? = null,
    val laborPriceMode: String = LaborPriceMode.CATALOG
)

@Entity(tableName = "crew_members")
data class CrewMemberEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val defaultPayMode: String = PayMode.PROFIT_PERCENT,
    val defaultRate: Double = 0.0,
    val active: Boolean = true
)

@Entity(tableName = "estimate_crew", primaryKeys = ["estimateId", "crewMemberId"])
data class EstimateCrewEntity(
    val estimateId: Long,
    val crewMemberId: Long,
    val workSharePct: Double = 0.0,
    val profitSharePct: Double = 0.0,
    val payMode: String = PayMode.PROFIT_PERCENT,
    val rate: Double = 0.0
)

@Entity(tableName = "extra_costs")
data class ExtraCostEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val estimateId: Long,
    val category: String = "Inne",
    val description: String,
    val amount: Double
)

@Entity(tableName = "photos")
data class PhotoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val estimateId: Long,
    val spaceId: Long? = null,
    val estimateWorkId: Long? = null,
    val uri: String,
    val caption: String = ""
)

@Entity(tableName = "material_price_history")
data class MaterialPriceHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val materialId: Long,
    val changedAt: Long = System.currentTimeMillis(),
    val priceBudget: Double,
    val priceStandard: Double,
    val pricePremium: Double
)

@Entity(tableName = "shopping_items", primaryKeys = ["estimateId", "materialId"])
data class ShoppingItemEntity(
    val estimateId: Long,
    val materialId: Long,
    val requiredQty: Double,
    val status: String = FulfillmentStatus.TO_BUY
)

@Entity(tableName = "tool_checklist", primaryKeys = ["estimateId", "toolId"])
data class ToolChecklistEntity(
    val estimateId: Long,
    val toolId: Long,
    val qty: Int = 1,
    val packed: Boolean = false
)

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val companyName: String = "",
    val nip: String = "",
    val address: String = "",
    val phone: String = "",
    val email: String = "",
    val logoUri: String = "",
    val monthlyTarget: Double = 18000.0,
    val workDaysMonth: Int = 20,
    val hoursPerDay: Double = 8.0,
    val defaultKmRate: Double = 1.15,
    val defaultFixedTravelFee: Double = 0.0
)
