package com.example.smarttravelplanner.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Stores the wishlist in the app's existing Preferences DataStore,
 * so it survives app restarts without needing a database migration.
 */
class WishlistRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : WishlistRepository {

    private val wishlistKey = stringSetPreferencesKey("explore_wishlist")

    override val savedDestinations: Flow<Set<String>> =
        dataStore.data
            .map { prefs -> prefs[wishlistKey].orEmpty() }
            .distinctUntilChanged()

    override suspend fun toggle(destinationName: String) {
        dataStore.edit { prefs ->
            val current = prefs[wishlistKey].orEmpty()
            prefs[wishlistKey] =
                if (destinationName in current) current - destinationName
                else current + destinationName
        }
    }
}
