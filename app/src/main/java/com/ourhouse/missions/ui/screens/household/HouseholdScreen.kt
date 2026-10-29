package com.ourhouse.missions.ui.screens.household

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.ourhouse.missions.notifications.NotificationHelper
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
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
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

                    val watchEnabled by viewModel.backgroundWatchEnabled.collectAsState()
                    NotificationHealthCard(watchEnabled = watchEnabled, onWatchToggle = viewModel::setBackgroundWatch)

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

/** Shows whether this phone can actually receive assignment notifications, and fixes for each way it can't. */
@Composable
private fun NotificationHealthCard(watchEnabled: Boolean, onWatchToggle: (Boolean) -> Unit) {
    val context = LocalContext.current

    // Re-check whenever the user comes back from a system settings screen.
    var refresh by remember { mutableIntStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) refresh++ }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val notificationsOn = remember(refresh) { NotificationManagerCompat.from(context).areNotificationsEnabled() }
    val batteryUnrestricted = remember(refresh) {
        context.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(context.packageName)
    }

    Column(
        modifier = Modifier.fillMaxWidth().background(Surface, RoundedCornerShape(20.dp)).border(1.dp, Border, RoundedCornerShape(20.dp)).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("התראות על משימות חדשות", style = MaterialTheme.typography.labelLarge, color = TextPrimary)

        HealthRow(
            ok = notificationsOn,
            text = if (notificationsOn) "התראות מותרות במכשיר" else "ההתראות חסומות למכשיר הזה",
            actionLabel = if (notificationsOn) null else "פתיחת הגדרות"
        ) {
            context.startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("האזנה ברקע (מומלץ)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(
                    "מציג סמל קטן קבוע בשורת ההתראות, ובתמורה ההתראות מגיעות מיד גם כשהאפליקציה סגורה.",
                    style = MaterialTheme.typography.labelMedium, color = TextMuted
                )
            }
            Switch(checked = watchEnabled, onCheckedChange = onWatchToggle, colors = SwitchDefaults.colors(checkedTrackColor = AccentPrimary))
        }

        HealthRow(
            ok = batteryUnrestricted,
            text = if (batteryUnrestricted) "אין הגבלת סוללה על האפליקציה" else "הסוללה עלולה לעצור את האפליקציה כשהמסך כבוי",
            actionLabel = if (batteryUnrestricted) null else "ביטול ההגבלה"
        ) {
            runCatching {
                context.startActivity(
                    Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${context.packageName}"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        }

        OutlinedButton(
            onClick = { NotificationHelper.show(context, "התראת ניסיון", "אם אתם רואים את זה — ההתראות עובדות במכשיר הזה", notificationId = 1) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) { Text("שליחת התראת ניסיון", color = AccentPrimary) }
    }
}

@Composable
private fun HealthRow(ok: Boolean, text: String, actionLabel: String?, onAction: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(
            if (ok) Icons.Filled.CheckCircle else Icons.Filled.Warning,
            contentDescription = null,
            tint = if (ok) AccentPrimary else UrgentText,
            modifier = Modifier.size(20.dp)
        )
        Text(text, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, modifier = Modifier.weight(1f))
        if (actionLabel != null) TextButton(onClick = onAction) { Text(actionLabel, color = AccentPrimary) }
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
