package com.ourhouse.missions

import com.ourhouse.missions.data.model.Task
import com.ourhouse.missions.data.model.Urgency
import com.ourhouse.missions.notifications.notificationCandidateFor
import com.ourhouse.missions.ui.screens.home.HomeSort
import com.ourhouse.missions.ui.screens.home.comparatorFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import java.util.Date
import org.junit.Test

class TaskLogicTest {

    private val me = "me"
    private val partner = "partner"

    private fun assignedToMe(at: Long = 1_000, by: String? = partner) = Task(
        id = "t1", title = "לקנות חלב", assigneeId = me,
        assignedBy = by, assignedByName = "רותם", assignedAt = Date(at), createdBy = partner
    )

    // ---- notifications -------------------------------------------------------------

    @Test fun `task a partner assigned to me notifies`() {
        val c = notificationCandidateFor(assignedToMe(), me)
        assertNotNull(c)
        assertEquals("רותם הוסיף/ה לך משימה", c!!.title)
        assertEquals("\"לקנות חלב\"", c.body)
    }

    @Test fun `task I assigned to myself does not notify`() {
        assertNull(notificationCandidateFor(assignedToMe(by = me), me))
    }

    @Test fun `task assigned to the partner does not notify me`() {
        assertNull(notificationCandidateFor(assignedToMe().copy(assigneeId = partner), me))
    }

    @Test fun `completed task does not notify`() {
        assertNull(notificationCandidateFor(assignedToMe().copy(completed = true), me))
    }

    @Test fun `task without an assignment timestamp does not notify`() {
        assertNull(notificationCandidateFor(assignedToMe().copy(assignedAt = null), me))
    }

    @Test fun `handoff notifies with the message and a different key than the original assignment`() {
        val original = notificationCandidateFor(assignedToMe(at = 1_000), me)!!
        val handoff = assignedToMe(at = 5_000).copy(lastHandoffAt = Date(5_000), lastHandoffMessage = "אני בעבודה")
        val c = notificationCandidateFor(handoff, me)!!
        assertEquals("רותם העביר/ה אליך אחריות", c.title)
        assertEquals("\"לקנות חלב\" — אני בעבודה", c.body)
        assertNotEquals(original.key, c.key)
    }

    @Test fun `reassigning to me again produces a new event key`() {
        val first = notificationCandidateFor(assignedToMe(at = 1_000), me)!!
        val second = notificationCandidateFor(assignedToMe(at = 2_000), me)!!
        assertNotEquals(first.key, second.key)
    }

    @Test fun `same assignment always produces the same key`() {
        assertEquals(notificationCandidateFor(assignedToMe(), me)!!.key, notificationCandidateFor(assignedToMe(), me)!!.key)
    }

    // ---- sorting -------------------------------------------------------------------

    private fun task(id: String, urgency: Urgency, due: Long?, created: Long = 0) =
        Task(id = id, urgency = urgency.name, dueAt = due?.let { Date(it) }, createdAt = Date(created))

    private val sample = listOf(
        task("noDateUrgent", Urgency.URGENT, null),
        task("laterLow", Urgency.LOW, 300),
        task("soonMedium", Urgency.MEDIUM, 100),
        task("soonUrgent", Urgency.URGENT, 200)
    )

    @Test fun `sort by date puts soonest first and undated last`() {
        val ids = sample.sortedWith(comparatorFor(HomeSort.DATE)).map { it.id }
        assertEquals(listOf("soonMedium", "soonUrgent", "laterLow", "noDateUrgent"), ids)
    }

    @Test fun `sort by urgency puts urgent first then soonest due`() {
        val ids = sample.sortedWith(comparatorFor(HomeSort.URGENCY)).map { it.id }
        assertEquals(listOf("soonUrgent", "noDateUrgent", "soonMedium", "laterLow"), ids)
    }

    @Test fun `ties fall back to newest first`() {
        val older = task("older", Urgency.MEDIUM, null, created = 1)
        val newer = task("newer", Urgency.MEDIUM, null, created = 2)
        assertTrue(listOf(older, newer).sortedWith(comparatorFor(HomeSort.DATE)).map { it.id } == listOf("newer", "older"))
    }
}
