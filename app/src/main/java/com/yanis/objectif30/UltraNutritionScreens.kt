package com.yanis.objectif30

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yanis.objectif30.data.*
import com.yanis.objectif30.ui.*
import kotlin.math.roundToInt

data class ShoppingLine(
    val key: String,
    val display: String,
    val searchQuery: String
)

@Composable
fun UltraNutritionScreen(
    settings: UserSettings,
    modifier: Modifier = Modifier,
    onPlanChanged: () -> Unit = {}
) {
    val context = LocalContext.current
    val prefs = remember { WeeklyNutritionPreferences(context) }
    var refresh by remember { mutableIntStateOf(0) }
    var selectedDay by remember { mutableIntStateOf(premiumTodayIndexForUltra()) }
    var pickerSlot by remember { mutableStateOf<MealSlot?>(null) }

    val meals = remember(selectedDay, refresh) {
        MealSlot.entries.map { slot ->
            WeeklyNutritionRepository.selected(selectedDay, slot, prefs)
        }
    }
    val calories = meals.sumOf { it.kcal }
    val protein = meals.sumOf { it.protein }
    val fiber = meals.sumOf { it.fiber }
    val timing = NutritionRepository.timing(settings.trainingHour, settings.trainingMinute)
    val dayName = PlanRepository.day(selectedDay).name

    Column(modifier.fillMaxSize()) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(PlanRepository.days.indices.toList()) { index ->
                val day = PlanRepository.days[index]
                FilterChip(
                    selected = selectedDay == index,
                    onClick = { selectedDay = index },
                    label = { Text(day.name) },
                    shape = RoundedCornerShape(14.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = WildPanel2,
                        labelColor = MaterialTheme.colorScheme.onSurface,
                        selectedContainerColor = WildCyan,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selectedDay == index,
                        borderColor = WildLine,
                        selectedBorderColor = WildCyan,
                        borderWidth = 1.dp,
                        selectedBorderWidth = 1.dp
                    )
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                WildEyebrow("Nutrition")
                Text(
                    "Semaine alimentaire",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Black
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WildTag("MODE ÉCO", WildGreen)
                    WildTag("28 REPAS", WildViolet)
                }
                Spacer(Modifier.height(12.dp))
                WildGlassCard(highlighted = true) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            WildEyebrow("Jour sélectionné")
                            Text(
                                dayName,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Black
                            )
                        }
                        WildTag("MODIFIABLE", WildAmber)
                    }
                    Text(
                        "≈ " + calories + " kcal • " + protein +
                            " g protéines • " + fiber + " g fibres",
                        color = WildCyan,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        WeeklyNutritionRepository.recipeCount().toString() +
                            " recettes disponibles. Chaque changement sur cette journée met automatiquement à jour la liste de courses de toute la semaine.",
                        color = WildMuted,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            items(meals) { meal ->
                ElevatedCard(shape = RoundedCornerShape(22.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            (timing[meal.slot] ?: "--:--") + " • " + meal.slot.label,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            meal.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black
                        )
                        Text(meal.description)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            meal.kcal.toString() + " kcal • " +
                                meal.protein + " g prot • " +
                                meal.carbs + " g gluc • " +
                                meal.fiber + " g fibres"
                        )
                        Text(
                            meal.cookMinutes.toString() + " min • budget " + meal.costLabel,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(Modifier.height(10.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            meal.ingredients.forEach { ingredient ->
                                SuggestionChip(
                                    onClick = {},
                                    label = { Text(ingredient) }
                                )
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Button(
                            onClick = { pickerSlot = meal.slot },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("🔄 Choisir une autre recette équivalente")
                        }
                    }
                }
            }

            item {
                ElevatedCard(shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("🧠 Anti-faim", fontWeight = FontWeight.Bold)
                        Text(
                            "Chaque journée garde 4 prises alimentaires avec protéines, fibres et volume. Les jours d’entraînement, la collation est placée environ 2 h avant la séance. Les jours calmes, tu peux la décaler ou la réduire si tu n’as réellement pas faim."
                        )
                    }
                }
            }

            item {
                OutlinedButton(
                    onClick = {
                        prefs.resetWeek()
                        refresh++
                        onPlanChanged()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("↺ Revenir au menu hebdomadaire proposé")
                }
            }
        }
    }

    val openSlot = pickerSlot
    if (openSlot != null) {
        val choices = WeeklyNutritionRepository.forSlot(openSlot)
        AlertDialog(
            onDismissRequest = { pickerSlot = null },
            title = { Text("Choisir • " + openSlot.label) },
            text = {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 520.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(choices) { option ->
                        OutlinedCard(
                            onClick = {
                                val index = choices.indexOf(option)
                                prefs.setChoice(selectedDay, openSlot, index)
                                refresh++
                                onPlanChanged()
                                pickerSlot = null
                            },
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text(option.name, fontWeight = FontWeight.Bold)
                                Text(option.description, style = MaterialTheme.typography.bodySmall)
                                Text(
                                    option.kcal.toString() + " kcal • " +
                                        option.protein + " g prot • " +
                                        option.fiber + " g fibres",
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { pickerSlot = null }) { Text("Fermer") }
            }
        )
    }
}

@Composable
fun UltraShoppingScreen(
    modifier: Modifier = Modifier,
    planRevision: Int = 0
) {
    val context = LocalContext.current
    val weeklyPrefs = remember { WeeklyNutritionPreferences(context) }
    val checklistPrefs = remember { ShoppingChecklistPreferences(context) }
    var refresh by remember { mutableIntStateOf(0) }

    val lines = remember(refresh, planRevision) {
        aggregateWeekIngredients(
            WeeklyNutritionRepository.weekMeals(weeklyPrefs).flatMap { it.second.ingredients }
        )
    }
    val checkedCount = lines.count { checklistPrefs.isChecked(it.key) }
    val remaining = lines.size - checkedCount

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            WildEyebrow("Budget courses")
            Text(
                "Panier de la semaine",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black
            )
            Text(
                "Synchronisée avec tes 7 journées : si tu changes une recette, les quantités à acheter sont recalculées automatiquement.",
                color = WildMuted
            )
            Spacer(Modifier.height(12.dp))
            WildGlassCard(highlighted = true) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            checkedCount.toString() + "/" + lines.size,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black
                        )
                        Text("articles traités", color = WildMuted)
                    }
                    WildTag(remaining.toString() + " RESTANTS", WildAmber)
                }
                LinearProgressIndicator(
                    progress = {
                        if (lines.isEmpty()) 0f else checkedCount.toFloat() / lines.size.toFloat()
                    },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = WildCyan,
                    trackColor = WildLine
                )
            }
        }

        item {
            ElevatedCard(shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("🛒 WildCart hebdomadaire • mode éco", fontWeight = FontWeight.Bold)
                    Text(
                        "La semaine économique est construite autour d’un petit noyau d’aliments réutilisés. Coche ce que tu as déjà : WildCart n’enverra que ce qu’il reste à acheter vers E.Leclerc Istres."
                    )
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = {
                            val pending = lines
                                .filter { !checklistPrefs.isChecked(it.key) }
                                .map { it.display }
                            val intent = Intent(context, LeclercActivity::class.java).apply {
                                putStringArrayListExtra(
                                    LeclercActivity.EXTRA_INGREDIENTS,
                                    ArrayList(pending)
                                )
                            }
                            context.startActivity(intent)
                        },
                        enabled = remaining > 0,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("⚡ Envoyer " + remaining + " articles vers E.Leclerc")
                    }
                }
            }
        }

        items(lines, key = { it.key }) { line ->
            val checked = checklistPrefs.isChecked(line.key)
            ElevatedCard(shape = RoundedCornerShape(18.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = checked,
                        onCheckedChange = { value ->
                            checklistPrefs.setChecked(line.key, value)
                            refresh++
                        }
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            line.display,
                            fontWeight = if (checked) FontWeight.Normal else FontWeight.SemiBold
                        )
                        Text(
                            if (checked) "Déjà à la maison / acheté" else "À acheter",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (checked) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.primary
                            }
                        )
                    }
                }
            }
        }

        item {
            OutlinedButton(
                onClick = {
                    checklistPrefs.clear()
                    refresh++
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Tout décocher")
            }
        }
    }
}

