package com.ourhouse.missions.data.repository

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

/**
 * Anonymous auth is enough here: we only need a stable, unique uid per phone to key
 * assignments and FCM tokens by — there's no need for either partner to create a
 * password account just to share a to-do list. Enable "Anonymous" sign-in in the
 * Firebase console under Authentication -> Sign-in method.
 */
class AuthRepository(private val auth: FirebaseAuth) {

    val currentUserId: String? get() = auth.currentUser?.uid

    suspend fun ensureSignedIn(): String {
        auth.currentUser?.let { return it.uid }
        val result = auth.signInAnonymously().await()
        return result.user!!.uid
    }
}
