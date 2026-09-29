package com.yanis.objectif30

import android.app.TimePickerDialog
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.yanis.objectif30.data.*
import com.yanis.objectif30.ui.*
import kotlin.math.max

@Composable
fun WildTodayPremium(settings: UserSettings, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val dayIndex = java.time.LocalDate.now().dayOfWeek.value - 1
    val plan = PlanRepository.day(dayIndex)
    val weeklyPrefs = remember { WeeklyNutritionPreferences(context) }
    val meals = remember(dayIndex) {
        MealSlot.entries.map { slot ->
            WeeklyNutritionRepository.selected(dayIndex, slot, weeklyPrefs)
        }
    }
    val calories = meals.sumOf { it.kcal }
    val protein = meals.sumOf { it.protein }
    val fiber = meals.sumOf { it.fiber }

    WildBackdrop(modifier) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp, 22.dp, 16.dp, 120.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        WildEyebrow("Wildsport")
                        Text(
                            "Ton cockpit",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Black
                        )
                    }
                    WildTag("ULTRA", WildViolet)
                }
            }

            item {
                WildGlassCard(highlighted = true) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                plan.name,
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Black
                            )
                            Text(plan.focus, color = WildMuted)
                        }
                        WildTag(plan.intensity, WildGreen)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        WildTag("%.1f kg".format(settings.currentWeight))
                        WildTag("Énergie " + settings.energyScore + "/10", WildAmber)
                    }
                    WildPrimaryButton(
                        text = "▶  Lancer Wildsport Run",
                        onClick = {
                            context.startActivity(
                                Intent(context, RunTrackerActivity::class.java)
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            item {
                WildSectionTitle("Vue rapide", "Les données utiles, sans bruit")
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        WildMetricTile(
                            "Poids",
                            "%.1f kg".format(settings.currentWeight),
                            "Objectif " + settings.targetWeight.toInt() + " kg",
                            modifier = Modifier.weight(1f)
                        )
                        WildMetricTile(
                            "Faim",
                            settings.hungerScore.toString() + "/10",
                            if (settings.hungerScore >= 7) "À surveiller" else "Stable",
                            if (settings.hungerScore >= 7) WildAmber else WildGreen,
                            Modifier.weight(1f)
                        )
                    }
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        WildMetricTile(
                            "Protéines",
                            protein.toString() + " g",
                            "plan du jour",
                            WildViolet,
                            Modifier.weight(1f)
                        )
                        WildMetricTile(
                            "Fibres",
                            fiber.toString() + " g",
                            "satiété",
                            WildGreen,
                            Modifier.weight(1f)
                        )
                    }
                }
            }

            item {
                val advice = when {
                    settings.hungerScore >= 7 ->
                        "Faim élevée : garde tes 4 prises et augmente d'abord légumes, soupe et fibres."
                    settings.energyScore <= 4 ->
                        "Énergie basse : garde les féculents autour de la séance et ne réduis pas davantage aujourd'hui."
                    else ->
                        "Bon équilibre aujourd'hui : garde ce rythme et juge les progrès sur plusieurs semaines."
                }
                WildGlassCard {
                    WildEyebrow("Coach adaptatif")
                    Text(advice, style = MaterialTheme.typography.bodyLarge)
                }
            }

            item {
                WildSectionTitle(
                    "Séance",
                    plan.exercises.size.toString() + " blocs aujourd'hui"
                )
            }

            items(plan.exercises) { ex ->
                val index = plan.exercises.indexOf(ex) + 1
                WildGlassCard {
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = WildCyan.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, WildCyan.copy(alpha = 0.35f))
                        ) {
                            Box(
                                Modifier.size(38.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    index.toString().padStart(2, '0'),
                                    color = WildCyan,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                        Column(Modifier.weight(1f)) {
                            Text(
                                ex.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black
                            )
                            Text(ex.prescription, color = WildMuted)
                            Text(
                                "Alternative • " + ex.replacement,
                                color = WildBlue,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            item {
                WildSectionTitle(
                    "Nutrition du jour",
                    "≈ " + calories + " kcal • " + protein + " g prot • " + fiber + " g fibres"
                )
            }

            items(meals) { meal ->
                WildGlassCard {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            WildEyebrow(meal.slot.label)
                            Text(
                                meal.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black
                            )
                            Text(meal.description, color = WildMuted)
                        }
                        Spacer(Modifier.width(10.dp))
                        WildTag(meal.protein.toString() + " g", WildGreen)
                    }
                }
            }
        }
    }
}

