package com.yanis.objectif30.data

import android.content.Context

object WeeklyNutritionRepository {
    private val extras = listOf(
        MealOption(
            "b4", MealSlot.BREAKFAST, "Shakshuka protéinée",
            "Œufs mijotés dans tomate, poivron et oignon avec pain complet. Chaud, volumineux et rassasiant.",
            545, 35, 52, 21, 11, 18, "€",
            listOf("4 œufs", "250 g tomate concassée", "1 poivron", "1 oignon", "2 tranches pain complet")
        ),
        MealOption(
            "b5", MealSlot.BREAKFAST, "Croque dinde œufs",
            "Pain complet grillé, dinde, œufs et fromage léger avec tomates chaudes.",
            575, 44, 50, 22, 8, 12, "€€",
            listOf("3 œufs", "100 g dinde", "3 tranches pain complet", "30 g emmental", "2 tomates")
        ),
        MealOption(
            "b6", MealSlot.BREAKFAST, "Semoule chaude skyr-banane",
            "Semoule fine au lait, banane, cannelle et skyr ajouté après cuisson.",
            565, 36, 84, 9, 8, 8, "€",
            listOf("70 g semoule fine", "250 ml lait demi-écrémé", "200 g skyr", "1 banane", "cannelle")
        ),

        MealOption(
            "l4", MealSlot.LUNCH, "Couscous poulet express",
            "Poulet, semoule, pois chiches et légumes couscous. Gros volume et beaucoup de fibres.",
            735, 59, 91, 15, 16, 25, "€",
            listOf("220 g blanc de poulet", "90 g semoule crue", "120 g pois chiches égouttés", "350 g légumes couscous")
        ),
        MealOption(
            "l5", MealSlot.LUNCH, "Bowl mexicain dinde",
            "Dinde hachée, riz, haricots rouges, poivrons et salsa tomate.",
            725, 57, 88, 16, 17, 22, "€",
            listOf("200 g dinde hachée", "80 g riz cru", "150 g haricots rouges égouttés", "250 g poivrons", "150 g sauce tomate")
        ),
        MealOption(
            "l6", MealSlot.LUNCH, "Pâtes thon tomate épinards",
            "Pâtes complètes, thon, épinards et sauce tomate. Rapide, économique et riche en protéines.",
            695, 52, 87, 14, 13, 18, "€",
            listOf("100 g pâtes complètes crues", "160 g thon au naturel égoutté", "250 g épinards", "200 g sauce tomate")
        ),

        MealOption(
            "s4", MealSlot.SNACK, "Porridge express anti-faim",
            "Petit porridge chaud avec lait, avoine et skyr. Bon compromis avant une séance tardive.",
            355, 28, 50, 6, 7, 6, "€",
            listOf("40 g flocons d’avoine", "150 ml lait demi-écrémé", "180 g skyr", "1 pomme")
        ),
        MealOption(
            "s5", MealSlot.SNACK, "Tartines dinde fromage blanc",
            "Pain complet chaud, dinde et fromage blanc assaisonné avec concombre.",
            340, 31, 39, 7, 6, 5, "€",
            listOf("2 tranches pain complet", "80 g dinde", "150 g fromage blanc", "100 g concombre")
        ),
        MealOption(
            "s6", MealSlot.SNACK, "Soupe lentilles œuf",
            "Bol chaud de soupe de lentilles avec un œuf et une tranche de pain complet.",
            350, 24, 45, 9, 11, 10, "€",
            listOf("300 ml soupe de lentilles", "1 œuf", "1 tranche pain complet")
        ),

        MealOption(
            "d4", MealSlot.DINNER, "Curry de dinde coco léger",
            "Dinde, riz, légumes et lait de coco léger. Chaud et très réconfortant.",
            720, 56, 84, 17, 12, 24, "€€",
            listOf("220 g dinde", "85 g riz cru", "300 g légumes surgelés", "100 ml lait de coco léger", "curry")
        ),
        MealOption(
            "d5", MealSlot.DINNER, "Hachis parmentier léger",
            "Bœuf 5 %, purée de pommes de terre et carottes avec salade ou légumes.",
            710, 55, 73, 20, 12, 30, "€",
            listOf("200 g bœuf haché 5 %", "400 g pommes de terre", "200 g carottes", "200 g haricots verts")
        ),
        MealOption(
            "d6", MealSlot.DINNER, "Poulet basquaise & riz",
            "Poulet mijoté tomate-poivron avec riz. Très simple à préparer en plusieurs portions.",
            705, 60, 80, 14, 12, 28, "€",
            listOf("220 g blanc de poulet", "85 g riz cru", "250 g poivrons", "200 g tomate concassée", "1 oignon")
        )
    )

    val options: List<MealOption> = NutritionRepository.options + extras

    fun forSlot(slot: MealSlot): List<MealOption> = options.filter { it.slot == slot }

    fun defaultIndex(dayIndex: Int, slot: MealSlot): Int {
        val size = forSlot(slot).size
        val offset = when (slot) {
            MealSlot.BREAKFAST -> 0
            MealSlot.LUNCH -> 2
            MealSlot.SNACK -> 4
            MealSlot.DINNER -> 1
        }
        return (dayIndex + offset) % size
    }

    fun selected(dayIndex: Int, slot: MealSlot, prefs: WeeklyNutritionPreferences): MealOption {
        val list = forSlot(slot)
        val index = prefs.choice(dayIndex, slot, defaultIndex(dayIndex, slot))
        return list[index.coerceIn(0, list.lastIndex)]
    }

    fun weekMeals(prefs: WeeklyNutritionPreferences): List<Pair<Int, MealOption>> =
        (0..6).flatMap { day ->
            MealSlot.entries.map { slot -> day to selected(day, slot, prefs) }
        }
}

class WeeklyNutritionPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("wildsport_weekly_nutrition", Context.MODE_PRIVATE)

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
    private val prefs = context.getSharedPreferences("wildsport_shopping_checklist", Context.MODE_PRIVATE)

    fun isChecked(key: String): Boolean = prefs.getBoolean(key, false)

    fun setChecked(key: String, checked: Boolean) {
        prefs.edit().putBoolean(key, checked).apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}
