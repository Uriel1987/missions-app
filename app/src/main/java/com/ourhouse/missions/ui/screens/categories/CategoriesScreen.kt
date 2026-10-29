package com.ourhouse.missions.ui.screens.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ourhouse.missions.data.model.Task
import com.ourhouse.missions.ui.components.CategoryChip
import com.ourhouse.missions.ui.components.MissionsBottomNavBar
import com.ourhouse.missions.ui.components.NavDestination
import com.ourhouse.missions.ui.components.TaskCard
import com.ourhouse.missions.ui.theme.*

@Composable
fun CategoriesScreen(
    viewModel: CategoriesViewModel,
    onOpenTask: (String) -> Unit,
    onNavigate: (NavDestination) -> Unit
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        containerColor = Background,
        bottomBar = { MissionsBottomNavBar(NavDestination.CATEGORIES, hasUnreadNotifications = false, onSelect = onNavigate) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            Column(modifier = Modifier.fillMaxWidth().background(Surface).padding(20.dp, 22.dp, 20.dp, 16.dp)) {
                Text("קטגוריות", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold))
                Text("כל המשימות שלכם, מסודרות לפי קטגוריה", color = TextMuted, style = MaterialTheme.typography.labelMedium)
            }

            val errorMessage = state.errorMessage
            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AccentPrimary)
                }
                errorMessage != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(errorMessage, color = UrgentText, style = MaterialTheme.typography.bodyMedium)
                }
                state.groups.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("אין משימות להצגה כרגע", color = TextMuted)
                }
                else -> LazyColumn(
                    contentPadding = PaddingValues(18.dp),
                    verticalArrangement = Arrangement.spacedBy(22.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(state.groups, key = { it.category?.key ?: it.customLabel ?: "none" }) { group ->
                        CategoryGroupSection(group = group, onOpenTask = onOpenTask, onToggleDone = viewModel::toggleCompleted)
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryGroupSection(group: CategoryGroup, onOpenTask: (String) -> Unit, onToggleDone: (Task) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (group.category != null || group.customLabel != null) {
                CategoryChip(category = group.category, customLabel = group.customLabel)
            } else {
                Text("ללא קטגוריה", color = TextSecondary, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
            }
            Text("${group.tasks.size} משימות", color = TextMuted, style = MaterialTheme.typography.labelMedium)
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            group.tasks.forEach { task ->
                TaskCard(task = task, onClick = { onOpenTask(task.id) }, onToggleDone = { onToggleDone(task) })
            }
        }
    }
}
