package com.ourhouse.missions.ui.screens.addtask

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.ourhouse.missions.data.model.Category
import com.ourhouse.missions.data.model.Urgency
import com.ourhouse.missions.ui.components.Base64Image
import com.ourhouse.missions.ui.components.UrgencyBadge
import com.ourhouse.missions.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * @param partnerId / partnerName come from the Household doc's other member — pass them
 * in from whichever screen hosts this (see MissionsNavGraph) once household state is loaded.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskScreen(
    viewModel: AddTaskViewModel,
    myUserId: String,
    myName: String,
    partnerId: String?,
    partnerName: String?,
    onSaved: () -> Unit,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.saved) { if (state.saved) onSaved() }

    Scaffold(containerColor = Surface) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(18.dp),
            ) {
                IconButton(onClick = onBack, modifier = Modifier.clip(RoundedCornerShape(11.dp)).background(Background)) {
                    Icon(Icons.Filled.ArrowForward, contentDescription = "חזרה", tint = TextPrimary)
                }
                Spacer(Modifier.width(12.dp))
                Text("משימה חדשה", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold))
            }

            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                LabeledField("כותרת המשימה") {
                    OutlinedTextField(
                        value = state.title,
                        onValueChange = viewModel::onTitleChange,
                        placeholder = { Text("מה צריך לעשות?") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = fieldColors()
                    )
                }

                LabeledField("הערה (אופציונלי)") {
                    OutlinedTextField(
                        value = state.note,
                        onValueChange = viewModel::onNoteChange,
                        placeholder = { Text("פרטים נוספים למשימה...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        shape = RoundedCornerShape(14.dp),
                        colors = fieldColors()
                    )
                }

                LabeledField("קישור למשימה (אופציונלי)") {
                    OutlinedTextField(
                        value = state.linkUrl ?: "",
                        onValueChange = viewModel::onLinkChange,
                        placeholder = { Text("https://...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = fieldColors()
                    )
                }

                LabeledField("תמונה מצורפת (אופציונלי)") {
                    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
                        uri?.let { viewModel.onImageSelected(it) }
                    }
                    val pickedImage = state.imageBase64
                    if (pickedImage != null) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Base64Image(
                                data = pickedImage,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .clip(RoundedCornerShape(14.dp))
                            )
                            IconButton(
                                onClick = viewModel::onImageRemoved,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(6.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(TextPrimary.copy(alpha = 0.55f))
                            ) { Icon(Icons.Filled.Close, contentDescription = "הסרת תמונה", tint = androidx.compose.ui.graphics.Color.White) }
                        }
                    } else {
                        OutlinedButton(
                            onClick = { imagePicker.launch("image/*") },
                            enabled = !state.isProcessingImage,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            if (state.isProcessingImage) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp))
                            } else {
                                Icon(Icons.Filled.Image, contentDescription = null, tint = AccentPrimary, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("הוספת תמונה", color = AccentPrimary)
                            }
                        }
                    }
                }

                LabeledField("אחראי/ת (אופציונלי)") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AssigneeOption("אני", selected = state.assigneeId == myUserId, color = AccentPrimary, modifier = Modifier.weight(1f)) {
                            viewModel.onAssigneeChange(myUserId, myName)
                        }
                        AssigneeOption(partnerName ?: "בן/בת הזוג", selected = state.assigneeId == partnerId, color = AccentPartner, modifier = Modifier.weight(1f)) {
                            viewModel.onAssigneeChange(partnerId, partnerName)
                        }
                        AssigneeOption("ללא שיוך", selected = state.assigneeId == null, color = TextMuted, dashed = true, modifier = Modifier.weight(1f)) {
                            viewModel.onAssigneeChange(null, null)
                        }
                    }
                }

                LabeledField("קטגוריה") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Category.entries.forEach { category ->
                            val selected = state.category == category.key
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(19.dp))
                                    .background(if (selected) TextPrimary else Surface)
                                    .border(1.5.dp, if (selected) TextPrimary else Border, RoundedCornerShape(19.dp))
                                    .clickable { viewModel.onCategoryChange(category.key) }
                                    .padding(horizontal = 14.dp, vertical = 9.dp)
                            ) {
                                Text(
                                    categoryLabel(category),
                                    color = if (selected) androidx.compose.ui.graphics.Color.White else TextSecondary,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                }

                LabeledField("רמת דחיפות") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Urgency.entries.forEach { urgency ->
                            val selected = state.urgency == urgency
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(64.dp)
                                    .clip(RoundedCornerShape(15.dp))
                                    .background(if (selected) Background else Surface)
                                    .border(if (selected) 2.dp else 1.5.dp, if (selected) urgencyColor(urgency) else Border, RoundedCornerShape(15.dp))
                                    .clickable { viewModel.onUrgencyChange(urgency) },
                                contentAlignment = Alignment.Center
                            ) { UrgencyBadge(urgency) }
                        }
                    }
                }

                LabeledField("קביעת תאריך ושעה (אופציונלי)") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Switch(
                                checked = state.hasDueDate,
                                onCheckedChange = viewModel::onDueDateToggle,
                                colors = SwitchDefaults.colors(checkedTrackColor = AccentPrimary)
                            )
                            Text(if (state.hasDueDate) "עם מועד קבוע" else "ללא מועד קבוע", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                        }
                        if (state.hasDueDate) {
                            var showDatePicker by remember { mutableStateOf(false) }
                            var showTimePicker by remember { mutableStateOf(false) }
                            val calendar = remember { Calendar.getInstance() }
                            state.dueAt?.let { calendar.time = it }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(
                                    onClick = { showDatePicker = true },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) { Text(state.dueAt?.let { dueDateFormatter.format(it) } ?: "בחירת תאריך") }
                                OutlinedButton(
                                    onClick = { showTimePicker = true },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) { Text(state.dueAt?.let { dueTimeFormatter.format(it) } ?: "בחירת שעה") }
                            }

                            if (showDatePicker) {
                                val datePickerState = rememberDatePickerState(initialSelectedDateMillis = calendar.timeInMillis)
                                DatePickerDialog(
                                    onDismissRequest = { showDatePicker = false },
                                    confirmButton = {
                                        TextButton(onClick = {
                                            datePickerState.selectedDateMillis?.let { millis ->
                                                val picked = Calendar.getInstance().apply { timeInMillis = millis }
                                                calendar.set(Calendar.YEAR, picked.get(Calendar.YEAR))
                                                calendar.set(Calendar.MONTH, picked.get(Calendar.MONTH))
                                                calendar.set(Calendar.DAY_OF_MONTH, picked.get(Calendar.DAY_OF_MONTH))
                                                viewModel.onDueDateChange(calendar.time)
                                            }
                                            showDatePicker = false
                                        }) { Text("אישור") }
                                    },
                                    dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("ביטול") } }
                                ) { DatePicker(state = datePickerState) }
                            }

                            if (showTimePicker) {
                                val timePickerState = rememberTimePickerState(
                                    initialHour = calendar.get(Calendar.HOUR_OF_DAY),
                                    initialMinute = calendar.get(Calendar.MINUTE),
                                    is24Hour = true
                                )
                                Dialog(onDismissRequest = { showTimePicker = false }) {
                                    androidx.compose.material3.Surface(shape = RoundedCornerShape(20.dp)) {
                                        Column(
                                            modifier = Modifier.padding(20.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            TimePicker(state = timePickerState)
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                                TextButton(onClick = { showTimePicker = false }, modifier = Modifier.weight(1f)) { Text("ביטול") }
                                                TextButton(
                                                    onClick = {
                                                        calendar.set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                                                        calendar.set(Calendar.MINUTE, timePickerState.minute)
                                                        viewModel.onDueDateChange(calendar.time)
                                                        showTimePicker = false
                                                    },
                                                    modifier = Modifier.weight(1f)
                                                ) { Text("אישור") }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                state.errorMessage?.let {
                    Text(it, color = UrgentText, style = MaterialTheme.typography.labelMedium)
                }

                Spacer(Modifier.height(8.dp))
            }

            Button(
                onClick = viewModel::save,
                enabled = !state.isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().padding(20.dp).height(54.dp)
            ) {
                if (state.isSaving) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = androidx.compose.ui.graphics.Color.White)
                else Text("שמירת משימה", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun LabeledField(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = TextPrimary)
        content()
    }
}

@Composable
private fun AssigneeOption(label: String, selected: Boolean, color: androidx.compose.ui.graphics.Color, dashed: Boolean = false, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .height(54.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(if (selected) color.copy(alpha = 0.12f) else Surface)
            .border(if (selected) 2.dp else 1.5.dp, if (selected) color else Border, RoundedCornerShape(15.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = if (selected) color else TextSecondary, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = AccentPrimary,
    unfocusedBorderColor = Border,
    focusedContainerColor = Background,
    unfocusedContainerColor = Background
)

private fun categoryLabel(category: Category) = when (category) {
    Category.HOME -> "בית"
    Category.SHOPPING -> "קניות"
    Category.KIDS -> "ילדים"
    Category.FINANCE -> "כספים"
}

private fun urgencyColor(urgency: Urgency) = when (urgency) {
    Urgency.URGENT -> UrgentText
    Urgency.MEDIUM -> MediumText
    Urgency.LOW -> LowText
}

private val dueDateFormatter = SimpleDateFormat("d.M.yyyy", Locale("iw", "IL"))
private val dueTimeFormatter = SimpleDateFormat("HH:mm", Locale("iw", "IL"))
