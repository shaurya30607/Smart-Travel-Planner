package com.example.smarttravelplanner.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class WishlistRepositoryImplTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var scope: CoroutineScope
    private lateinit var repository: WishlistRepositoryImpl

    @Before
    fun setUp() {
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val dataStore = PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = { tempFolder.newFile("test_wishlist.preferences_pb") }
        )
        repository = WishlistRepositoryImpl(dataStore)
    }

    @After
    fun tearDown() {
        scope.cancel()
    }

    @Test
    fun startsEmpty() = runBlocking {
        assertTrue(repository.savedDestinations.first().isEmpty())
    }

    @Test
    fun toggleAddsThenRemoves() = runBlocking {
        repository.toggle("Goa")
        assertEquals(setOf("Goa"), repository.savedDestinations.first())

        repository.toggle("Goa")
        assertTrue(repository.savedDestinations.first().isEmpty())
    }

    @Test
    fun keepsMultipleDestinations() = runBlocking {
        repository.toggle("Goa")
        repository.toggle("Leh")
        repository.toggle("Jaipur")
        repository.toggle("Leh")

        assertEquals(setOf("Goa", "Jaipur"), repository.savedDestinations.first())
    }
}
