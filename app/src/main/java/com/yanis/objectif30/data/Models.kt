package com.yanis.objectif30.data

data class Exercise(
    val name: String,
    val prescription: String,
    val replacement: String,
    val note: String = ""
)

data class Meal(
    val title: String,
    val description: String
)

data class DayPlan(
    val dayIndex: Int,
    val name: String,
    val focus: String,
    val intensity: String,
    val exercises: List<Exercise>,
    val meals: List<Meal>,
    val prep: List<String>,
    val recovery: String
)
