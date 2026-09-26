package com.example.smarttravelplanner.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.smarttravelplanner.data.local.db.Converters

@Entity(tableName = "trips")
@TypeConverters(Converters::class)
data class TripEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val destination: String,
    val startDate: String,       // ISO-8601 string
    val endDate: String,         // ISO-8601 string
    val budget: Double,
    val adults: Int = 1,
    val kids: Int = 0,
    val interests: List<String> = emptyList(),
    val notes: String = "",
    val coverImageUrl: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
