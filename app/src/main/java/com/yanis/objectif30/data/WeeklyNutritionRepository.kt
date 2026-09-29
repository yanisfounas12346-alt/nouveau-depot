package com.yanis.objectif30.data

import android.content.Context

object WeeklyNutritionRepository {
    /*
     * Les recettes "budget" sont volontairement construites autour d'un petit
     * nombre d'aliments réutilisés toute la semaine. Cela évite d'acheter
     * 25 ingrédients différents pour une seule portion de chaque recette.
     */
    private val budgetRecipes = listOf(
        MealOption(
            "eb1", MealSlot.BREAKFAST, "Porridge éco banane",
            "Avoine chaude au lait, fromage blanc et banane. Très rassasiant pour peu d'ingrédients.",
            555, 38, 78, 10, 10, 8, "€",
            listOf("60 g flocons d’avoine", "250 ml lait demi-écrémé", "200 g fromage blanc", "1 banane")
        ),
        MealOption(
            "eb2", MealSlot.BREAKFAST, "Œufs, pommes de terre & légumes",
            "Petit-déjeuner salé et chaud : œufs, pommes de terre et légumes surgelés.",
            520, 31, 55, 20, 10, 15, "€",
            listOf("3 œufs", "250 g pommes de terre", "200 g légumes surgelés")
        ),
        MealOption(
            "eb3", MealSlot.BREAKFAST, "Porridge pomme-œufs",
            "Porridge pomme-cannelle avec deux œufs à côté pour monter les protéines sans multiplier les achats.",
            565, 39, 72, 15, 11, 10, "€",
            listOf("60 g flocons d’avoine", "200 g fromage blanc", "1 pomme", "2 œufs")
        ),

        MealOption(
            "el1", MealSlot.LUNCH, "Poulet riz légumes batch",
            "Poulet, riz et gros volume de légumes surgelés. Préparable en 3 ou 4 portions.",
            680, 52, 82, 14, 12, 18, "€",
            listOf("160 g blanc de poulet", "80 g riz cru", "300 g légumes surgelés")
        ),
        MealOption(
            "el2", MealSlot.LUNCH, "Thon pommes de terre légumes",
            "Thon au naturel, pommes de terre et légumes. Très simple et sans ingrédient exotique.",
            640, 46, 70, 13, 12, 16, "€",
            listOf("140 g thon au naturel égoutté", "300 g pommes de terre", "300 g légumes surgelés")
        ),
        MealOption(
            "el3", MealSlot.LUNCH, "Poulet pâtes tomate",
            "Pâtes, poulet, tomate et légumes. Sauce simple et ingrédients réutilisés dans la semaine.",
            690, 53, 84, 13, 12, 20, "€",
            listOf("160 g blanc de poulet", "90 g pâtes crues", "200 g tomate concassée", "200 g légumes surgelés")
        ),

        MealOption(
            "es1", MealSlot.SNACK, "Fromage blanc banane avoine",
            "Collation simple et rassasiante, sans produit premium obligatoire.",
            335, 26, 49, 4, 6, 2, "€",
            listOf("250 g fromage blanc", "1 banane", "20 g flocons d’avoine")
        ),
        MealOption(
            "es2", MealSlot.SNACK, "Fromage blanc pomme avoine",
            "Même base économique, avec pomme pour varier sans changer toute la liste de courses.",
            325, 26, 45, 4, 7, 2, "€",
            listOf("250 g fromage blanc", "1 pomme", "20 g flocons d’avoine")
        ),
        MealOption(
            "es3", MealSlot.SNACK, "Œufs & tartines",
            "Deux œufs et pain complet : chaud, salé et très simple quand tu ne veux pas de laitage.",
            330, 22, 32, 14, 5, 7, "€",
            listOf("2 œufs", "2 tranches pain complet")
        ),

        MealOption(
            "ed1", MealSlot.DINNER, "Lentilles œufs tomate",
            "Lentilles, œufs, tomate et légumes : protéines + fibres avec un coût bas.",
            650, 39, 73, 18, 20, 22, "€",
            listOf("80 g lentilles sèches", "3 œufs", "200 g tomate concassée", "250 g légumes surgelés")
        ),
        MealOption(
            "ed2", MealSlot.DINNER, "Poulet riz légumes soir",
            "Le même trio économique que le midi, assaisonné différemment pour limiter les achats.",
            665, 51, 79, 14, 11, 18, "€",
            listOf("160 g blanc de poulet", "80 g riz cru", "300 g légumes surgelés")
        ),
        MealOption(
            "ed3", MealSlot.DINNER, "Thon pâtes tomate",
            "Pâtes, thon, tomate et légumes. Rapide, chaud et basé sur les mêmes produits du placard.",
            655, 47, 79, 12, 12, 18, "€",
            listOf("140 g thon au naturel égoutté", "90 g pâtes crues", "200 g tomate concassée", "250 g légumes surgelés")
        )
    )

