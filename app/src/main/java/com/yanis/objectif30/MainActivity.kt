package com.yanis.objectif30

import android.Manifest
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.yanis.objectif30.data.*
import com.yanis.objectif30.notifications.ReminderScheduler
import com.yanis.objectif30.ui.Objectif30Theme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.max

class MainActivity : ComponentActivity() {
    private lateinit var prefs: UserPreferences

    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = UserPreferences(this)

        lifecycleScope.launch {
            val s = prefs.settings.first()
            if (s.reminderEnabled) ReminderScheduler.schedule(this@MainActivity, s.reminderHour, s.reminderMinute)
        }

        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)

        setContent { Objectif30Theme { PremiumApp(prefs) } }
    }
}

enum class AppTab(val label: String, val emoji: String) {
    TODAY("Aujourd’hui", "🥊"),
    WEEK("Semaine", "📅"),
    FOOD("Repas", "🍲"),
    PROGRESS("Suivi", "📉")
}

@Composable
fun Objectif30App(prefs: UserPreferences) {
    val settings by prefs.settings.collectAsState(initial = UserSettings())
    var tab by remember { mutableStateOf(AppTab.TODAY) }
    val context = LocalContext.current
    val activity = context as? ComponentActivity

    Scaffold(
        bottomBar = {
            NavigationBar {
                AppTab.entries.forEach { item ->
                    NavigationBarItem(
                        selected = tab == item,
                        onClick = { tab = item },
                        icon = { Text(item.emoji) },
                        label = { Text(item.label) }
                    )
                }
            }
        }
    ) { pad ->
        when (tab) {
            AppTab.TODAY -> DayScreen(PlanRepository.day(todayIndex()), Modifier.padding(pad))
            AppTab.WEEK -> WeekScreen(Modifier.padding(pad))
            AppTab.FOOD -> FoodScreen(Modifier.padding(pad))
            AppTab.PROGRESS -> ProgressScreen(
                modifier = Modifier.padding(pad),
                settings = settings,
                saveWeight = { w -> activity?.lifecycleScope?.launch { prefs.setWeight(w) } },
                changeReminder = { enabled, h, m ->
                    activity?.lifecycleScope?.launch {
                        prefs.setReminder(enabled, h, m)
                        if (enabled) ReminderScheduler.schedule(context, h, m) else ReminderScheduler.cancel(context)
                    }
                }
            )
        }
    }
}

private fun todayIndex() = LocalDate.now().dayOfWeek.value - 1

@Composable
fun DayScreen(plan: DayPlan, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("OBJECTIF 30 ANS", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Text(plan.name, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black)
            Text(plan.focus, style = MaterialTheme.typography.titleMedium)
            AssistChip(onClick = {}, label = { Text("Intensité : ${plan.intensity}") })
        }

        item { Text("🏋️ Séance", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
        items(plan.exercises) { ex ->
            ElevatedCard {
                Column(Modifier.padding(16.dp)) {
                    Text(ex.name, fontWeight = FontWeight.Bold)
                    Text(ex.prescription)
                    Spacer(Modifier.height(8.dp))
                    Text("Remplacement : ${ex.replacement}", color = MaterialTheme.colorScheme.secondary)
                    if (ex.note.isNotBlank()) Text(ex.note, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        item { Text("🍲 Menu chaud", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
        items(plan.meals) { meal ->
            OutlinedCard {
                Column(Modifier.padding(16.dp)) {
                    Text(meal.title, fontWeight = FontWeight.Bold)
                    Text(meal.description)
                }
            }
        }

        item { Text("🎒 À préparer", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
        items(plan.prep) { Text("✓ $it") }

        item {
            ElevatedCard { Column(Modifier.padding(16.dp)) {
                Text("😴 Récupération", fontWeight = FontWeight.Bold)
                Text(plan.recovery)
            }}
        }

        item {
            Text(
                "En cas de douleur thoracique, malaise, essoufflement inhabituel ou douleur articulaire vive : arrête la séance.",
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
fun WeekScreen(modifier: Modifier = Modifier) {
    var selected by remember { mutableIntStateOf(todayIndex()) }
    Column(modifier.fillMaxSize()) {
        ScrollableTabRow(selectedTabIndex = selected) {
            PlanRepository.days.forEachIndexed { i, day ->
                Tab(selected = selected == i, onClick = { selected = i }, text = { Text(day.name.take(3)) })
            }
        }
        DayScreen(PlanRepository.day(selected), Modifier.weight(1f))
    }
}

@Composable
fun FoodScreen(modifier: Modifier = Modifier) {
    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Nutrition", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black)
            Text("Chaud, simple, économique.")
        }
        item {
            ElevatedCard { Column(Modifier.padding(16.dp)) {
                Text("Repères", fontWeight = FontWeight.Bold)
                Text("• Protéines : environ 150–170 g/jour.")
                Text("• Légumes au déjeuner et au dîner.")
                Text("• Féculents adaptés aux jours d’entraînement.")
                Text("• Eau comme boisson principale.")
            }}
        }
        items(PlanRepository.days) { day ->
            ElevatedCard { Column(Modifier.padding(16.dp)) {
                Text(day.name, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                day.meals.forEach {
                    Spacer(Modifier.height(6.dp))
                    Text(it.title, fontWeight = FontWeight.SemiBold)
                    Text(it.description)
                }
            }}
        }
    }
}

@Composable
fun ProgressScreen(
    modifier: Modifier = Modifier,
    settings: UserSettings,
    saveWeight: (Double) -> Unit,
    changeReminder: (Boolean, Int, Int) -> Unit
) {
    val context = LocalContext.current
    var weight by remember(settings.currentWeight) { mutableStateOf("%.1f".format(settings.currentWeight)) }

    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Suivi", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black)
        }

        item {
            ElevatedCard { Column(Modifier.padding(16.dp)) {
                Text("Poids", fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = it.replace(',', '.') },
                    label = { Text("Poids actuel (kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                Button(onClick = { weight.toDoubleOrNull()?.let(saveWeight) }) { Text("Enregistrer") }

                Spacer(Modifier.height(16.dp))
                val total = max(0.1, 90.0 - settings.targetWeight)
                val done = (90.0 - settings.currentWeight).coerceIn(0.0, total)
                LinearProgressIndicator(progress = { (done / total).toFloat() }, modifier = Modifier.fillMaxWidth())
                Text("${"%.1f".format(max(0.0, settings.currentWeight - settings.targetWeight))} kg jusqu’à 80 kg")
            }}
        }

        item {
            ElevatedCard { Column(Modifier.padding(16.dp)) {
                Text("Notification du soir", fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = settings.reminderEnabled,
                        onCheckedChange = { changeReminder(it, settings.reminderHour, settings.reminderMinute) }
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(if (settings.reminderEnabled) "Activée" else "Désactivée")
                }
                Text("Heure : %02d:%02d".format(settings.reminderHour, settings.reminderMinute))
                TextButton(onClick = {
                    TimePickerDialog(
                        context,
                        { _, h, m -> changeReminder(true, h, m) },
                        settings.reminderHour,
                        settings.reminderMinute,
                        true
                    ).show()
                }) { Text("Changer l’heure") }
            }}
        }
    }
}
