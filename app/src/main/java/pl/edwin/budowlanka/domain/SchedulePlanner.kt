package pl.edwin.budowlanka.domain

import pl.edwin.budowlanka.data.EstimateEntity
import pl.edwin.budowlanka.data.EstimateStatus
import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.math.ceil

object SchedulePlanner {
    private fun nextWorkday(d: LocalDate): LocalDate {
        var x = d
        while (x.dayOfWeek == DayOfWeek.SATURDAY || x.dayOfWeek == DayOfWeek.SUNDAY) {
            x = x.plusDays(1)
        }
        return x
    }

    private fun endForWorkingDays(start: LocalDate, days: Int): LocalDate {
        var d = nextWorkday(start)
        var left = days.coerceAtLeast(1) - 1
        while (left > 0) {
            d = nextWorkday(d.plusDays(1))
            left--
        }
        return d
    }

    fun propose(
        estimateId: Long,
        technicalDays: Double,
        all: List<EstimateEntity>,
        from: LocalDate = LocalDate.now()
    ): Pair<String, String> {
        val duration = ceil(technicalDays.coerceAtLeast(1.0)).toInt()
        val blockingStatuses = setOf(
            EstimateStatus.ACCEPTED,
            EstimateStatus.IN_PROGRESS
        )
        val occupied = all.filter {
            it.id != estimateId &&
                it.status in blockingStatuses &&
                it.startDate.isNotBlank()
        }.mapNotNull {
            runCatching {
                val s = LocalDate.parse(it.startDate)
                val e = LocalDate.parse(it.endDate.ifBlank { it.startDate })
                s to e
            }.getOrNull()
        }

        var start = nextWorkday(from)
        repeat(366) {
            val end = endForWorkingDays(start, duration)
            val collision = occupied.firstOrNull { (s, e) -> start <= e && s <= end }
            if (collision == null) return start.toString() to end.toString()
            start = nextWorkday(collision.second.plusDays(1))
        }
        val end = endForWorkingDays(start, duration)
        return start.toString() to end.toString()
    }
}
