package org.nikitocheck.calendar

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

const val HIGH_PRIORITY = 10
const val MEDIUM_PRIORITY = 5
const val LOW_PRIORITY = 0

internal class OverlapRemoverTest {


    private val overlapRemover = OverlapRemover()

    private val dailyStandUp = CalendarEvent(
        title = "daily stand up",
        priority = HIGH_PRIORITY,
        startedAt = LocalDateTime.of(2023, 10, 16, 10, 0, 0),
        finishAt = LocalDateTime.of(2023, 10, 16, 10, 30, 0),
    )

    private val coffeeBreak = CalendarEvent(
        title = "accidental coffee break",
        priority = LOW_PRIORITY,
        startedAt = LocalDateTime.of(2023, 10, 16, 10, 15, 0),
        finishAt = LocalDateTime.of(2023, 10, 16, 11, 0, 0),
    )

    private val backlogGrooming = CalendarEvent(
        title = "backlog grooming",
        priority = MEDIUM_PRIORITY,
        startedAt = LocalDateTime.of(2023, 10, 16, 10, 15, 0),
        finishAt = LocalDateTime.of(2023, 10, 16, 12, 0, 0),
    )

    private val productionAccident = CalendarEvent(
        title = "accident in prod",
        priority = HIGH_PRIORITY,
        startedAt = LocalDateTime.of(2023, 10, 16, 10, 15, 0),
        finishAt = LocalDateTime.of(2023, 10, 16, 12, 0, 0),
    )
    private val allDayLongMeeting  = CalendarEvent(
        title = "all day meeting",
        priority = HIGH_PRIORITY,
        startedAt = LocalDateTime.of(2023, 10, 16, 9, 0, 0),
        finishAt = LocalDateTime.of(2023, 10, 16, 18, 0, 0),
    )

    private val demo = CalendarEvent(
        title = "demo",
        priority = HIGH_PRIORITY,
        startedAt = LocalDateTime.of(2023, 10, 16, 12, 0, 0),
        finishAt = LocalDateTime.of(2023, 10, 16, 12, 30, 0),
    )

    /************************************************************
     * before:
     * --------------------------
     *  |----STAND UP-----|
     * --------------------------
     * after :
     * ---------------------------*
     *  |----STAND UP-----|
     * --------------------------
     *************************************************************
     */
    @Test
    fun `should process single event`() {
        val events = listOf(dailyStandUp)
        val result = overlapRemover.removeOverlap(events)

        Assertions.assertEquals(dailyStandUp, result.single())
    }



    /************************************************************
     * before:
     * --------------------------
     *  |----STAND UP-----|
     *                           |------COFFEE-------|
     * --------------------------
     * after :
     * ---------------------------*
     *  |----STAND UP-----|      |------COFFEE-------|
     * --------------------------
     *************************************************************
     */
    @Test
    fun `should process non overlapping event`() {
        val events = listOf(dailyStandUp, demo)
        val result = overlapRemover.removeOverlap(events)

        Assertions.assertEquals(2, result.size)
        Assertions.assertEquals(dailyStandUp, result[0])
        Assertions.assertEquals(demo, result[1])
    }

    /************************************************************
     * before:
     * --------------------------
     *  |----STAND UP-----|
     *              |------COFFEE-------|
     * --------------------------
     * after :
     * ---------------------------*
     *  |----STAND UP-----|-COFFEE------|
     * --------------------------
     *************************************************************
     */
    @Test
    fun `should process 2 overlapping events with priority`() {
        val events = listOf(dailyStandUp, coffeeBreak)
        val result = overlapRemover.removeOverlap(events)

        Assertions.assertEquals(2, result.size)
        Assertions.assertEquals(dailyStandUp, result[0])
        Assertions.assertEquals(coffeeBreak.copy(startedAt = dailyStandUp.finishAt), result[1])
    }
    /************************************************************
     * before:
     * --------------------------
     *  |----STAND UP-----|
     *              |------COFFEE-------|
     * --------------------------
     * after :
     * ---------------------------*
     *  |----STAND UP-----|-COFFEE------|
     * --------------------------
     *************************************************************
     */
    @Test
    fun `event with higher priority should overlap event with lower priority `() {
        val events = listOf(dailyStandUp, coffeeBreak, backlogGrooming, productionAccident)
        val result = overlapRemover.removeOverlap(events)

        Assertions.assertEquals(2, result.size)
        Assertions.assertEquals(dailyStandUp.copy(finishAt = productionAccident.startedAt), result[0])
        Assertions.assertEquals(productionAccident, result[1])
    }

    /************************************************************
     * before:
     * -----------------------------------------------------------------------
     *                          |----STAND UP-----|
     * |--------------------------------meeting----------------------------|
     * --------------------------
     * after :
     * ---------------------------*
     * |---------meeting--------|----STAND UP-----|------meeting----------|
     * --------------------------
     *************************************************************
     */
    @Test
    fun `should process enclosing event with same priority `() {
        val events = listOf(dailyStandUp, allDayLongMeeting)
        val result = overlapRemover.removeOverlap(events)

        Assertions.assertEquals(3, result.size)
        Assertions.assertEquals(allDayLongMeeting.copy(finishAt = dailyStandUp.startedAt), result[0])
        Assertions.assertEquals(dailyStandUp, result[1])
        Assertions.assertEquals(allDayLongMeeting.copy(startedAt = dailyStandUp.finishAt), result[2])
    }


    /************************************************************
     *
     * events
     *
     * before:
     * ------------------------------------------------------------
     * |--meet1-------------|
     *              |---meet2-----------------|
     *                                   |---meet3-----------|
     * -------------------------------------------------------------
     * after :
     * ---------------------------*
     * |--meet1-----|---meet2------------|---meet3-----------|
     * --------------------------
     *************************************************************
     */
    @Test
    fun `start of event with same priority should overlap previous event`() {
        val meet1 = CalendarEvent(
            title = "meet1",
            priority =  HIGH_PRIORITY,
            startedAt = LocalDateTime.of(2023, 10, 16, 10,0),
            finishAt = LocalDateTime.of(2023, 10, 16, 14,0),
        )
        val meet2 = CalendarEvent(
            title = "meet2",
            priority =  HIGH_PRIORITY,
            startedAt = LocalDateTime.of(2023, 10, 16, 12,0),
            finishAt = LocalDateTime.of(2023, 10, 16, 16,0),
        )
        val meet3 = CalendarEvent(
            title = "meet2",
            priority =  HIGH_PRIORITY,
            startedAt = LocalDateTime.of(2023, 10, 16, 15,0),
            finishAt = LocalDateTime.of(2023, 10, 16, 18,0),
        )
        val events = listOf(meet1, meet2, meet3)
        val result = overlapRemover.removeOverlap(events)

        Assertions.assertEquals(3, result.size)
        Assertions.assertEquals(meet1.copy(finishAt = meet2.startedAt), result[0])
        Assertions.assertEquals(meet2.copy(finishAt = meet3.startedAt), result[1])
        Assertions.assertEquals(meet3, result[2])
    }


}