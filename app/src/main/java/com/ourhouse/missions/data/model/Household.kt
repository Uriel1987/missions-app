package com.ourhouse.missions.data.model

import com.google.firebase.firestore.DocumentId

/**
 * The shared "household" both partners belong to, at households/{id}.
 * Everything under this document — tasks, member list — is what makes the
 * two phones see the same data.
 */
data class Household(
    @DocumentId
    val id: String = "",

    /** Short human-typeable invite code, e.g. "482913". */
    val inviteCode: String = "",

    val memberIds: List<String> = emptyList(),
    /** uid -> display name, so cards can show "רון" / "מיכל" without another lookup. */
    val memberNames: Map<String, String> = emptyMap()
)