private fun premiumTodayIndexForUltra(): Int =
    java.time.LocalDate.now().dayOfWeek.value - 1

private fun aggregateWeekIngredients(rawIngredients: List<String>): List<ShoppingLine> {
    data class Acc(
        var amount: Double = 0.0,
        val unit: String,
        val name: String,
        var count: Int = 0
    )

    val map = linkedMapOf<String, Acc>()
    val numeric = Regex(
        "^\\s*(\\d+(?:[.,]\\d+)?)\\s*(g|kg|ml|cl|l)?\\s+(.+)$",
        RegexOption.IGNORE_CASE
    )

    rawIngredients.forEach { raw ->
        val clean = raw.trim()
        val match = numeric.find(clean)
        if (match != null) {
            var amount = match.groupValues[1].replace(',', '.').toDoubleOrNull() ?: 0.0
            var unit = match.groupValues[2].lowercase()
            val name = match.groupValues[3].trim()
            if (unit.isBlank()) {
                unit = "p"
            } else if (unit == "kg") {
                amount *= 1000.0
                unit = "g"
            } else if (unit == "l") {
                amount *= 1000.0
                unit = "ml"
            } else if (unit == "cl") {
                amount *= 10.0
                unit = "ml"
            }
            val key = unit + "|" + name.lowercase()
            val acc = map.getOrPut(key) { Acc(0.0, unit, name, 0) }
            acc.amount += amount
            acc.count++
        } else {
            val key = "x|" + clean.lowercase()
            val acc = map.getOrPut(key) { Acc(0.0, "", clean, 0) }
            acc.count++
        }
    }

    return map.map { (key, acc) ->
        val display = when (acc.unit) {
            "g" -> {
                if (acc.amount >= 1000.0) {
                    val kg = acc.amount / 1000.0
                    val value = if (kg % 1.0 == 0.0) kg.roundToInt().toString()
                    else "%.1f".format(kg)
                    value + " kg " + acc.name
                } else {
                    acc.amount.roundToInt().toString() + " g " + acc.name
                }
            }
            "ml" -> {
                if (acc.amount >= 1000.0) {
                    val liters = acc.amount / 1000.0
                    val value = if (liters % 1.0 == 0.0) liters.roundToInt().toString()
                    else "%.1f".format(liters)
                    value + " L " + acc.name
                } else {
                    acc.amount.roundToInt().toString() + " ml " + acc.name
                }
            }
            "p" -> acc.amount.roundToInt().toString() + " " + acc.name
            "" -> {
                if (acc.count > 1) acc.count.toString() + " × " + acc.name else acc.name
            }
            else -> acc.amount.roundToInt().toString() + " " + acc.unit + " " + acc.name
        }

        val versionedKey = key + "|" +
            (if (acc.unit.isNotBlank()) "%.2f".format(acc.amount) else acc.count.toString())

        ShoppingLine(
            key = versionedKey,
            display = display,
            searchQuery = acc.name
        )
    }.sortedBy { it.searchQuery.lowercase() }
}
