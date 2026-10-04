package com.ourhouse.missions.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.People
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ourhouse.missions.ui.theme.AccentPrimary
import com.ourhouse.missions.ui.theme.Border
import com.ourhouse.missions.ui.theme.Surface
import com.ourhouse.missions.ui.theme.TextMuted
import com.ourhouse.missions.ui.theme.UrgentText

enum class NavDestination { HOME, CATEGORIES, NOTIFICATIONS, HOUSEHOLD }

@Composable
fun MissionsBottomNavBar(
    current: NavDestination,
    hasUnreadNotifications: Boolean,
    onSelect: (NavDestination) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().background(Surface)) {
        Divider(color = Border, thickness = 1.dp)
        Row(modifier = Modifier.fillMaxWidth().height(78.dp), verticalAlignment = Alignment.CenterVertically) {
            NavItem(Icons.Filled.Home, "בית", current == NavDestination.HOME) { onSelect(NavDestination.HOME) }
            NavItem(Icons.Outlined.Category, "קטגוריות", current == NavDestination.CATEGORIES) { onSelect(NavDestination.CATEGORIES) }
            NavItem(Icons.Outlined.Notifications, "התראות", current == NavDestination.NOTIFICATIONS, badge = hasUnreadNotifications) { onSelect(NavDestination.NOTIFICATIONS) }
            NavItem(Icons.Outlined.People, "הבית שלנו", current == NavDestination.HOUSEHOLD) { onSelect(NavDestination.HOUSEHOLD) }
        }
    }
}

@Composable
private fun RowScope.NavItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    badge: Boolean = false,
    onClick: () -> Unit
) {
    val color = if (selected) AccentPrimary else TextMuted
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box {
            Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(21.dp))
            if (badge) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .align(Alignment.TopEnd)
                        .background(UrgentText, CircleShape)
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(label, color = color, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold))
    }
}
