package pl.edwin.budowlanka.domain

import pl.edwin.budowlanka.data.*
import kotlin.math.ceil

data class MaterialNeed(
    val materialId: Long,
    val name: String,
    val unit: String,
    val quantity: Double,
    val unitPrice: Double,
    val cost: Double
)

data class ToolNeed(
    val toolId: Long,
    val name: String,
    val qty: Int
)

data class EstimateResult(
    val laborBase: Double = 0.0,
    val materialBase: Double = 0.0,
    val extraCosts: Double = 0.0,
    val travelCost: Double = 0.0,
    val laborSell: Double = 0.0,
    val materialSell: Double = 0.0,
    val beforeDiscount: Double = 0.0,
    val discountValue: Double = 0.0,
    val clientTotal: Double = 0.0,
    val laborHours: Double = 0.0,
    val technicalDays: Double = 0.0,
    val crewDirectCost: Double = 0.0,
    val estimatedProfitBeforeProfitShare: Double = 0.0,
    val financialMaxDays: Double = 0.0,
    val targetPerDay: Double = 0.0,
    val materials: List<MaterialNeed> = emptyList(),
    val tools: List<ToolNeed> = emptyList()
)

object EstimateCalculator {
    fun resolveLaborRate(line: EstimateWorkEntity, work: WorkEntity): Double = when (line.laborPriceMode) {
        LaborPriceMode.MIN -> work.laborRateLow.takeIf { it > 0.0 } ?: work.laborRate
        LaborPriceMode.MAX -> work.laborRateHigh.takeIf { it > 0.0 } ?: work.laborRate
        LaborPriceMode.CUSTOM -> line.laborRateOverride ?: work.laborRate
        else -> work.laborRate
    }

    fun suggestedQuantitySource(work: WorkEntity): String {
        if (work.defaultQuantitySource != QuantitySource.MANUAL) return work.defaultQuantitySource

        val text = "${work.category} ${work.name}".lowercase()
        return when {
            work.unit.lowercase().contains("szt") -> QuantitySource.PIECES
            text.contains("podłog") || text.contains("posadzk") ||
                text.contains("wylewk") || text.contains("panel") -> QuantitySource.FLOOR
            text.contains("sufit") -> QuantitySource.CEILING
            text.contains("listw") || text.contains("cokoł") || text.contains("cokół") -> QuantitySource.PERIMETER
            text.contains("ścian") || text.contains("tynk") || text.contains("gład") ||
                text.contains("malowan") || text.contains("elewac") -> QuantitySource.WALLS
            else -> QuantitySource.MANUAL
        }
    }

    fun resolveQuantity(line: EstimateWorkEntity, spaces: List<SpaceEntity>): Double {
        if (line.quantitySource == QuantitySource.MANUAL || line.quantitySource == QuantitySource.PIECES) {
            return line.quantity.coerceAtLeast(0.0)
        }

        val selectedSpaces = if (line.spaceId == null) {
            spaces
        } else {
            spaces.filter { it.id == line.spaceId }
        }

        return selectedSpaces.sumOf { space ->
            when (line.quantitySource) {
                QuantitySource.FLOOR -> space.floorArea()
                QuantitySource.WALLS -> space.wallArea()
                QuantitySource.CEILING -> space.ceilingArea()
                QuantitySource.PERIMETER -> space.perimeter()
                QuantitySource.OPENINGS -> space.openingsArea.coerceAtLeast(0.0)
                else -> 0.0
            }
        }.coerceAtLeast(0.0)
    }

    private fun materialPrice(m: MaterialEntity, tier: String): Double = when (tier) {
        MaterialTier.BUDGET -> m.priceBudget
        MaterialTier.PREMIUM -> m.pricePremium
        else -> m.priceStandard
    }

