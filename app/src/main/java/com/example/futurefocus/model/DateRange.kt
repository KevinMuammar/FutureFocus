package com.example.futurefocus.model

data class DateRange(
    val startMillis: Long,
    val endMillis: Long
) {
    val dayCount: Int
        get() = ((endMillis - startMillis) / DAY_MILLIS).toInt().coerceAtLeast(1)

    companion object {
        const val DAY_MILLIS = 86_400_000L
        val WEEK: Long = 7L * DAY_MILLIS
        val MONTH: Long = 30L * DAY_MILLIS
        val YEAR: Long = 365L * DAY_MILLIS

        fun lastDays(days: Int): DateRange {
            val end = System.currentTimeMillis()
            val start = end - days * DAY_MILLIS
            return DateRange(start, end)
        }
    }
}

data class DailyBreakdown(
    val dateKey: String,
    val totalMinutes: Int,
    val sessionCount: Int
)

data class PeriodStats(
    val totalFocusMinutes: Int = 0,
    val totalSessions: Int = 0,
    val successfulSessions: Int = 0,
    val failedSessions: Int = 0,
    val dailyBreakdown: List<DailyBreakdown> = emptyList(),
    val completedGoalIds: List<String> = emptyList()
) {
    val totalHours: Float get() = totalFocusMinutes / 60f
    val dailyAverageMinutes: Int
        get() = if (dailyBreakdown.isNotEmpty())
            dailyBreakdown.sumOf { it.totalMinutes } / dailyBreakdown.size else 0
    val weeklyAverageMinutes: Int
        get() = dailyAverageMinutes * 7
    val monthlyAverageMinutes: Int
        get() = dailyAverageMinutes * 30
    val successRate: Float
        get() = if (totalSessions > 0) successfulSessions.toFloat() / totalSessions.toFloat() else 0f
}
