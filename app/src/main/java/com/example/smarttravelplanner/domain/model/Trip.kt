package com.example.smarttravelplanner.domain.model

import java.time.LocalDate

data class Trip(
    val id: Long = 0,
    val title: String,
    val destination: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val budget: Double,
    val adults: Int = 1,
    val kids: Int = 0,
    val interests: List<String> = emptyList(),
    val notes: String = "",
    val coverImageUrl: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    val totalPassengers: Int
        get() = (adults + kids).coerceAtLeast(1)
}
