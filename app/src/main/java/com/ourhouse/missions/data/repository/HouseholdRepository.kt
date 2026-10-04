package com.ourhouse.missions.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.ourhouse.missions.data.model.Household
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlin.random.Random

/**
 * Creates/joins the shared "household" that links both phones together.
 *
 * Pairing flow used by PairingScreen:
 *  - First partner calls [createHousehold] -> gets back a 6-digit [Household.inviteCode].
 *  - Second partner calls [joinHousehold] with that code -> both memberIds now point at
 *    the same household document, so every task written under it is visible to both.
 */
class HouseholdRepository(private val firestore: FirebaseFirestore) {

    private val households get() = firestore.collection("households")

    companion object {
        /** The app is built around a two-person household; a third join attempt is rejected. */
        const val MAX_HOUSEHOLD_SIZE = 2
    }

    suspend fun createHousehold(userId: String, displayName: String): Household {
        val code = Random.nextInt(100000, 1_000_000).toString()
        val doc = households.document()
        val household = Household(
            id = doc.id,
            inviteCode = code,
            memberIds = listOf(userId),
            memberNames = mapOf(userId to displayName)
        )
        doc.set(household).await()
        return household
    }

    suspend fun joinHousehold(inviteCode: String, userId: String, displayName: String): Result<Household> {
        return runCatching {
            val snapshot = households.whereEqualTo("inviteCode", inviteCode).limit(1).get().await()
            val doc = snapshot.documents.firstOrNull()
                ?: throw IllegalArgumentException("קוד ההזמנה לא נמצא")

            val existingMembers = doc.toObject(Household::class.java)?.memberIds.orEmpty()
            // Re-joining (e.g. re-pairing the same device) doesn't count against the cap —
            // only a genuinely new third member does.
            if (userId !in existingMembers && existingMembers.size >= MAX_HOUSEHOLD_SIZE) {
                throw IllegalStateException("משק הבית כבר מלא — עד $MAX_HOUSEHOLD_SIZE בני זוג לכל משק בית")
            }

            doc.reference.update(
                mapOf(
                    "memberIds" to (existingMembers + userId).distinct(),
                    "memberNames.$userId" to displayName
                )
            ).await()

            doc.reference.get().await().toObject(Household::class.java)!!
        }
    }

    fun observeHousehold(householdId: String): Flow<Household> = callbackFlow {
        val registration = households.document(householdId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            snapshot?.toObject(Household::class.java)?.let { trySend(it) }
        }
        awaitClose { registration.remove() }
    }
}
