package com.ourhouse.missions.ui.screens.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ourhouse.missions.data.model.Task
import com.ourhouse.missions.ui.components.MissionsBottomNavBar
import com.ourhouse.missions.ui.components.NavDestination
import com.ourhouse.missions.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Lightweight "activity feed" built from the tasks list itself (any task with a
 * [Task.lastHandoffAt] recently set). For a fuller history than "most recent handoff
 * per task", add a dedicated households/{id}/activity collection written by the same
 * Cloud Function that sends the push in functions/index.js.
 */
@Composable
fun NotificationsScreen(tasks: List<Task>, onNavigate: (NavDestination) -> Unit) {
    val recentHandoffs = tasks
        .filter { it.lastHandoffAt != null }
        .sortedByDescending { it.lastHandoffAt }

    Scaffold(
        containerColor = Background,
        bottomBar = { MissionsBottomNavBar(NavDestination.NOTIFICATIONS, hasUnreadNotifications = false, onSelect = onNavigate) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            Column(modifier = Modifier.fillMaxWidth().background(Surface).padding(20.dp, 22.dp, 20.dp, 16.dp)) {
                Text("התראות", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold))
                Text("כל עדכון על משימה שלכם מגיע לכאן ולטלפון", color = TextMuted, style = MaterialTheme.typography.labelMedium)
            }

            if (recentHandoffs.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("אין התראות עדיין", color = TextMuted)
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(recentHandoffs, key = { it.id }) { task -> HandoffRow(task) }
                }
            }
        }
    }
}

@Composable
private fun HandoffRow(task: Task) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface, RoundedCornerShape(18.dp))
            .border(1.dp, Border, RoundedCornerShape(18.dp))
            .padding(15.dp)
    ) {
        Box(Modifier.size(40.dp).background(CategoryHomeBg, RoundedCornerShape(13.dp)), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.SwapHoriz, contentDescription = null, tint = AccentPrimary)
        }
        Column {
            Text("${task.lastHandoffFromName ?: "מישהו"} העביר/ה אחריות", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text("\"${task.title}\"" + (task.lastHandoffMessage?.let { " — $it" } ?: ""), color = TextSecondary, style = MaterialTheme.typography.labelMedium)
            task.lastHandoffAt?.let {
                Text(timeFormatter.format(it), color = TextMuted, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

private val timeFormatter = SimpleDateFormat("d.M, HH:mm", Locale("iw", "IL"))
