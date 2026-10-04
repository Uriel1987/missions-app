package com.ourhouse.missions.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

enum class Urgency { URGENT, MEDIUM, LOW }

/** Built-in categories the app ships with; households can add their own free-text ones too. */
enum class Category(val key: String) {
    HOME("home"),
    SHOPPING("shopping"),
    KIDS("kids"),
    FINANCE("finance");

    companion object {
        fun fromKey(key: String?): Category? = entries.firstOrNull { it.key == key }
    }
}

/**
 * A single mission / to-do, stored at households/{householdId}/tasks/{taskId}.
 *
 * [assigneeId] and [dueAt] are both intentionally nullable — the assignee can be left
 * blank ("no one yet") and the due date/time is optional per the product spec.
 */
data class Task(
    @DocumentId
    val id: String = "",

    val title: String = "",
    val note: String = "",

    /** Free-text category key, e.g. "home" / "shopping" / a custom one the couple typed in. */
    val category: String? = null,

    @get:PropertyName("urgency")
    @set:PropertyName("urgency")
    var urgency: String = Urgency.MEDIUM.name,

    /** null = unassigned ("ללא שיוך") */
    val assigneeId: String? = null,
    val assigneeName: String? = null,

    /** null = no fixed due date/time. Deliberately NOT @ServerTimestamp — that annotation
     *  would stamp "now" into every task written with a null value. */
    val dueAt: Date? = null,

    val linkUrl: String? = null,
    /** The (compressed) photo itself lives in households/{id}/images/{taskId}, not on the
     *  task, so the task list stays small — this flag says whether to go fetch it. */
    val hasImage: Boolean = false,

    val createdBy: String = "",
    val createdByName: String = "",
    @ServerTimestamp
    val createdAt: Date? = null,

    val completed: Boolean = false,
    val completedBy: String? = null,

    /** Short free-text note attached the last time responsibility was passed. */
    val lastHandoffMessage: String? = null,
    /** uid of whoever passed responsibility last — a lookup key, not for display. */
    val lastHandoffFrom: String? = null,
    /** Display name of [lastHandoffFrom], captured at handoff time so the UI never
     *  has to show a raw uid. */
    val lastHandoffFromName: String? = null,
    /** Null until the first handoff (see note on [dueAt] about @ServerTimestamp). */
    val lastHandoffAt: Date? = null
) {
    val urgencyEnum: Urgency
        get() = runCatching { Urgency.valueOf(urgency) }.getOrDefault(Urgency.MEDIUM)
}