    /*
     * Les anciennes recettes restent disponibles comme alternatives pour la
     * variété, mais le planning par défaut utilise les recettes budget ci-dessus.
     */
    val options: List<MealOption> =
        budgetRecipes + NutritionRepository.options

    fun forSlot(slot: MealSlot): List<MealOption> =
        options.filter { it.slot == slot }

    private val budgetWeekIds = mapOf(
        MealSlot.BREAKFAST to listOf("eb1", "eb2", "eb1", "eb3", "eb1", "eb2", "eb1"),
        MealSlot.LUNCH to listOf("el1", "el1", "el2", "el1", "el3", "el2", "el1"),
        MealSlot.SNACK to listOf("es1", "es2", "es1", "es3", "es1", "es2", "es1"),
        MealSlot.DINNER to listOf("ed1", "ed2", "ed1", "ed3", "ed2", "ed1", "ed2")
    )

    fun defaultIndex(dayIndex: Int, slot: MealSlot): Int {
        val list = forSlot(slot)
        val id = budgetWeekIds.getValue(slot)[dayIndex.coerceIn(0, 6)]
        return list.indexOfFirst { it.id == id }.coerceAtLeast(0)
    }

    fun selected(
        dayIndex: Int,
        slot: MealSlot,
        prefs: WeeklyNutritionPreferences
    ): MealOption {
        val list = forSlot(slot)
        val index = prefs.choice(dayIndex, slot, defaultIndex(dayIndex, slot))
        return list[index.coerceIn(0, list.lastIndex)]
    }

    fun weekMeals(prefs: WeeklyNutritionPreferences): List<Pair<Int, MealOption>> =
        (0..6).flatMap { day ->
            MealSlot.entries.map { slot ->
                day to selected(day, slot, prefs)
            }
        }

    fun recipeCount(): Int = options.size
}

class WeeklyNutritionPreferences(context: Context) {
    private val prefs =
        context.getSharedPreferences("wildsport_weekly_nutrition", Context.MODE_PRIVATE)

    fun choice(dayIndex: Int, slot: MealSlot, defaultIndex: Int): Int =
        prefs.getInt("choice_" + dayIndex + "_" + slot.name, defaultIndex)

    fun setChoice(dayIndex: Int, slot: MealSlot, index: Int) {
        prefs.edit().putInt("choice_" + dayIndex + "_" + slot.name, index).apply()
    }

    fun resetWeek() {
        prefs.edit().clear().apply()
    }
}

class ShoppingChecklistPreferences(context: Context) {
    private val prefs =
        context.getSharedPreferences("wildsport_shopping_checklist", Context.MODE_PRIVATE)

    fun isChecked(key: String): Boolean = prefs.getBoolean(key, false)

    fun setChecked(key: String, checked: Boolean) {
        prefs.edit().putBoolean(key, checked).apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}
