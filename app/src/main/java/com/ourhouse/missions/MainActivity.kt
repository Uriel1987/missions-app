package com.ourhouse.missions

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.content.ContextCompat
import com.ourhouse.missions.ui.navigation.MissionsNavGraph
import com.ourhouse.missions.ui.theme.MissionsTheme

class MainActivity : ComponentActivity() {

    // Android 13+ requires this to be asked for at runtime — without it, the
    // "assigned to you" / "passed to you" notifications never display.
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op either way */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestNotificationPermissionIfNeeded()

        val container = (application as MissionsApp).container

        setContent {
            // The whole app is Hebrew-first, so force RTL layout direction regardless
            // of the device's system language.
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                MissionsTheme {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        MissionsNavGraph(container = container)
                    }
                }
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
