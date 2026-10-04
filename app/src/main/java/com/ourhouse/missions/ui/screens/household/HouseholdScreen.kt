package com.ourhouse.missions.ui.screens.household

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ourhouse.missions.ui.components.AssigneeAvatar
import com.ourhouse.missions.ui.components.MissionsBottomNavBar
import com.ourhouse.missions.ui.components.NavDestination
import com.ourhouse.missions.ui.theme.*

@Composable
fun HouseholdScreen(
    viewModel: HouseholdViewModel,
    onNavigate: (NavDestination) -> Unit,
    onLeft: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var showLeaveConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(state.left) { if (state.left) onLeft() }

    Scaffold(
        containerColor = Background,
        bottomBar = { MissionsBottomNavBar(NavDestination.HOUSEHOLD, hasUnreadNotifications = false, onSelect = onNavigate) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            Column(modifier = Modifier.fillMaxWidth().background(Surface).padding(20.dp, 22.dp, 20.dp, 16.dp)) {
                Text("הבית שלנו", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold))
                Text("ניהול החיבור בין המכשירים", color = TextMuted, style = MaterialTheme.typography.labelMedium)
            }

            val errorMessage = state.errorMessage
            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AccentPrimary)
                }
                errorMessage != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(errorMessage, color = UrgentText, style = MaterialTheme.typography.bodyMedium)
                }
                else -> Column(
                    modifier = Modifier.padding(20.dp).fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    val household = state.household
                    if (household != null) {
                        Column(
                            modifier = Modifier.fillMaxWidth().background(Surface, RoundedCornerShape(20.dp)).border(1.dp, Border, RoundedCornerShape(20.dp)).padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("קוד ההזמנה שלכם", color = TextMuted, style = MaterialTheme.typography.labelMedium)
                            Text(household.inviteCode, color = AccentPrimary, style = MaterialTheme.typography.titleLarge.copy(fontSize = 30.sp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedButton(
                                    onClick = { copyToClipboard(context, household.inviteCode) },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("העתקה")
                                }
                                Button(
                                    onClick = { shareInviteCode(context, household.inviteCode) },
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("שיתוף")
                                }
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("בני הבית", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                            household.memberIds.forEach { uid ->
                                val name = household.memberNames[uid] ?: "משתמש/ת"
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.fillMaxWidth().background(Surface, RoundedCornerShape(16.dp)).padding(14.dp)
                                ) {
                                    AssigneeAvatar(name = name, color = if (uid == state.myUserId) AccentPrimary else AccentPartner, size = 34)
                                    Text(
                                        if (uid == state.myUserId) "$name (אני)" else name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.weight(1f))

                    OutlinedButton(
                        onClick = { showLeaveConfirm = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = UrgentText),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) { Text("ניתוק המכשיר הזה") }
                }
            }
        }
    }

    if (showLeaveConfirm) {
        AlertDialog(
            onDismissRequest = { showLeaveConfirm = false },
            title = { Text("ניתוק המכשיר?") },
            text = { Text("המכשיר הזה ייצא מהבית המשותף ותצטרכו להתחבר מחדש עם קוד ההזמנה. הנתונים של שאר בני הבית לא יימחקו.") },
            confirmButton = {
                TextButton(onClick = { showLeaveConfirm = false; viewModel.leaveOnThisDevice() }) {
                    Text("ניתוק", color = UrgentText)
                }
            },
            dismissButton = { TextButton(onClick = { showLeaveConfirm = false }) { Text("ביטול") } }
        )
    }
}

private fun copyToClipboard(context: Context, code: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("קוד הזמנה", code))
}

private fun shareInviteCode(context: Context, code: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, "בואו נתחבר באפליקציית המשימות שלנו! קוד ההזמנה: $code")
    }
    context.startActivity(Intent.createChooser(intent, "שיתוף קוד ההזמנה"))
}
