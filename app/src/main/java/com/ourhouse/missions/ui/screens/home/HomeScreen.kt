package com.ourhouse.missions.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ourhouse.missions.ui.components.MissionsBottomNavBar
import com.ourhouse.missions.ui.components.NavDestination
import com.ourhouse.missions.ui.components.TaskCard
import com.ourhouse.missions.ui.theme.*

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onAddTask: () -> Unit,
    onOpenTask: (String) -> Unit,
    onNavigate: (NavDestination) -> Unit
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        containerColor = Background,
        bottomBar = { MissionsBottomNavBar(NavDestination.HOME, hasUnreadNotifications = state.hasUnreadHandoff, onSelect = onNavigate) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddTask,
                containerColor = AccentPrimary,
                shape = CircleShape,
                modifier = Modifier.size(58.dp)
            ) { Icon(Icons.Filled.Add, contentDescription = "הוספת משימה חדשה", tint = Color.White) }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            HomeHeader(
                filter = state.filter,
                onFilterChange = viewModel::setFilter,
                sort = state.sort,
                onSortChange = viewModel::setSort,
                showCompleted = state.showCompleted,
                completedCount = state.completedCount,
                onShowCompletedChange = viewModel::setShowCompleted,
                onSettingsClick = { onNavigate(NavDestination.HOUSEHOLD) }
            )

            val errorMessage = state.errorMessage
            if (errorMessage != null) {
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text(errorMessage, color = UrgentText, style = MaterialTheme.typography.bodyMedium)
                }
            } else if (state.isLoading) {
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AccentPrimary)
                }
            } else if (state.tasks.isEmpty()) {
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text(if (state.showCompleted) "עוד לא הושלמו משימות" else "אין משימות פעילות כרגע", color = TextMuted)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(state.tasks, key = { it.id }) { task ->
                        TaskCard(
                            task = task,
                            onClick = { onOpenTask(task.id) },
                            onToggleDone = { viewModel.toggleCompleted(task) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeHeader(
    filter: HomeFilter,
    onFilterChange: (HomeFilter) -> Unit,
    sort: HomeSort,
    onSortChange: (HomeSort) -> Unit,
    showCompleted: Boolean,
    completedCount: Int,
    onShowCompletedChange: (Boolean) -> Unit,
    onSettingsClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().background(Surface).padding(20.dp, 22.dp, 20.dp, 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("המשימות שלנו", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold), color = TextPrimary)
            IconButton(onClick = onSettingsClick) {
                Icon(Icons.Filled.Settings, contentDescription = "הגדרות", tint = TextSecondary)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().background(Background, RoundedCornerShape(12.dp)).padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            SegmentButton("הכל", filter == HomeFilter.ALL, Modifier.weight(1f)) { onFilterChange(HomeFilter.ALL) }
            SegmentButton("שלי", filter == HomeFilter.MINE, Modifier.weight(1f)) { onFilterChange(HomeFilter.MINE) }
            SegmentButton("של בן/בת הזוג", filter == HomeFilter.PARTNER, Modifier.weight(1f)) { onFilterChange(HomeFilter.PARTNER) }
            SegmentButton("ללא שיוך", filter == HomeFilter.UNASSIGNED, Modifier.weight(1f)) { onFilterChange(HomeFilter.UNASSIGNED) }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("מיון:", color = TextMuted, style = MaterialTheme.typography.labelMedium)
            OptionChip("לפי תאריך", sort == HomeSort.DATE) { onSortChange(HomeSort.DATE) }
            OptionChip("לפי דחיפות", sort == HomeSort.URGENCY) { onSortChange(HomeSort.URGENCY) }
            Spacer(Modifier.weight(1f))
            OptionChip(
                if (completedCount > 0) "הושלמו ($completedCount)" else "הושלמו",
                showCompleted
            ) { onShowCompletedChange(!showCompleted) }
        }
    }
}

@Composable
private fun OptionChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        label,
        color = if (selected) Color.White else TextSecondary,
        style = MaterialTheme.typography.labelMedium,
        maxLines = 1,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) AccentPrimary else Background)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp)
    )
}

@Composable
private fun SegmentButton(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) TextPrimary else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (selected) Color.White else TextSecondary,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1
        )
    }
}
