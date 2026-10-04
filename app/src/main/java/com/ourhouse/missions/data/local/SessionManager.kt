package com.ourhouse.missions.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "missions_session")

/**
 * Remembers which household this phone belongs to, so the app can skip the
 * pairing screen on every launch once the two phones are linked once.
 */
class SessionManager(private val context: Context) {

    private val householdIdKey = stringPreferencesKey("household_id")
    private val displayNameKey = stringPreferencesKey("display_name")
    private val baselinedKey = booleanPreferencesKey("notifications_baselined")
    private val notifiedKeysKey = stringSetPreferencesKey("notified_keys")

    val householdId: Flow<String?> = context.dataStore.data.map { it[householdIdKey] }
    val displayName: Flow<String?> = context.dataStore.data.map { it[displayNameKey] }

    suspend fun saveHousehold(householdId: String, displayName: String) {
        context.dataStore.edit { prefs ->
            prefs[householdIdKey] = householdId
            prefs[displayNameKey] = displayName
        }
    }

    /** Which assignment/handoff events this phone has already notified about, so the
     *  live listener and the periodic background check never show the same one twice.
     *  [baselined] is false until the first server-confirmed task list has been seen —
     *  that first list is recorded silently so pairing doesn't fire a burst of old events. */
    data class NotificationState(val baselined: Boolean, val notifiedKeys: Set<String>)

    suspend fun readNotificationState(): NotificationState {
        val prefs = context.dataStore.data.first()
        return NotificationState(prefs[baselinedKey] == true, prefs[notifiedKeysKey].orEmpty())
    }

    suspend fun saveNotificationState(state: NotificationState) {
        context.dataStore.edit { prefs ->
            prefs[baselinedKey] = state.baselined
            prefs[notifiedKeysKey] = state.notifiedKeys
        }
    }

    /** Also forgets the notification state, so re-pairing starts from a fresh baseline. */
    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }
}
