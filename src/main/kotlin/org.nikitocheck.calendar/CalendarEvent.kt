package org.nikitocheck.calendar

import java.time.LocalDateTime

data class CalendarEvent(
    val title: String,
    val priority: Int,
    val startedAt: LocalDateTime,
    val finishAt: LocalDateTime,
)