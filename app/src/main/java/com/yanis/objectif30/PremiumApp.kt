package com.yanis.objectif30

import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
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
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.max

enum class PremiumTab(val label: String, val emoji: String) {
    TODAY("Aujourd’hui", "🥊"),
    WEEK("Semaine", "📅"),
    MEALS("Repas", "🍲"),
    PERF("Perf", "📈"),
    SHOPPING("Courses", "🛒")
}

@Composable
fun PremiumApp(prefs: UserPreferences) {
    val settings by prefs.settings.collectAsState(initial = UserSettings())
    var tab by remember { mutableStateOf(PremiumTab.TODAY) }
    val context = LocalContext.current
    val activity = context as? ComponentActivity

    Scaffold(
        bottomBar = {
            NavigationBar {
                PremiumTab.entries.forEach { item ->
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
            PremiumTab.TODAY -> PremiumToday(settings, Modifier.padding(pad))
            PremiumTab.WEEK -> PremiumWeek(Modifier.padding(pad))
            PremiumTab.MEALS -> PremiumMeals(
                settings = settings,
                modifier = Modifier.padding(pad),
                onSwap = { slot, index ->
                    activity?.lifecycleScope?.launch { prefs.setMealChoice(slot, index) }
                },
                onTrainingTime = { h, m ->
                    activity?.lifecycleScope?.launch { prefs.setTrainingTime(h, m) }
                }
            )
            PremiumTab.PERF -> PremiumPerformance(
                settings = settings,
                modifier = Modifier.padding(pad),
                saveWeight = { value ->
                    activity?.lifecycleScope?.launch { prefs.setWeight(value) }
                },
                savePerformance = { pull, push, run, shadow, energy, hunger ->
                    activity?.lifecycleScope?.launch {
                        prefs.setPerformance(pull, push, run, shadow, energy, hunger)
                    }
                },
                changeReminder = { enabled, h, m ->
                    activity?.lifecycleScope?.launch {
                        prefs.setReminder(enabled, h, m)
                        if (enabled) ReminderScheduler.schedule(context, h, m)
                        else ReminderScheduler.cancel(context)
                    }
                }
            )
            PremiumTab.SHOPPING -> PremiumShopping(settings, Modifier.padding(pad))
        }
    }
}

private fun premiumTodayIndex(): Int = LocalDate.now().dayOfWeek.value - 1

private fun premiumChoice(settings: UserSettings, slot: MealSlot): Int = when (slot) {
    MealSlot.BREAKFAST -> settings.breakfastChoice
    MealSlot.LUNCH -> settings.lunchChoice
    MealSlot.SNACK -> settings.snackChoice
    MealSlot.DINNER -> settings.dinnerChoice
}

private fun premiumMeals(settings: UserSettings): List<MealOption> =
    MealSlot.entries.map { slot ->
        NutritionRepository.selected(slot, premiumChoice(settings, slot))
    }

@Composable
fun PremiumToday(settings: UserSettings, modifier: Modifier = Modifier) {
    val plan = PlanRepository.day(premiumTodayIndex())
    val meals = premiumMeals(settings)
    val timing = NutritionRepository.timing(settings.trainingHour, settings.trainingMinute)
    val protein = meals.sumOf { it.protein }
    val calories = meals.sumOf { it.kcal }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "WILDSPORT • COACH PERSONNEL",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Text(plan.name, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black)
            Text(plan.focus, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = {}, label = { Text("⚡ " + plan.intensity) })
                AssistChip(onClick = {}, label = { Text("⚖️ " + "%.1f".format(settings.currentWeight) + " kg") })
            }
        }

        item {
            ElevatedCard {
                Column(Modifier.padding(16.dp)) {
                    Text("🎯 Tableau de bord", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(6.dp))
                    Text("Énergie " + settings.energyScore + "/10 • Faim " + settings.hungerScore + "/10")
                    Text("Tractions " + settings.pullUps + " • Pompes " + settings.pushUps)
                    Text("Course facile " + settings.easyRunMinutes + " min • Shadow " + settings.shadowRounds + " rounds")
                    Spacer(Modifier.height(8.dp))
                    val advice = when {
                        settings.hungerScore >= 7 ->
                            "Faim haute : conserve les 4 repas et augmente d’abord le volume de légumes/soupe avant de réduire les portions."
                        settings.energyScore <= 4 ->
                            "Énergie basse : garde les féculents autour de la séance et n’accentue pas le déficit cette semaine."
                        else ->
                            "Zone stable : garde la structure et juge la tendance sur plusieurs semaines."
                    }
                    Text(advice, color = MaterialTheme.colorScheme.secondary)
                }
            }
        }

        item {
            Text("🏋️ Séance du jour", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }

        items(plan.exercises) { ex ->
            ElevatedCard {
                Column(Modifier.padding(16.dp)) {
                    Text(ex.name, fontWeight = FontWeight.Bold)
                    Text(ex.prescription)
                    Spacer(Modifier.height(6.dp))
                    Text("Alternative : " + ex.replacement, color = MaterialTheme.colorScheme.secondary)
                    if (ex.note.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(ex.note, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        item {
            Text("🍲 Plan alimentaire", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                "≈ " + calories + " kcal • ≈ " + protein + " g protéines. Repères, pas obligation au gramme près.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        items(meals) { meal ->
            ElevatedCard {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        (timing[meal.slot] ?: "--:--") + " • " + meal.slot.label,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(meal.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(meal.description)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        meal.protein.toString() + " g prot • " +
                            meal.carbs + " g gluc • " +
                            meal.fiber + " g fibres • " +
                            meal.kcal + " kcal"
                    )
                }
            }
        }

        item {
            ElevatedCard {
                Column(Modifier.padding(16.dp)) {
                    Text("🧠 Stratégie anti-faim", fontWeight = FontWeight.Bold)
                    Text(
                        "Objectif : réduire fortement la faim, pas promettre zéro faim. Protéines à chaque repas, gros volume de légumes, féculents autour de l’entraînement et repas espacés d’environ 3 à 4 heures."
                    )
                }
            }
        }
    }
}

@Composable
fun PremiumWeek(modifier: Modifier = Modifier) {
    var selected by remember { mutableIntStateOf(premiumTodayIndex()) }

    Column(modifier.fillMaxSize()) {
        ScrollableTabRow(selectedTabIndex = selected) {
            PlanRepository.days.forEachIndexed { index, day ->
                Tab(
                    selected = selected == index,
                    onClick = { selected = index },
                    text = { Text(day.name.take(3)) }
                )
            }
        }

        val plan = PlanRepository.day(selected)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(plan.name, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black)
                Text(plan.focus)
            }
            items(plan.exercises) { ex ->
                ElevatedCard {
                    Column(Modifier.padding(16.dp)) {
                        Text(ex.name, fontWeight = FontWeight.Bold)
                        Text(ex.prescription)
                        Text("Alternative : " + ex.replacement, color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }
            item {
                ElevatedCard {
                    Column(Modifier.padding(16.dp)) {
                        Text("😴 Récupération", fontWeight = FontWeight.Bold)
                        Text(plan.recovery)
                    }
                }
            }
        }
    }
}

@Composable
fun PremiumMeals(
    settings: UserSettings,
    modifier: Modifier = Modifier,
    onSwap: (MealSlot, Int) -> Unit,
    onTrainingTime: (Int, Int) -> Unit
) {
    val context = LocalContext.current
    val timing = NutritionRepository.timing(settings.trainingHour, settings.trainingMinute)
    val meals = premiumMeals(settings)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Nutrition Premium", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black)
            Text("Repas chauds, satiété, timing autour de tes entraînements et équivalences instantanées.")
        }

        item {
            ElevatedCard {
                Column(Modifier.padding(16.dp)) {
                    Text("⏱ Heure habituelle d’entraînement", fontWeight = FontWeight.Bold)
                    Text(
                        "%02d:%02d".format(settings.trainingHour, settings.trainingMinute),
                        style = MaterialTheme.typography.headlineSmall
                    )
                    TextButton(
                        onClick = {
                            TimePickerDialog(
                                context,
                                { _, h, m -> onTrainingTime(h, m) },
                                settings.trainingHour,
                                settings.trainingMinute,
                                true
                            ).show()
                        }
                    ) {
                        Text("Changer l’heure")
                    }
                }
            }
        }

        items(meals) { meal ->
            val list = NutritionRepository.forSlot(meal.slot)
            val current = premiumChoice(settings, meal.slot).coerceIn(0, list.lastIndex)

            ElevatedCard {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        (timing[meal.slot] ?: "--:--") + " • " + meal.slot.label,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(meal.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                    Text(meal.description)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        meal.kcal.toString() + " kcal • " +
                            meal.protein + " g prot • " +
                            meal.fiber + " g fibres • " +
                            meal.cookMinutes + " min • " + meal.costLabel
                    )
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = {
                            val next = (current + 1) % list.size
                            onSwap(meal.slot, next)
                        }
                    ) {
                        Text("🔄 Changer pour un équivalent")
                    }
                    Text(
                        "Le remplacement reste proche en protéines, énergie et satiété.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        item {
            ElevatedCard {
                Column(Modifier.padding(16.dp)) {
                    Text("🛟 Plan anti-faim", fontWeight = FontWeight.Bold)
                    Text(
                        "Si tu as faim entre les repas : commence par eau, soupe ou légumes. Si la faim persiste, ajoute une petite portion protéinée comme skyr, fromage blanc ou œufs plutôt qu’un grignotage sucré."
                    )
                }
            }
        }
    }
}

@Composable
fun PremiumPerformance(
    settings: UserSettings,
    modifier: Modifier = Modifier,
    saveWeight: (Double) -> Unit,
    savePerformance: (Int, Int, Int, Int, Int, Int) -> Unit,
    changeReminder: (Boolean, Int, Int) -> Unit
) {
    val context = LocalContext.current
    var weight by remember(settings.currentWeight) { mutableStateOf("%.1f".format(settings.currentWeight)) }
    var pull by remember(settings.pullUps) { mutableStateOf(settings.pullUps.toString()) }
    var push by remember(settings.pushUps) { mutableStateOf(settings.pushUps.toString()) }
    var run by remember(settings.easyRunMinutes) { mutableStateOf(settings.easyRunMinutes.toString()) }
    var shadow by remember(settings.shadowRounds) { mutableStateOf(settings.shadowRounds.toString()) }
    var energy by remember(settings.energyScore) { mutableFloatStateOf(settings.energyScore.toFloat()) }
    var hunger by remember(settings.hungerScore) { mutableFloatStateOf(settings.hungerScore.toFloat()) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Mes performances", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black)
            Text("Poids + force + cardio + boxe + énergie + faim.")
        }

        item {
            ElevatedCard {
                Column(Modifier.padding(16.dp)) {
                    Text("⚖️ Poids", fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = weight,
                        onValueChange = { weight = it.replace(',', '.') },
                        label = { Text("Poids actuel (kg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { weight.toDoubleOrNull()?.let(saveWeight) }) {
                        Text("Enregistrer")
                    }
                    Spacer(Modifier.height(10.dp))
                    val total = max(0.1, 90.0 - settings.targetWeight)
                    val done = (90.0 - settings.currentWeight).coerceIn(0.0, total)
                    LinearProgressIndicator(
                        progress = { (done / total).toFloat() },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "%.1f".format(max(0.0, settings.currentWeight - settings.targetWeight)) +
                            " kg jusqu’à 80 kg"
                    )
                }
            }
        }

        item {
            ElevatedCard {
                Column(Modifier.padding(16.dp)) {
                    Text("🏋️ Marqueurs personnels", fontWeight = FontWeight.Bold)
                    PremiumMetricField("Tractions propres max", pull) { pull = it }
                    PremiumMetricField("Pompes propres max", push) { push = it }
                    PremiumMetricField("Course facile tenue (min)", run) { run = it }
                    PremiumMetricField("Rounds de shadow propres", shadow) { shadow = it }

                    Spacer(Modifier.height(12.dp))
                    Text("Énergie : " + energy.toInt() + "/10")
                    Slider(
                        value = energy,
                        onValueChange = { energy = it },
                        valueRange = 1f..10f,
                        steps = 8
                    )
                    Text("Faim moyenne : " + hunger.toInt() + "/10")
                    Slider(
                        value = hunger,
                        onValueChange = { hunger = it },
                        valueRange = 1f..10f,
                        steps = 8
                    )

                    Button(
                        onClick = {
                            savePerformance(
                                pull.toIntOrNull() ?: settings.pullUps,
                                push.toIntOrNull() ?: settings.pushUps,
                                run.toIntOrNull() ?: settings.easyRunMinutes,
                                shadow.toIntOrNull() ?: settings.shadowRounds,
                                energy.toInt(),
                                hunger.toInt()
                            )
                        }
                    ) {
                        Text("Sauvegarder mes performances")
                    }
                }
            }
        }

        item {
            ElevatedCard {
                Column(Modifier.padding(16.dp)) {
                    Text("🔔 Briefing du soir", fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(
                            checked = settings.reminderEnabled,
                            onCheckedChange = {
                                changeReminder(it, settings.reminderHour, settings.reminderMinute)
                            }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(if (settings.reminderEnabled) "Activé" else "Désactivé")
                    }
                    Text(
                        "Heure : " + "%02d:%02d".format(
                            settings.reminderHour,
                            settings.reminderMinute
                        )
                    )
                    TextButton(
                        onClick = {
                            TimePickerDialog(
                                context,
                                { _, h, m -> changeReminder(true, h, m) },
                                settings.reminderHour,
                                settings.reminderMinute,
                                true
                            ).show()
                        }
                    ) {
                        Text("Changer l’heure")
                    }
                }
            }
        }
    }
}

@Composable
private fun PremiumMetricField(
    label: String,
    value: String,
    onChange: (String) -> Unit
) {
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(
        value = value,
        onValueChange = { onChange(it.filter(Char::isDigit)) },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun PremiumShopping(settings: UserSettings, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val meals = premiumMeals(settings)
    val ingredientCount = meals.sumOf { it.ingredients.size }

    val shoppingText = buildString {
        appendLine("WILDSPORT — Liste de courses")
        appendLine()
        meals.forEach { meal ->
            appendLine(meal.slot.label + " — " + meal.name)
            meal.ingredients.forEach { ingredient ->
                appendLine("• " + ingredient)
            }
            appendLine()
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Courses", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black)
            Text("Liste générée automatiquement depuis tes repas choisis.")
        }

        items(meals) { meal ->
            ElevatedCard {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        meal.name,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    meal.ingredients.forEach { ingredient ->
                        Text("☐ " + ingredient)
                    }
                }
            }
        }

        item {
            Button(
                onClick = {
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "Liste de courses Wildsport")
                        putExtra(Intent.EXTRA_TEXT, shoppingText)
                    }
                    context.startActivity(
                        Intent.createChooser(send, "Partager la liste de courses")
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("📤 Partager ma liste")
            }
        }

        item {
            Button(
                onClick = {
                    val intent = Intent(context, LeclercActivity::class.java).apply {
                        putStringArrayListExtra(
                            LeclercActivity.EXTRA_INGREDIENTS,
                            ArrayList(meals.flatMap { it.ingredients })
                        )
                    }
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("⚡ WildCart • connecter E.Leclerc")
            }
        }

        item {
            OutlinedButton(
                onClick = {
                    val launch = context.packageManager
                        .getLaunchIntentForPackage("com.wishop.dev.jow")
                    if (launch != null) {
                        context.startActivity(launch)
                    } else {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://jow.fr/"))
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("🛒 Ouvrir Jow")
            }
        }

        item {
            ElevatedCard {
                Column(Modifier.padding(16.dp)) {
                    Text("Connexion Jow", fontWeight = FontWeight.Bold)
                    Text(
                        "L’application peut préparer ta liste, la partager et ouvrir Jow. Le remplissage automatique du panier Jow demanderait une API officielle/partenaire ; aucune API publique documentée n’est disponible actuellement."
                    )
                }
            }
        }

        item {
            Text(
                ingredientCount.toString() + " lignes d’ingrédients",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
