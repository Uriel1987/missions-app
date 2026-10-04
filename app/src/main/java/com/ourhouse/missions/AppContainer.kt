package com.ourhouse.missions

import android.content.ContentResolver
import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ourhouse.missions.data.local.SessionManager
import com.ourhouse.missions.data.repository.AuthRepository
import com.ourhouse.missions.data.repository.HouseholdRepository
import com.ourhouse.missions.data.repository.TaskRepository
import com.ourhouse.missions.notifications.TaskNotifier

/**
 * Minimal hand-rolled DI container — deliberately no Hilt/Dagger here to keep the
 * project easy to read for a first native-Android build. Swap for Hilt later if the
 * app grows past a few screens.
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    val sessionManager = SessionManager(appContext)
    val authRepository = AuthRepository(auth)
    val householdRepository = HouseholdRepository(firestore)
    val taskRepository = TaskRepository(firestore)
    val taskNotifier = TaskNotifier(appContext, sessionManager)
    val contentResolver: ContentResolver get() = appContext.contentResolver
}
