package com.yanis.objectif30

import androidx.activity.ComponentActivity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.QueryStats
import androidx.compose.material.icons.rounded.RestaurantMenu
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.yanis.objectif30.data.UserPreferences
import com.yanis.objectif30.data.UserSettings
import com.yanis.objectif30.notifications.ReminderScheduler
import com.yanis.objectif30.ui.WildBackdrop
import com.yanis.objectif30.ui.WildLine
import com.yanis.objectif30.ui.WildPanel
import kotlinx.coroutines.launch

private enum class WildTab(val label: String) {
    HOME("Accueil"),
    WEEK("Semaine"),
    FOOD("Nutrition"),
    PERF("Progression"),
    SHOP("Courses")
}

@Composable
fun WildsportUltraApp(prefs: UserPreferences) {
    val settings by prefs.settings.collectAsState(initial = UserSettings())
    var tab by remember { mutableStateOf(WildTab.HOME) }
    var nutritionRevision by remember { mutableIntStateOf(0) }
    val context = LocalContext.current
    val activity = context as? ComponentActivity

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(26.dp),
                    color = WildPanel.copy(alpha = 0.98f),
                    border = BorderStroke(1.dp, WildLine),
                    tonalElevation = 4.dp
                ) {
                    NavigationBar(
                        containerColor = Color.Transparent,
                        tonalElevation = 0.dp
                    ) {
                        NavigationBarItem(
                            selected = tab == WildTab.HOME,
                            onClick = { tab = WildTab.HOME },
                            icon = { Icon(Icons.Rounded.Home, null) },
                            label = { Text("Accueil") },
                            alwaysShowLabel = false
                        )
                        NavigationBarItem(
                            selected = tab == WildTab.WEEK,
                            onClick = { tab = WildTab.WEEK },
                            icon = { Icon(Icons.Rounded.CalendarMonth, null) },
                            label = { Text("Semaine") },
                            alwaysShowLabel = false
                        )
                        NavigationBarItem(
                            selected = tab == WildTab.FOOD,
                            onClick = { tab = WildTab.FOOD },
                            icon = { Icon(Icons.Rounded.RestaurantMenu, null) },
                            label = { Text("Repas") },
                            alwaysShowLabel = false
                        )
                        NavigationBarItem(
                            selected = tab == WildTab.PERF,
                            onClick = { tab = WildTab.PERF },
                            icon = { Icon(Icons.Rounded.QueryStats, null) },
                            label = { Text("Perf") },
                            alwaysShowLabel = false
                        )
                        NavigationBarItem(
                            selected = tab == WildTab.SHOP,
                            onClick = { tab = WildTab.SHOP },
                            icon = { Icon(Icons.Rounded.ShoppingCart, null) },
                            label = { Text("Courses") },
                            alwaysShowLabel = false
                        )
                    }
                }
            }
        }
    ) { padding ->
        when (tab) {
            WildTab.HOME -> WildTodayPremium(
                settings,
                Modifier.padding(bottom = padding.calculateBottomPadding())
            )

            WildTab.WEEK -> WildWeekPremium(
                Modifier.padding(bottom = padding.calculateBottomPadding())
            )

            WildTab.FOOD -> WildBackdrop(
                Modifier.padding(bottom = padding.calculateBottomPadding())
            ) {
                UltraNutritionScreen(
                    settings = settings,
                    modifier = Modifier.fillMaxSize(),
                    onPlanChanged = { nutritionRevision++ }
                )
            }

            WildTab.PERF -> WildPerformancePremium(
                settings = settings,
                modifier = Modifier.padding(bottom = padding.calculateBottomPadding()),
                saveWeight = { value ->
                    activity?.lifecycleScope?.launch {
                        prefs.setWeight(value)
                    }
                },
                savePerformance = { pull, push, run, shadow, energy, hunger ->
                    activity?.lifecycleScope?.launch {
                        prefs.setPerformance(
                            pull,
                            push,
                            run,
                            shadow,
                            energy,
                            hunger
                        )
                    }
                },
                changeReminder = { enabled, hour, minute ->
                    activity?.lifecycleScope?.launch {
                        prefs.setReminder(enabled, hour, minute)
                        if (enabled) {
                            ReminderScheduler.schedule(context, hour, minute)
                        } else {
                            ReminderScheduler.cancel(context)
                        }
                    }
                }
            )

            WildTab.SHOP -> WildBackdrop(
                Modifier.padding(bottom = padding.calculateBottomPadding())
            ) {
                UltraShoppingScreen(
                    modifier = Modifier.fillMaxSize(),
                    planRevision = nutritionRevision
                )
            }
        }
    }
}
