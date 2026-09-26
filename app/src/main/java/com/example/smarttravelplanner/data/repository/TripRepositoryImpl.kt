package com.example.smarttravelplanner.data.repository

import com.example.smarttravelplanner.data.local.dao.TripDao
import com.example.smarttravelplanner.data.local.entity.TripEntity
import com.example.smarttravelplanner.domain.model.Trip
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

class TripRepositoryImpl @Inject constructor(
    private val tripDao: TripDao
) : TripRepository {

    override fun getAllTrips(): Flow<List<Trip>> =
        tripDao.getAllTrips().map { entities -> entities.map { it.toDomain() } }

    override suspend fun getTripById(id: Long): Trip? =
        tripDao.getTripById(id)?.toDomain()

    override suspend fun saveTrip(trip: Trip): Long =
        tripDao.insertTrip(trip.toEntity())

    override suspend fun updateTrip(trip: Trip) =
        tripDao.updateTrip(trip.toEntity())

    override suspend fun deleteTrip(id: Long) =
        tripDao.deleteTripById(id)

    // ── Mappers ───────────────────────────────────────────────────────────────

    private fun TripEntity.toDomain() = Trip(
        id = id,
        title = title,
        destination = destination,
        startDate = LocalDate.parse(startDate),
        endDate = LocalDate.parse(endDate),
        budget = budget,
        adults = adults,
        kids = kids,
        interests = interests,
        notes = notes,
        coverImageUrl = coverImageUrl,
        createdAt = createdAt
    )

    private fun Trip.toEntity() = TripEntity(
        id = id,
        title = title,
        destination = destination,
        startDate = startDate.toString(),
        endDate = endDate.toString(),
        budget = budget,
        adults = adults,
        kids = kids,
        interests = interests,
        notes = notes,
        coverImageUrl = coverImageUrl,
        createdAt = createdAt
    )
}
