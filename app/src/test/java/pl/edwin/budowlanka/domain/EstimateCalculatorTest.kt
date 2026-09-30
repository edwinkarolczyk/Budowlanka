package pl.edwin.budowlanka.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import pl.edwin.budowlanka.data.EstimateEntity
import pl.edwin.budowlanka.data.EstimateWorkEntity
import pl.edwin.budowlanka.data.ExtraCostEntity
import pl.edwin.budowlanka.data.QuantitySource
import pl.edwin.budowlanka.data.SpaceEntity
import pl.edwin.budowlanka.data.WorkEntity

class EstimateCalculatorTest {

    @Test
    fun quoteWithoutWorkScopeStaysZeroEvenWithTravelAndExtras() {
        val estimate = EstimateEntity(
            id = 1,
            title = "Pusta wycena",
            oneWayKm = 20.0,
            travelDays = 2,
            kmRate = 1.5,
            fixedTravelFee = 50.0
        )

        val result = EstimateCalculator.calculate(
            estimate = estimate,
            works = emptyList(),
            estimateWorks = emptyList(),
            materials = emptyList(),
            workMaterials = emptyList(),
            tools = emptyList(),
            workTools = emptyList(),
            spaces = emptyList(),
            crew = emptyList(),
            extraCosts = listOf(
                ExtraCostEntity(
                    id = 1,
                    estimateId = estimate.id,
                    description = "Transport",
                    amount = 100.0
                )
            )
        )

        assertEquals(0.0, result.clientTotal, 0.0001)
        assertEquals(100.0, result.extraCosts, 0.0001)
        assertEquals(170.0, result.travelCost, 0.0001)
    }

    @Test
    fun wallQuantityDeductsOpeningsAndUsesSelectedSpaceOnly() {
        val roomA = SpaceEntity(
            id = 10,
            estimateId = 1,
            name = "Salon",
            length = 5.0,
            width = 4.0,
            height = 2.5,
            openingsArea = 3.0
        )
        val roomB = SpaceEntity(
            id = 11,
            estimateId = 1,
            name = "Kuchnia",
            length = 3.0,
            width = 3.0,
            height = 2.5,
            openingsArea = 1.0
        )
        val line = EstimateWorkEntity(
            id = 20,
            estimateId = 1,
            workId = 30,
            spaceId = roomA.id,
            quantitySource = QuantitySource.WALLS
        )

        val quantity = EstimateCalculator.resolveQuantity(line, listOf(roomA, roomB))

        // 2 * (5 + 4) * 2.5 - 3 = 42 m2
        assertEquals(42.0, quantity, 0.0001)
    }

    @Test
    fun workScopeProducesLaborValueAndHours() {
        val estimate = EstimateEntity(id = 1, title = "Malowanie")
        val work = WorkEntity(
            id = 7,
            name = "Malowanie ścian",
            unit = "m²",
            laborRate = 25.0,
            laborHoursPerUnit = 0.2
        )
        val line = EstimateWorkEntity(
            id = 8,
            estimateId = estimate.id,
            workId = work.id,
            quantity = 40.0,
            quantitySource = QuantitySource.MANUAL
        )

        val result = EstimateCalculator.calculate(
            estimate = estimate,
            works = listOf(work),
            estimateWorks = listOf(line),
            materials = emptyList(),
            workMaterials = emptyList(),
            tools = emptyList(),
            workTools = emptyList(),
            spaces = emptyList(),
            crew = emptyList(),
            extraCosts = emptyList()
        )

        assertEquals(1000.0, result.laborBase, 0.0001)
        assertEquals(1000.0, result.clientTotal, 0.0001)
        assertEquals(8.0, result.laborHours, 0.0001)
    }
}
