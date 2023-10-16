package org.nikitocheck.calendar

import org.nikitocheck.calendar.OverlapRemover.StampType.FINISH
import org.nikitocheck.calendar.OverlapRemover.StampType.START
import java.time.LocalDateTime

class OverlapRemover {

    /**
     * squashes list of overlapping events into list of events without overlap
     */
    enum class StampType { START, FINISH }
    data class Stamp(val event: CalendarEvent, val ts: LocalDateTime, val type: StampType)

    fun removeOverlap(events: List<CalendarEvent>): List<CalendarEvent> {

        // O(n)
        val sortedStamps = splitIntoOrderedStamps(events)

        // current active events
        val active = sortedSetOf(compareByDescending<CalendarEvent> { it.priority }.thenByDescending { it.startedAt })

        val stampsWithoutOverlap = mutableListOf<Stamp>()

        for (current in sortedStamps) {
            if (current.type == START) {
                active.add(current.event)
            }
            /**
             * add stamp if it belongs to the most recent event
             */
            if (current.event.priority >= active.first().priority && active.first() == current.event) {
                stampsWithoutOverlap.add(current)
            }

            if (current.type == FINISH) {
                active.remove(current.event)
            }

        }

        printDebug("===== stamps without overlap=====", stampsWithoutOverlap)

        val eventsWithoutOverlap = mutableListOf<CalendarEvent>()
        for (i in 0 until stampsWithoutOverlap.lastIndex) {
            val currentStamp = stampsWithoutOverlap[i]
            val nextStamp = stampsWithoutOverlap[i + 1]

            when {
                (currentStamp.type == START && nextStamp.type == FINISH && currentStamp.event == nextStamp.event) -> {
                    eventsWithoutOverlap.add(
                        CalendarEvent(
                            title = currentStamp.event.title,
                            priority = currentStamp.event.priority,
                            startedAt = currentStamp.ts,
                            finishAt = nextStamp.ts
                        )
                    )
                }
                (currentStamp.type == FINISH && nextStamp.type == FINISH && currentStamp.event != nextStamp.event) -> {
                    eventsWithoutOverlap.add(
                        CalendarEvent(
                            title = nextStamp.event.title,
                            priority = nextStamp.event.priority,
                            startedAt = currentStamp.ts,
                            finishAt = nextStamp.ts
                        )
                    )
                }
                (currentStamp.type == START && nextStamp.type == START && currentStamp.event != nextStamp.event) -> {
                    eventsWithoutOverlap.add(
                        CalendarEvent(
                            title = currentStamp.event.title,
                            priority = currentStamp.event.priority,
                            startedAt = currentStamp.ts,
                            finishAt = nextStamp.ts
                        )
                    )
                }
            }

        }

        return eventsWithoutOverlap
    }

    private fun splitIntoOrderedStamps(events: List<CalendarEvent>): List<Stamp> {
        return buildList {
            for (event in events) {
                add(Stamp(event, event.startedAt, START))
                add(Stamp(event, event.finishAt, FINISH))
            }
        }.sortedWith(compareBy<Stamp> { it.ts }.thenBy { it.event.startedAt })
            .also { items -> printDebug("===== stamps with overlap =====", items) }
    }

    private fun printDebug(title: String, items: List<Stamp>) {
        println()
        println(title)
        items.map { "${it.ts} ${it.event.title} ${it.type}" }.forEach(::println)
    }
}