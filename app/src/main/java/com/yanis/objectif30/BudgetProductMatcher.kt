package com.yanis.objectif30

import kotlin.math.ceil
import kotlin.math.roundToInt

data class PurchaseNeed(
    val amount: Double,
    val unit: String,
    val name: String
)

data class PurchaseRecommendation(
    val product: LeclercProduct,
    val packs: Int,
    val totalCost: Double,
    val purchasedAmount: Double?,
    val wasteAmount: Double?,
    val unit: String,
    val confidence: Int
)

object BudgetProductMatcher {
    fun parseNeed(text: String): PurchaseNeed {
        val clean = text.trim()
        val qty = Regex(
            "^\\s*(\\d+(?:[.,]\\d+)?)\\s*(kg|g|ml|cl|l)?\\s*(?:[×x]\\s*)?(.+)$",
            RegexOption.IGNORE_CASE
        ).find(clean)

        if (qty != null) {
            var amount = qty.groupValues[1].replace(',', '.').toDoubleOrNull() ?: 1.0
            var unit = qty.groupValues[2].lowercase()
            val name = qty.groupValues[3].trim()

            when (unit) {
                "kg" -> {
                    amount *= 1000.0
                    unit = "g"
                }
                "l" -> {
                    amount *= 1000.0
                    unit = "ml"
                }
                "cl" -> {
                    amount *= 10.0
                    unit = "ml"
                }
                "" -> unit = "p"
            }
            return PurchaseNeed(amount, unit, name)
        }

        return PurchaseNeed(1.0, "p", clean)
    }

    fun recommend(needText: String, products: List<LeclercProduct>): PurchaseRecommendation? {
        val need = parseNeed(needText)
        val candidates = products
            .filter { it.available && it.price > 0.0 }
            .mapNotNull { product ->
                val pack = inferPack(product, need.unit) ?: return@mapNotNull null
                if (pack.first != need.unit || pack.second <= 0.0) return@mapNotNull null

                val packs = ceil(need.amount / pack.second).toInt().coerceAtLeast(1)
                val bought = packs * pack.second
                val waste = (bought - need.amount).coerceAtLeast(0.0)
                val total = packs * product.price

                val wasteRatio = if (need.amount > 0) waste / need.amount else 0.0
                val score = total + wasteRatio.coerceAtMost(2.0) * 0.30

                Scored(
                    recommendation = PurchaseRecommendation(
                        product = product,
                        packs = packs,
                        totalCost = total,
                        purchasedAmount = bought,
                        wasteAmount = waste,
                        unit = need.unit,
                        confidence = pack.third
                    ),
                    score = score
                )
            }
            .sortedWith(
                compareBy<Scored> { it.score }
                    .thenBy { it.recommendation.totalCost }
                    .thenByDescending { it.recommendation.confidence }
            )

        return candidates.firstOrNull()?.recommendation
    }

    fun alternatives(
        needText: String,
        products: List<LeclercProduct>,
        limit: Int = 3
    ): List<PurchaseRecommendation> {
        val need = parseNeed(needText)
        return products
            .filter { it.available && it.price > 0.0 }
            .mapNotNull { product ->
                val pack = inferPack(product, need.unit) ?: return@mapNotNull null
                if (pack.first != need.unit || pack.second <= 0.0) return@mapNotNull null
                val packs = ceil(need.amount / pack.second).toInt().coerceAtLeast(1)
                val bought = packs * pack.second
                val waste = (bought - need.amount).coerceAtLeast(0.0)
                PurchaseRecommendation(
                    product = product,
                    packs = packs,
                    totalCost = packs * product.price,
                    purchasedAmount = bought,
                    wasteAmount = waste,
                    unit = need.unit,
                    confidence = pack.third
                )
            }
            .sortedWith(
                compareBy<PurchaseRecommendation> { it.totalCost }
                    .thenBy { it.wasteAmount ?: Double.MAX_VALUE }
                    .thenByDescending { it.confidence }
            )
            .take(limit)
    }

