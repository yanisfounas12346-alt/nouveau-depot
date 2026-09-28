package com.yanis.objectif30.data

object PlanRepository {
    val days = listOf(
        DayPlan(0, "Lundi", "Haut du corps — force utile boxe", "Modérée",
            listOf(
                Exercise("Tractions", "4 × 4–10, garde 1–2 reps en réserve", "Rowing inversé sous une barre basse"),
                Exercise("Pompes", "4 × 10–15", "Pompes inclinées sur banc"),
                Exercise("Pike push-ups", "3 × 6–10", "Pike moins inclinées"),
                Exercise("Dips", "3 × 6–12 sur support stable", "Pompes serrées")
            ),
            listOf(
                Meal("Petit-déjeuner chaud", "Omelette 4 œufs + pain complet + tomates."),
                Meal("Déjeuner chaud", "Poulet 200–220 g + riz + légumes."),
                Meal("Collation", "Skyr/fromage blanc + fruit."),
                Meal("Dîner chaud", "Dinde 200 g + pommes de terre + légumes.")
            ),
            listOf("Tenue", "Barre/parc", "Eau", "Serviette", "Préparer riz + poulet"),
            "Marche 5–10 min et sommeil prioritaire."
        ),
        DayPlan(1, "Mardi", "Endurance fondamentale", "Facile",
            listOf(
                Exercise("Course facile", "25–30 min à allure conversationnelle", "3 min course / 1 min marche"),
                Exercise("Retour au calme", "5–10 min de marche", "Marche 35–45 min si douleur")
            ),
            listOf(
                Meal("Petit-déjeuner chaud", "Porridge + banane + 3 œufs."),
                Meal("Déjeuner chaud", "Poulet curry + riz + légumes."),
                Meal("Collation", "Yaourt protéiné + fruit."),
                Meal("Dîner chaud", "Pommes de terre + thon + haricots verts + œufs.")
            ),
            listOf("Chaussures course", "Chaussettes", "Tenue météo", "Eau", "Parcours plat"),
            "Pas d’intervalles rapides le même jour."
        ),
        DayPlan(2, "Mercredi", "Repos — assimilation", "Très facile",
            listOf(
                Exercise("Repos", "Pas de séance structurée", "10–20 min de marche douce"),
                Exercise("Mobilité", "5–8 min facultatives", "Repos complet")
            ),
            listOf(
                Meal("Petit-déjeuner chaud", "Œufs brouillés + champignons + pain complet."),
                Meal("Déjeuner chaud", "Lentilles + poulet + riz."),
                Meal("Collation", "Skyr + pomme."),
                Meal("Dîner chaud", "Soupe de légumes + omelette + pommes de terre.")
            ),
            listOf("Préparer tenue jeudi", "Si Five : chaussures", "Sinon : chrono"),
            "Hydratation et sommeil."
        ),
        DayPlan(3, "Jeudi", "Explosivité contrôlée / Five", "Élevée mais courte",
            listOf(
                Exercise("Five", "Match avec montée progressive", "Réduire changements de direction"),
                Exercise("Circuit cardio", "4 tours : 30 s squats, genoux hauts, pompes, mountain climbers + 60 s marche", "Squats sans saut + marche genoux hauts + pompes inclinées + climbers lents")
            ),
            listOf(
                Meal("Petit-déjeuner chaud", "Porridge + lait + œufs."),
                Meal("Déjeuner chaud", "Pâtes complètes + dinde + légumes."),
                Meal("Collation", "Banane + skyr."),
                Meal("Dîner chaud", "Riz + poulet + légumes + œuf.")
            ),
            listOf("Chaussures", "Tenue", "Eau", "Serviette", "Chrono"),
            "Pas de sprint supplémentaire."
        ),
        DayPlan(4, "Vendredi", "Jambes + tronc", "Modérée",
            listOf(
                Exercise("Squats", "4 × 15–20", "Squat sur banc"),
                Exercise("Fentes arrière", "3 × 8–12 / jambe", "Split squat petite amplitude"),
                Exercise("Mollets", "3 × 15–20 / jambe", "Deux jambes simultanées"),
                Exercise("Planche", "4 × 30–60 s", "4 × 20–30 s")
            ),
            listOf(
                Meal("Petit-déjeuner chaud", "Omelette + pain complet."),
                Meal("Déjeuner chaud", "Dinde ou steak 5% + pommes de terre + ratatouille."),
                Meal("Collation", "Skyr + fruit."),
                Meal("Dîner chaud", "Riz sauté poulet, œufs et légumes.")
            ),
            listOf("Baskets stables", "Eau", "Serviette", "Préparer tenue boxe"),
            "Réduis une série si les jambes sont lourdes."
        ),
        DayPlan(5, "Samedi", "Boxe — technique et cardio", "Modérée à élevée",
            listOf(
                Exercise("Déplacements en garde", "5 × 3 min / 1 min repos", "Pas glissés sans saut"),
                Exercise("Shadow boxing", "4 × 3 min", "3 × 2 min"),
                Exercise("Séquence southpaw", "Jab droit → esquive → pivot → gauche au corps", "Travail lent devant un reflet")
            ),
            listOf(
                Meal("Petit-déjeuner chaud", "Porridge + banane + omelette."),
                Meal("Déjeuner chaud", "Poulet paprika + riz/semoule + légumes."),
                Meal("Collation", "Banane + skyr."),
                Meal("Dîner chaud", "Pâtes + sauce tomate + boulettes de dinde.")
            ),
            listOf("Chaussures légères", "Tenue boxe", "Eau", "Serviette", "Chrono rounds"),
            "Qualité technique avant épuisement."
        ),
        DayPlan(6, "Dimanche", "Repos actif", "Facile",
            listOf(
                Exercise("Marche", "45 min tranquille", "25–30 min si grosse fatigue"),
                Exercise("Mobilité", "5–10 min facultatives", "Repos")
            ),
            listOf(
                Meal("Petit-déjeuner chaud", "Porridge + 3 œufs."),
                Meal("Déjeuner chaud", "Chili de dinde/bœuf 5% + haricots rouges + riz."),
                Meal("Collation", "Skyr + fruit."),
                Meal("Dîner chaud", "Lentilles mijotées + poulet/dinde + légumes.")
            ),
            listOf("Chaussures confortables", "Eau", "Préparer lundi"),
            "Fais le point sur énergie, douleurs et sommeil."
        )
    )

    fun day(index: Int): DayPlan = days[index.coerceIn(0, 6)]
}
