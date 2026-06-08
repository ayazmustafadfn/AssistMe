package com.artsistem.assistme

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings as AndroidSettings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.artsistem.assistme.ui.ReminderEditScreen
import com.artsistem.assistme.ui.ReminderListScreen
import com.artsistem.assistme.ui.ReminderViewModel
import com.artsistem.assistme.ui.theme.AssistMeTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AssistMeTheme {
                val context = LocalContext.current

                // Android 13+ bildirim iznini iste.
                val notifPermission = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { /* sonucu sessizce ele al */ }

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    requestExactAlarmIfNeeded(context)
                    requestFullScreenIntentIfNeeded(context)
                }

                val navController = rememberNavController()
                val viewModel: ReminderViewModel =
                    viewModel(factory = ReminderViewModel.Factory)

                NavHost(navController = navController, startDestination = "list") {
                    composable("list") {
                        ReminderListScreen(
                            viewModel = viewModel,
                            onAdd = { navController.navigate("edit/0") },
                            onEdit = { id -> navController.navigate("edit/$id") }
                        )
                    }
                    composable("edit/{id}") { backStackEntry ->
                        val id = backStackEntry.arguments?.getString("id")?.toLongOrNull() ?: 0L
                        ReminderEditScreen(
                            viewModel = viewModel,
                            reminderId = id,
                            onDone = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }

    private fun requestExactAlarmIfNeeded(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                runCatching {
                    context.startActivity(
                        Intent(AndroidSettings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                            data = Uri.parse("package:${context.packageName}")
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                    )
                }
            }
        }
    }

    /**
     * Android 14+ tam ekran alarm için ayrı bir izin ister. Verilmezse alarm
     * tam ekran açılmaz ama yine yüksek öncelikli sesli bildirim olarak düşer.
     */
    private fun requestFullScreenIntentIfNeeded(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val nm = context.getSystemService(android.app.NotificationManager::class.java)
            if (nm != null && !nm.canUseFullScreenIntent()) {
                runCatching {
                    context.startActivity(
                        Intent(AndroidSettings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                            data = Uri.parse("package:${context.packageName}")
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                    )
                }
            }
        }
    }
}
