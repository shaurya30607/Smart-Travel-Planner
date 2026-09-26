package com.example.smarttravelplanner.data.repository

import kotlinx.coroutines.flow.Flow

/** Destinations the user has saved (hearted) on the Explore screen. */
interface WishlistRepository {

    /** Names of saved destinations; emits again whenever the wishlist changes. */
    val savedDestinations: Flow<Set<String>>

    /** Saves [destinationName] if it isn't saved yet, otherwise removes it. */
    suspend fun toggle(destinationName: String)
}