    fun calculate(
        estimate: EstimateEntity,
        works: List<WorkEntity>,
        estimateWorks: List<EstimateWorkEntity>,
        materials: List<MaterialEntity>,
        workMaterials: List<WorkMaterialEntity>,
        tools: List<ToolEntity>,
        workTools: List<WorkToolEntity>,
        spaces: List<SpaceEntity>,
        crew: List<EstimateCrewEntity>,
        extraCosts: List<ExtraCostEntity>
    ): EstimateResult {
        var laborBase = 0.0
        var laborHours = 0.0
        val materialMap = linkedMapOf<Long, Pair<Double, Double>>()
        val materialTierById = mutableMapOf<Long, String>()
        val toolQty = linkedMapOf<Long, Int>()

        val scopedLines = estimateWorks.filter { it.estimateId == estimate.id }
        val hasWorkScope = scopedLines.isNotEmpty()

        scopedLines.forEach { line ->
            val work = works.firstOrNull { it.id == line.workId } ?: return@forEach
            val qty = resolveQuantity(line, spaces.filter { it.estimateId == estimate.id })
            val laborRate = resolveLaborRate(line, work)
            laborBase += qty * laborRate
            laborHours += qty * work.laborHoursPerUnit

            if (estimate.includeMaterials) {
                val waste = (line.wastePctOverride ?: work.defaultWastePct).coerceAtLeast(0.0)
                workMaterials.filter { it.workId == work.id }.forEach { wm ->
                    val mat = materials.firstOrNull { it.id == wm.materialId } ?: return@forEach
                    val required = qty * wm.qtyPerWorkUnit * (1.0 + waste / 100.0)
                    val price = materialPrice(mat, line.materialTier)
                    val prev = materialMap[mat.id] ?: (0.0 to price)
                    materialMap[mat.id] = (prev.first + required) to price
                    materialTierById[mat.id] = line.materialTier
                }
            }

            workTools.filter { it.workId == work.id }.forEach { wt ->
                toolQty[wt.toolId] = maxOf(toolQty[wt.toolId] ?: 0, wt.qty)
            }
        }

        val materialNeeds = materialMap.mapNotNull { (id, qp) ->
            val m = materials.firstOrNull { it.id == id } ?: return@mapNotNull null
            MaterialNeed(id, m.name, m.unit, qp.first, qp.second, qp.first * qp.second)
        }
        val materialBase = materialNeeds.sumOf { it.cost }
        val extras = extraCosts.filter { it.estimateId == estimate.id }.sumOf { it.amount }
        val travel = estimate.oneWayKm.coerceAtLeast(0.0) * 2.0 *
            estimate.travelDays.coerceAtLeast(0) * estimate.kmRate.coerceAtLeast(0.0) +
            estimate.fixedTravelFee.coerceAtLeast(0.0)

        val laborSell = laborBase * (1.0 + estimate.marginLaborPct / 100.0)
        val materialSell = materialBase * (1.0 + estimate.marginMaterialPct / 100.0)
        // Dopóki nie ma choć jednej roboty, wycena klienta pozostaje zerowa.
        // Dojazd i koszty dodatkowe są zapamiętane, ale nie tworzą samodzielnie wyceny.
        val preOverall = if (hasWorkScope) laborSell + materialSell + extras + travel else 0.0
        val beforeDiscount = preOverall * (1.0 + estimate.marginOverallPct / 100.0)
        val discountValue = beforeDiscount * estimate.discountPct.coerceIn(0.0, 100.0) / 100.0
        val clientTotal = (beforeDiscount - discountValue).coerceAtLeast(0.0)

        val crewCount = crew.filter { it.estimateId == estimate.id }.size.coerceIn(1, 6)
        val efficiency = when (crewCount) {
            1 -> 1.0
            2 -> 1.75
            3 -> 2.35
            4 -> 2.8
            5 -> 3.15
            else -> 3.4
        }
        val hoursPerDay = estimate.hoursDaySnapshot.coerceAtLeast(1.0)
        val technicalDays = if (laborHours <= 0.0) 0.0 else laborHours / (hoursPerDay * efficiency)

        var directCrew = 0.0
        if (hasWorkScope) {
            crew.filter { it.estimateId == estimate.id }.forEach { c ->
                when (c.payMode) {
                    PayMode.HOURLY -> directCrew += laborHours * (c.workSharePct / 100.0) * c.rate
                    PayMode.DAILY -> directCrew += technicalDays * c.rate
                    PayMode.FLAT -> directCrew += c.rate
                }
            }
        }

        val estimatedProfit = if (hasWorkScope) {
            (clientTotal - materialBase - extras - travel - directCrew).coerceAtLeast(0.0)
        } else 0.0
        val targetPerDay = if (estimate.workDaysMonthSnapshot > 0)
            estimate.monthlyTargetSnapshot / estimate.workDaysMonthSnapshot.toDouble() else 0.0
        val financialDays = if (hasWorkScope && targetPerDay > 0.0) estimatedProfit / targetPerDay else 0.0

        val toolNeeds = toolQty.mapNotNull { (id, qty) ->
            tools.firstOrNull { it.id == id }?.let { ToolNeed(id, it.name, qty) }
        }

        return EstimateResult(
            laborBase = laborBase,
            materialBase = materialBase,
            extraCosts = extras,
            travelCost = travel,
            laborSell = laborSell,
            materialSell = materialSell,
            beforeDiscount = beforeDiscount,
            discountValue = discountValue,
            clientTotal = clientTotal,
            laborHours = laborHours,
            technicalDays = technicalDays,
            crewDirectCost = directCrew,
            estimatedProfitBeforeProfitShare = estimatedProfit,
            financialMaxDays = financialDays,
            targetPerDay = targetPerDay,
            materials = materialNeeds,
            tools = toolNeeds
        )
    }
}
