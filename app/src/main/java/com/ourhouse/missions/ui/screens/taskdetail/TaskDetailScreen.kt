package com.ourhouse.missions.ui.screens.taskdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ourhouse.missions.data.model.Category
import com.ourhouse.missions.ui.components.Base64Image
import com.ourhouse.missions.ui.components.CategoryChip
import com.ourhouse.missions.ui.components.UrgencyBadge
import com.ourhouse.missions.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun TaskDetailScreen(
    viewModel: TaskDetailViewModel,
    taskId: String,
    partnerId: String,
    partnerName: String,
    onEdit: () -> Unit,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    var showDeleteConfirm by remember { mutableStateOf(false) }
    LaunchedEffect(taskId) { viewModel.load(taskId) }
    LaunchedEffect(state.deleted) { if (state.deleted) onBack() }

    val task = state.task ?: run {
        val errorMessage = state.errorMessage
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (errorMessage != null) {
                Text(errorMessage, color = UrgentText, style = MaterialTheme.typography.bodyMedium)
            } else {
                CircularProgressIndicator(color = AccentPrimary)
            }
        }
        return
    }

    Scaffold(containerColor = Surface) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(18.dp)) {
                IconButton(onClick = onBack, modifier = Modifier.clip(RoundedCornerShape(11.dp)).background(Background)) {
                    Icon(Icons.Filled.ArrowForward, contentDescription = "חזרה", tint = TextPrimary)
                }
                Spacer(Modifier.width(12.dp))
                Text("פרטי משימה", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold), modifier = Modifier.weight(1f))
                IconButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, contentDescription = "עריכת משימה", tint = TextSecondary)
                }
                IconButton(onClick = { showDeleteConfirm = true }) {
                    Icon(Icons.Filled.Delete, contentDescription = "מחיקת משימה", tint = UrgentText)
                }
            }

            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        UrgencyBadge(task.urgencyEnum)
                        Category.fromKey(task.category)?.let { CategoryChip(it) }
                    }
                    Text(
                        task.title, style = MaterialTheme.typography.titleLarge,
                        color = if (task.completed) TextMuted else TextPrimary,
                        textDecoration = if (task.completed) TextDecoration.LineThrough else null
                    )
                    if (task.note.isNotBlank()) Text(task.note, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }

                if (task.completed) {
                    OutlinedButton(
                        onClick = viewModel::toggleCompleted,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) { Text("החזרה למשימות פעילות", style = MaterialTheme.typography.labelLarge, color = AccentPrimary) }
                } else {
                    Button(
                        onClick = viewModel::toggleCompleted,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("סימון כבוצעה", style = MaterialTheme.typography.labelLarge)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    InfoTile("מועד לביצוע", task.dueAt?.let { dateFormatter.format(it) } ?: "ללא תאריך", Modifier.weight(1f))
                    InfoTile("נוסף על ידי", task.createdByName.ifBlank { "—" }, Modifier.weight(1f))
                }

                if (task.hasImage) {
                    val photo = state.imageBase64
                    if (photo != null) {
                        Base64Image(
                            data = photo,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(16.dp))
                        )
                    } else {
                        Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = AccentPrimary)
                        }
                    }
                }

                task.linkUrl?.let { url ->
                    AttachmentRow(url)
                }

                if (task.assigneeName != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth().background(CategoryHomeBg, RoundedCornerShape(18.dp)).padding(15.dp)
                    ) {
                        Box(Modifier.size(42.dp).background(AccentPrimary, CircleShape), contentAlignment = Alignment.Center) {
                            Text(task.assigneeName.take(2), color = Color.White, style = MaterialTheme.typography.labelLarge)
                        }
                        Column {
                            Text("${task.assigneeName} אחראי/ת כרגע", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                            task.lastHandoffAt?.let {
                                Text("הועבר אליו/ה על ידי ${task.lastHandoffFromName ?: "—"}", style = MaterialTheme.typography.labelMedium, color = AccentPrimary)
                            }
                        }
                    }
                }

                Divider(color = Border)

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("העברת אחריות ל$partnerName", style = MaterialTheme.typography.titleMedium)

                    OutlinedTextField(
                        value = state.handoffMessage,
                        onValueChange = viewModel::onHandoffMessageChange,
                        placeholder = { Text("לדוגמה: אני בעבודה עד מאוחר, תוכל/י לטפל בזה היום?") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentPrimary,
                            unfocusedBorderColor = Border,
                            focusedContainerColor = Background,
                            unfocusedContainerColor = Background
                        )
                    )

                    Button(
                        onClick = { viewModel.passResponsibility(partnerId, partnerName) },
                        enabled = !state.isSending,
                        colors = ButtonDefaults.buttonColors(containerColor = TextPrimary),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) {
                        if (state.isSending) CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                        else Text("העברה ושליחת התראה ל$partnerName", style = MaterialTheme.typography.labelLarge)
                    }

                    Text(
                        "$partnerName תקבל/י התראה מיידית בטלפון עם ההודעה שצירפתם.",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextMuted,
                        modifier = Modifier.fillMaxWidth()
                    )

                    state.errorMessage?.let {
                        Text(it, color = UrgentText, style = MaterialTheme.typography.labelMedium)
                    }
                }

                Spacer(Modifier.height(12.dp))
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("למחוק את המשימה?") },
            text = { Text("המשימה תימחק לצמיתות אצל שניכם ולא ניתן יהיה לשחזר אותה.") },
            confirmButton = {
                TextButton(onClick = { showDeleteConfirm = false; viewModel.deleteTask() }, enabled = !state.isDeleting) {
                    Text("מחיקה", color = UrgentText)
                }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("ביטול") } }
        )
    }
}

@Composable
private fun InfoTile(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.background(Background, RoundedCornerShape(16.dp)).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
    }
}

@Composable
private fun AttachmentRow(url: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(Background, RoundedCornerShape(16.dp))
            .border(1.dp, Border, RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Box(Modifier.size(36.dp).background(CategoryHomeBg, RoundedCornerShape(11.dp)), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Link, contentDescription = null, tint = AccentPrimary)
        }
        Text(url, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.weight(1f))
    }
}

private val dateFormatter = SimpleDateFormat("EEE, HH:mm", Locale("iw", "IL"))
