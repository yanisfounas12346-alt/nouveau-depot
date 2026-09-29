package com.yanis.objectif30.data

enum class MealSlot(val label: String) {
    BREAKFAST("Petit-déjeuner"),
    LUNCH("Déjeuner"),
    SNACK("Collation"),
    DINNER("Dîner")
}

data class MealOption(
    val id: String,
    val slot: MealSlot,
    val name: String,
    val description: String,
    val kcal: Int,
    val protein: Int,
    val carbs: Int,
    val fat: Int,
    val fiber: Int,
    val cookMinutes: Int,
    val costLabel: String,
    val ingredients: List<String>
)

object NutritionRepository {
    val options = listOf(
        MealOption(
            "b1", MealSlot.BREAKFAST, "Omelette bulldozer",
            "4 œufs, pain complet, tomates et champignons poêlés. Très rassasiant et salé.",
            560, 36, 48, 23, 9, 12, "€",
            listOf("4 œufs", "2 tranches pain complet", "2 tomates", "150 g champignons")
        ),
        MealOption(
            "b2", MealSlot.BREAKFAST, "Porridge protéiné chaud",
            "Avoine au lait, banane, cannelle + skyr incorporé après cuisson.",
            590, 38, 79, 14, 12, 8, "€",
            listOf("80 g flocons d’avoine", "250 ml lait demi-écrémé", "1 banane", "200 g skyr", "cannelle")
        ),
        MealOption(
            "b3", MealSlot.BREAKFAST, "Bol chaud pommes de terre & œufs",
            "Pommes de terre sautées sans excès d’huile, œufs et légumes.",
            540, 33, 55, 20, 10, 15, "€",
            listOf("300 g pommes de terre", "4 œufs", "1 courgette", "1 oignon")
        ),

        MealOption(
            "l1", MealSlot.LUNCH, "Poulet curry riz",
            "Poulet, riz, gros volume de légumes et sauce curry légère.",
            720, 58, 86, 17, 12, 20, "€",
            listOf("220 g blanc de poulet", "100 g riz cru", "300 g légumes surgelés", "curry", "100 g yaourt nature")
        ),
        MealOption(
            "l2", MealSlot.LUNCH, "Chili protéiné",
            "Bœuf 5 %, haricots rouges, tomate, maïs et riz. Dense en protéines et fibres.",
            740, 55, 89, 18, 18, 25, "€",
            listOf("200 g bœuf haché 5 %", "150 g haricots rouges égouttés", "80 g riz cru", "200 g tomate concassée", "80 g maïs")
        ),
        MealOption(
            "l3", MealSlot.LUNCH, "Dinde, patates & ratatouille",
            "Assiette volumineuse, simple et très chaude.",
            690, 56, 75, 18, 13, 22, "€",
            listOf("220 g dinde", "400 g pommes de terre", "300 g ratatouille")
        ),

        MealOption(
            "s1", MealSlot.SNACK, "Bol skyr anti-faim",
            "Skyr, banane et avoine. Idéal 1 h 30 à 2 h 30 avant l’entraînement.",
            360, 29, 53, 5, 6, 2, "€",
            listOf("250 g skyr", "1 banane", "30 g flocons d’avoine")
        ),
        MealOption(
            "s2", MealSlot.SNACK, "Œufs + soupe",
            "Option chaude et salée : œufs durs/pochés avec un grand bol de soupe de légumes.",
            330, 24, 28, 14, 7, 8, "€",
            listOf("3 œufs", "400 ml soupe de légumes", "1 tranche pain complet")
        ),
        MealOption(
            "s3", MealSlot.SNACK, "Fromage blanc pomme cannelle",
            "Très simple, riche en protéines, gros volume pour peu de préparation.",
            320, 31, 39, 4, 7, 2, "€",
            listOf("300 g fromage blanc 0-3 %", "1 grosse pomme", "20 g avoine", "cannelle")
        ),

        MealOption(
            "d1", MealSlot.DINNER, "Riz sauté poulet-œufs",
            "Poulet, œufs, riz, petits pois et carottes : parfait après boxe ou jambes.",
            760, 62, 87, 19, 11, 18, "€",
            listOf("200 g poulet", "90 g riz cru", "2 œufs", "200 g petits pois-carottes", "sauce soja légère")
        ),
        MealOption(
            "d2", MealSlot.DINNER, "Pâtes bolognaise dinde",
            "Pâtes, dinde hachée, tomate et courgettes. Chaud, simple, familial.",
            735, 57, 91, 16, 13, 22, "€",
            listOf("100 g pâtes crues", "200 g dinde hachée", "250 g sauce tomate", "250 g courgettes")
        ),
        MealOption(
            "d3", MealSlot.DINNER, "Lentilles mijotées au poulet",
            "Lentilles, poulet, carottes et oignons. Très riche en fibres et rassasiant.",
            700, 61, 77, 13, 20, 28, "€",
            listOf("220 g poulet", "250 g lentilles cuites", "2 carottes", "1 oignon", "200 g tomate concassée")
        )
    )

    fun forSlot(slot: MealSlot): List<MealOption> = options.filter { it.slot == slot }

    fun selected(slot: MealSlot, index: Int): MealOption {
        val list = forSlot(slot)
        return list[index.coerceIn(0, list.lastIndex)]
    }

    fun timing(trainingHour: Int, trainingMinute: Int): Map<MealSlot, String> {
        val trainingMinutes = trainingHour * 60 + trainingMinute
        val snack = (trainingMinutes - 120).coerceAtLeast(14 * 60)
        val dinner = (trainingMinutes + 90).coerceAtMost(22 * 60)
        fun fmt(total: Int) = "%02d:%02d".format(total / 60, total % 60)
        return mapOf(
            MealSlot.BREAKFAST to "08:00",
            MealSlot.LUNCH to "12:30",
            MealSlot.SNACK to fmt(snack),
            MealSlot.DINNER to fmt(dinner)
        )
    }
}