@Composable
fun WildWeekPremium(modifier: Modifier = Modifier) {
    var selected by remember {
        mutableIntStateOf(java.time.LocalDate.now().dayOfWeek.value - 1)
    }
    val plan = PlanRepository.day(selected)

    WildBackdrop(modifier) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp, 22.dp, 16.dp, 120.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                WildEyebrow("Programme")
                Text(
                    "Ta semaine",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Black
                )
                Text(
                    "Lisible d'un coup d'œil, ajustable jour par jour.",
                    color = WildMuted
                )
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(PlanRepository.days.indices.toList()) { index ->
                        FilterChip(
                            selected = selected == index,
                            onClick = { selected = index },
                            label = { Text(PlanRepository.days[index].name.take(3)) },
                            shape = RoundedCornerShape(14.dp)
                        )
                    }
                }
            }

            item {
                WildGlassCard(highlighted = true) {
                    WildEyebrow("Jour " + (selected + 1))
                    Text(
                        plan.name,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Black
                    )
                    Text(plan.focus, color = WildMuted)
                    WildTag(plan.intensity, WildGreen)
                }
            }

            item {
                WildSectionTitle(
                    "Déroulé",
                    plan.exercises.size.toString() + " blocs"
                )
            }

            items(plan.exercises) { ex ->
                WildGlassCard {
                    Text(
                        ex.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black
                    )
                    Text(ex.prescription)
                    Text(
                        "Alternative • " + ex.replacement,
                        color = WildBlue,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            item {
                WildGlassCard {
                    WildEyebrow("Récupération")
                    Text(plan.recovery, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

@Composable
fun WildPerformancePremium(
    settings: UserSettings,
    modifier: Modifier = Modifier,
    saveWeight: (Double) -> Unit,
    savePerformance: (Int, Int, Int, Int, Int, Int) -> Unit,
    changeReminder: (Boolean, Int, Int) -> Unit
) {
    val context = LocalContext.current
    val runPrefs = remember { RunPreferences(context) }
    var weight by remember(settings.currentWeight) {
        mutableStateOf("%.1f".format(settings.currentWeight))
    }
    var pull by remember(settings.pullUps) { mutableStateOf(settings.pullUps.toString()) }
    var push by remember(settings.pushUps) { mutableStateOf(settings.pushUps.toString()) }
    var run by remember(settings.easyRunMinutes) { mutableStateOf(settings.easyRunMinutes.toString()) }
    var shadow by remember(settings.shadowRounds) { mutableStateOf(settings.shadowRounds.toString()) }
    var energy by remember(settings.energyScore) { mutableFloatStateOf(settings.energyScore.toFloat()) }
    var hunger by remember(settings.hungerScore) { mutableFloatStateOf(settings.hungerScore.toFloat()) }

    val total = max(0.1, 90.0 - settings.targetWeight)
    val done = (90.0 - settings.currentWeight).coerceIn(0.0, total)
    val progress = (done / total).toFloat()

    WildBackdrop(modifier) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp, 22.dp, 16.dp, 120.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                WildEyebrow("Progression")
                Text(
                    "Tes performances",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Black
                )
                Text(
                    "Force, cardio, poids, énergie et faim dans un même cockpit.",
                    color = WildMuted
                )
            }

            item {
                WildGlassCard(highlighted = true) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            WildEyebrow("Poids actuel")
                            Text(
                                "%.1f kg".format(settings.currentWeight),
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Black
                            )
                        }
                        WildTag(settings.targetWeight.toInt().toString() + " kg cible", WildGreen)
                    }
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                        color = WildCyan,
                        trackColor = WildLine
                    )
                    Text(
                        "%.1f kg restants".format(
                            max(0.0, settings.currentWeight - settings.targetWeight)
                        ),
                        color = WildMuted
                    )
                    OutlinedTextField(
                        value = weight,
                        onValueChange = { weight = it.replace(',', '.') },
                        label = { Text("Mettre à jour le poids") },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    )
                    WildPrimaryButton(
                        "Enregistrer le poids",
                        { weight.toDoubleOrNull()?.let(saveWeight) },
                        Modifier.fillMaxWidth()
                    )
                }
            }

            item {
                WildSectionTitle(
                    "Dernière course",
                    if (runPrefs.lastSeconds() > 0) "session enregistrée" else "aucune session"
                )
            }

            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    WildMetricTile(
                        "Distance",
                        "%.2f km".format(runPrefs.lastDistanceKm()),
                        accent = WildBlue,
                        modifier = Modifier.weight(1f)
                    )
                    WildMetricTile(
                        "Vitesse moy.",
                        "%.1f km/h".format(runPrefs.lastAvgKmh()),
                        accent = WildGreen,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    WildMetricTile(
                        "Pas",
                        runPrefs.lastSteps().toString(),
                        accent = WildViolet,
                        modifier = Modifier.weight(1f)
                    )
                    WildMetricTile(
                        "Durée",
                        "%02d:%02d".format(
                            runPrefs.lastSeconds() / 60,
                            runPrefs.lastSeconds() % 60
                        ),
                        accent = WildAmber,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                WildSectionTitle(
                    "Marqueurs personnels",
                    "Mets-les à jour quand tu progresses"
                )
            }

            item {
                WildGlassCard {
                    WildMetricInput("Tractions propres max", pull) { pull = it }
                    WildMetricInput("Pompes propres max", push) { push = it }
                    WildMetricInput("Course facile tenue (min)", run) { run = it }
                    WildMetricInput("Rounds de shadow propres", shadow) { shadow = it }

                    Text("Énergie • " + energy.toInt() + "/10", fontWeight = FontWeight.Bold)
                    Slider(
                        value = energy,
                        onValueChange = { energy = it },
                        valueRange = 1f..10f,
                        steps = 8
                    )
                    Text("Faim • " + hunger.toInt() + "/10", fontWeight = FontWeight.Bold)
                    Slider(
                        value = hunger,
                        onValueChange = { hunger = it },
                        valueRange = 1f..10f,
                        steps = 8
                    )
                    WildPrimaryButton(
                        "Sauvegarder mes performances",
                        {
                            savePerformance(
                                pull.toIntOrNull() ?: settings.pullUps,
                                push.toIntOrNull() ?: settings.pushUps,
                                run.toIntOrNull() ?: settings.easyRunMinutes,
                                shadow.toIntOrNull() ?: settings.shadowRounds,
                                energy.toInt(),
                                hunger.toInt()
                            )
                        },
                        Modifier.fillMaxWidth()
                    )
                }
            }

            item {
                WildGlassCard {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            WildEyebrow("Briefing")
                            Text(
                                "Notification du soir",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                "%02d:%02d".format(
                                    settings.reminderHour,
                                    settings.reminderMinute
                                ),
                                color = WildMuted
                            )
                        }
                        Switch(
                            checked = settings.reminderEnabled,
                            onCheckedChange = {
                                changeReminder(
                                    it,
                                    settings.reminderHour,
                                    settings.reminderMinute
                                )
                            }
                        )
                    }
                    WildSecondaryButton(
                        "Changer l'heure",
                        {
                            TimePickerDialog(
                                context,
                                { _, h, m -> changeReminder(true, h, m) },
                                settings.reminderHour,
                                settings.reminderMinute,
                                true
                            ).show()
                        },
                        Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun WildMetricInput(
    label: String,
    value: String,
    onChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onChange(it.filter(Char::isDigit)) },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    )
}
