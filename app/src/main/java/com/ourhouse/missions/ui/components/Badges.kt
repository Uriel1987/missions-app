package com.ourhouse.missions.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ourhouse.missions.data.model.Category
import com.ourhouse.missions.data.model.Urgency
import com.ourhouse.missions.ui.theme.*

@Composable
fun UrgencyBadge(urgency: Urgency, modifier: Modifier = Modifier) {
    val (label, text, bg) = when (urgency) {
        Urgency.URGENT -> Triple("דחוף", UrgentText, UrgentBg)
        Urgency.MEDIUM -> Triple("בינוני", MediumText, MediumBg)
        Urgency.LOW -> Triple("לא דחוף", LowText, LowBg)
    }
    Text(
        text = label,
        color = text,
        style = MaterialTheme.typography.labelSmall,
        modifier = modifier
            .background(bg, RoundedCornerShape(7.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}

@Composable
fun CategoryChip(category: Category?, customLabel: String? = null, modifier: Modifier = Modifier) {
    val (label, text, bg) = when (category) {
        Category.HOME -> Triple("בית", CategoryHomeText, CategoryHomeBg)
        Category.SHOPPING -> Triple("קניות", CategoryShoppingText, CategoryShoppingBg)
        Category.KIDS -> Triple("ילדים", CategoryKidsText, CategoryKidsBg)
        Category.FINANCE -> Triple("כספים", CategoryFinanceText, CategoryFinanceBg)
        null -> Triple(customLabel ?: return, TextSecondary, Background)
    }
    Text(
        text = label,
        color = text,
        style = MaterialTheme.typography.labelSmall,
        modifier = modifier
            .background(bg, RoundedCornerShape(7.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}
