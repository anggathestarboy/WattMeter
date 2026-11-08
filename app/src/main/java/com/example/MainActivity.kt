package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ElectricMeter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.ElectricMeter
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MainViewModel
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.ProtectionScreen
import com.example.ui.screens.TipsScreen
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary

enum class NavTab(
    val titleRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    DASHBOARD(R.string.tab_dashboard, Icons.Filled.ElectricMeter, Icons.Outlined.ElectricMeter, "nav_tab_dashboard"),
    HISTORY(R.string.tab_history, Icons.Filled.History, Icons.Outlined.History, "nav_tab_history"),
    PROTECTION(R.string.tab_protection, Icons.Filled.Shield, Icons.Outlined.Shield, "nav_tab_protection"),
    TIPS(R.string.tab_tips, Icons.Filled.Lightbulb, Icons.Outlined.Lightbulb, "nav_tab_tips")
}

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            MyApplicationTheme {
                WattMeterApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun WattMeterApp(viewModel: MainViewModel) {
    val context = LocalContext.current
    var currentTab by rememberSaveable { mutableStateOf(NavTab.DASHBOARD) }

    // Request notification permission on Android 13+ for battery charge alarms
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPermission) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // BackHandler: return to Dashboard if on other tabs
    if (currentTab != NavTab.DASHBOARD) {
        BackHandler {
            currentTab = NavTab.DASHBOARD
        }
    }

    val batteryData by viewModel.batteryData.collectAsStateWithLifecycle()
    val wattHistory by viewModel.wattHistory.collectAsStateWithLifecycle()
    val chargingSessions by viewModel.chargingSessions.collectAsStateWithLifecycle()
    val targetPercentage by viewModel.targetPercentage.collectAsStateWithLifecycle()
    val isAlarmEnabled by viewModel.isAlarmEnabled.collectAsStateWithLifecycle()
    val isVibrationEnabled by viewModel.isVibrationEnabled.collectAsStateWithLifecycle()
    val tempWarningLimit by viewModel.tempWarningLimit.collectAsStateWithLifecycle()
    val simulatedWatts by viewModel.simulatedWatts.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = DarkBackground,
        bottomBar = {
            NavigationBar(
                containerColor = DarkSurface,
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("bottom_nav_bar")
            ) {
                NavTab.values().forEach { tab ->
                    val isSelected = currentTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentTab = tab },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = stringResource(tab.titleRes)
                            )
                        },
                        label = {
                            Text(text = stringResource(tab.titleRes))
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF00363D),
                            selectedTextColor = CyanPrimary,
                            indicatorColor = CyanPrimary,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextSecondary
                        ),
                        modifier = Modifier.testTag(tab.testTag)
                    )
                }
            }
        }
    ) { innerPadding ->
        when (currentTab) {
            NavTab.DASHBOARD -> DashboardScreen(
                batteryData = batteryData,
                wattHistory = wattHistory,
                simulatedWatts = simulatedWatts,
                onSetSimulationWatts = { viewModel.setSimulationWatts(it) },
                onRefresh = { viewModel.refresh() },
                modifier = Modifier.padding(innerPadding)
            )

            NavTab.HISTORY -> HistoryScreen(
                sessions = chargingSessions,
                onSaveCurrentSession = { viewModel.saveCurrentSessionManually() },
                onDeleteSession = { viewModel.deleteSession(it) },
                onClearAll = { viewModel.clearAllSessions() },
                modifier = Modifier.padding(innerPadding)
            )

            NavTab.PROTECTION -> ProtectionScreen(
                batteryData = batteryData,
                targetPercentage = targetPercentage,
                isAlarmEnabled = isAlarmEnabled,
                isVibrationEnabled = isVibrationEnabled,
                tempWarningLimit = tempWarningLimit,
                onSetTargetPercentage = { viewModel.setTargetPercentage(it) },
                onToggleAlarm = { viewModel.toggleAlarm(it) },
                onToggleVibration = { viewModel.toggleVibration(it) },
                onSetTempWarningLimit = { viewModel.setTempWarningLimit(it) },
                modifier = Modifier.padding(innerPadding)
            )

            NavTab.TIPS -> TipsScreen(
                onTriggerRefresh = { viewModel.refresh() },
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}
