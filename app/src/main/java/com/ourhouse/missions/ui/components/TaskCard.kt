package com.ourhouse.missions.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ourhouse.missions.data.model.Category as MissionCategory
import com.ourhouse.missions.data.model.Task
import com.ourhouse.missions.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun TaskCard(task: Task, onClick: () -> Unit, modifier: Modifier = Modifier, onToggleDone: (() -> Unit)? = null) {
    val category = MissionCategory.fromKey(task.category)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .background(Surface, RoundedCornerShape(20.dp))
            .border(1.dp, Border, RoundedCornerShape(20.dp))
            .padding(15.dp),
        horizontalArrangement = Arrangement.spacedBy(13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onToggleDone != null) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (task.completed) AccentPrimary else Surface)
                    .border(2.dp, if (task.completed) AccentPrimary else TextMuted, CircleShape)
                    .clickable(onClick = onToggleDone),
                contentAlignment = Alignment.Center
            ) {
                if (task.completed) {
                    Icon(Icons.Filled.Check, contentDescription = "החזרה למשימות פעילות", tint = Color.White, modifier = Modifier.size(18.dp))
                } else {
                    Icon(Icons.Filled.Check, contentDescription = "סימון כבוצעה", tint = Color.Transparent, modifier = Modifier.size(18.dp))
                }
            }
        }

        // Category icon chip stands in for the task's category — deliberately not a
        // colored left border, to keep the card from reading as generic AI-template UI.
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(categoryBg(category), RoundedCornerShape(13.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.Category, contentDescription = null, tint = categoryText(category))
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (task.completed) TextMuted else TextPrimary,
                    textDecoration = if (task.completed) TextDecoration.LineThrough else null,
                    modifier = Modifier.weight(1f)
                )
                if (!task.completed) UrgencyBadge(task.urgencyEnum)
            }

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.CalendarToday, contentDescription = null, tint = TextMuted, modifier = Modifier.size(13.dp))
                    Text(
                        text = task.dueAt?.let { dateFormatter.format(it) } ?: "ללא תאריך",
                        color = TextSecondary,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (task.assigneeName != null) {
                        AssigneeAvatar(name = task.assigneeName, color = AccentPrimary)
                        Text(task.assigneeName, color = TextSecondary, style = MaterialTheme.typography.labelMedium)
                    } else {
                        Icon(Icons.Outlined.PersonOutline, contentDescription = null, tint = TextMuted, modifier = Modifier.size(15.dp))
                        Text("ללא שיוך", color = TextMuted, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
fun AssigneeAvatar(name: String, color: Color, size: Int = 19) {
    Box(
        modifier = Modifier.size(size.dp).background(color, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name.take(2),
            color = Color.White,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = (size / 2.2f).sp)
        )
    }
}

private fun categoryBg(category: MissionCategory?) = when (category) {
    MissionCategory.HOME -> CategoryHomeBg
    MissionCategory.SHOPPING -> CategoryShoppingBg
    MissionCategory.KIDS -> CategoryKidsBg
    MissionCategory.FINANCE -> CategoryFinanceBg
    null -> Background
}

private fun categoryText(category: MissionCategory?) = when (category) {
    MissionCategory.HOME -> CategoryHomeText
    MissionCategory.SHOPPING -> CategoryShoppingText
    MissionCategory.KIDS -> CategoryKidsText
    MissionCategory.FINANCE -> CategoryFinanceText
    null -> TextMuted
}

private val dateFormatter = SimpleDateFormat("EEE, HH:mm", Locale("iw", "IL"))