    fun formatAmount(amount: Double?, unit: String): String {
        if (amount == null) return "?"
        return when (unit) {
            "g" -> if (amount >= 1000) {
                val kg = amount / 1000.0
                val txt = if (kg % 1.0 == 0.0) kg.roundToInt().toString() else "%.1f".format(kg)
                txt + " kg"
            } else {
                amount.roundToInt().toString() + " g"
            }
            "ml" -> if (amount >= 1000) {
                val liters = amount / 1000.0
                val txt = if (liters % 1.0 == 0.0) liters.roundToInt().toString() else "%.1f".format(liters)
                txt + " L"
            } else {
                amount.roundToInt().toString() + " ml"
            }
            "p" -> amount.roundToInt().toString() + " pièce(s)"
            else -> "%.1f".format(amount) + " " + unit
        }
    }

    private data class Scored(
        val recommendation: PurchaseRecommendation,
        val score: Double
    )

    private fun inferPack(product: LeclercProduct, targetUnit: String): Triple<String, Double, Int>? {
        inferFromPricePerUnit(product, targetUnit)?.let { return it }

        val label = product.label.lowercase()
            .replace("œ", "oe")
            .replace("×", "x")

        val multiMetric = Regex(
            "(\\d+)\\s*x\\s*(\\d+(?:[.,]\\d+)?)\\s*(kg|g|ml|cl|l)\\b",
            RegexOption.IGNORE_CASE
        ).find(label)
        if (multiMetric != null) {
            val count = multiMetric.groupValues[1].toDouble()
            var amount = multiMetric.groupValues[2].replace(',', '.').toDouble()
            var unit = multiMetric.groupValues[3].lowercase()
            when (unit) {
                "kg" -> {
                    amount *= 1000.0
                    unit = "g"
                }
                "l" -> {
                    amount *= 1000.0
                    unit = "ml"
                }
                "cl" -> {
                    amount *= 10.0
                    unit = "ml"
                }
            }
            return Triple(unit, count * amount, 3)
        }

        val metricMatches = Regex(
            "(\\d+(?:[.,]\\d+)?)\\s*(kg|g|ml|cl|l)\\b",
            RegexOption.IGNORE_CASE
        ).findAll(label).toList()
        if (metricMatches.isNotEmpty()) {
            val match = metricMatches.last()
            var amount = match.groupValues[1].replace(',', '.').toDouble()
            var unit = match.groupValues[2].lowercase()
            when (unit) {
                "kg" -> {
                    amount *= 1000.0
                    unit = "g"
                }
                "l" -> {
                    amount *= 1000.0
                    unit = "ml"
                }
                "cl" -> {
                    amount *= 10.0
                    unit = "ml"
                }
            }
            return Triple(unit, amount, 2)
        }

        val xCount = Regex("\\bx\\s*(\\d+)\\b", RegexOption.IGNORE_CASE).find(label)
            ?: Regex("\\b(\\d+)\\s*(?:oeufs?|tranches?|portions?|sachets?|pots?)\\b", RegexOption.IGNORE_CASE).find(label)
        if (xCount != null) {
            return Triple("p", xCount.groupValues[1].toDouble(), 2)
        }

        if (targetUnit == "p") {
            return Triple("p", 1.0, 1)
        }

        return null
    }

    private fun inferFromPricePerUnit(
        product: LeclercProduct,
        targetUnit: String
    ): Triple<String, Double, Int>? {
        val raw = product.pricePerUnit.lowercase().replace(" ", "")
        if (raw.isBlank()) return null

        val value = Regex("(\\d+(?:[.,]\\d+)?)€?/(kg|l|p|pièce|piece)")
            .find(raw) ?: return null

        val unitPrice = value.groupValues[1].replace(',', '.').toDoubleOrNull() ?: return null
        if (unitPrice <= 0.0) return null

        val baseUnit = value.groupValues[2]
        return when {
            baseUnit == "kg" && targetUnit == "g" -> {
                val grams = product.price / unitPrice * 1000.0
                Triple("g", grams, 4)
            }
            baseUnit == "l" && targetUnit == "ml" -> {
                val ml = product.price / unitPrice * 1000.0
                Triple("ml", ml, 4)
            }
            (baseUnit == "p" || baseUnit == "pièce" || baseUnit == "piece") && targetUnit == "p" -> {
                val pieces = product.price / unitPrice
                Triple("p", pieces, 4)
            }
            else -> null
        }
    }
}
