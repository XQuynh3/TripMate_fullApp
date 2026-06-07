package com.example.notingapp.model

data class SuggestionResponse(
    val destination: String,
    val intro: String,
    val places: List<Place>,
    val foods: List<FoodItem>,
    val checklist: List<SuggestionChecklistItem>
)

data class Place(
    val name: String,
    val type: String,
    val reason: String,
    val estimatedCost: String
)

data class FoodItem(
    val name: String,
    val reason: String,
    val estimatedCost: String
)

data class SuggestionChecklistItem(
    val text: String,
    val category: String
)
