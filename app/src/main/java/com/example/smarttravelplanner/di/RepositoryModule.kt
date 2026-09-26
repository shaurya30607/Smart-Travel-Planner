package com.example.smarttravelplanner.di

import com.example.smarttravelplanner.data.repository.AiRepository
import com.example.smarttravelplanner.data.repository.AiRepositoryImpl
import com.example.smarttravelplanner.data.repository.ItineraryRepository
import com.example.smarttravelplanner.data.repository.ItineraryRepositoryImpl
import com.example.smarttravelplanner.data.repository.TripRepository
import com.example.smarttravelplanner.data.repository.TripRepositoryImpl
import com.example.smarttravelplanner.data.repository.UserPreferencesRepository
import com.example.smarttravelplanner.data.repository.UserPreferencesRepositoryImpl
import com.example.smarttravelplanner.data.repository.WeatherRepository
import com.example.smarttravelplanner.data.repository.WeatherRepositoryImpl
import com.example.smarttravelplanner.data.repository.AuthRepository
import com.example.smarttravelplanner.data.repository.AuthRepositoryImpl
import com.example.smarttravelplanner.data.repository.CurrencyRepository
import com.example.smarttravelplanner.data.repository.CurrencyRepositoryImpl
import com.example.smarttravelplanner.data.repository.DestinationRepository
import com.example.smarttravelplanner.data.repository.DestinationRepositoryImpl
import com.example.smarttravelplanner.data.repository.WishlistRepository
import com.example.smarttravelplanner.data.repository.WishlistRepositoryImpl
import com.example.smarttravelplanner.data.repository.PhotoRepository
import com.example.smarttravelplanner.data.repository.PhotoRepositoryImpl
import com.example.smarttravelplanner.data.repository.PlacesRepository
import com.example.smarttravelplanner.data.repository.PlacesRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds @Singleton
    abstract fun bindTripRepository(impl: TripRepositoryImpl): TripRepository

    @Binds @Singleton
    abstract fun bindAiRepository(impl: AiRepositoryImpl): AiRepository

    @Binds @Singleton
    abstract fun bindItineraryRepository(impl: ItineraryRepositoryImpl): ItineraryRepository

    @Binds @Singleton
    abstract fun bindWeatherRepository(impl: WeatherRepositoryImpl): WeatherRepository

    @Binds @Singleton
    abstract fun bindUserPreferencesRepository(impl: UserPreferencesRepositoryImpl): UserPreferencesRepository

    @Binds @Singleton
    abstract fun bindPlacesRepository(impl: PlacesRepositoryImpl): PlacesRepository

    @Binds @Singleton
    abstract fun bindPhotoRepository(impl: PhotoRepositoryImpl): PhotoRepository

    @Binds @Singleton
    abstract fun bindCurrencyRepository(impl: CurrencyRepositoryImpl): CurrencyRepository

    @Binds @Singleton
    abstract fun bindDestinationRepository(impl: DestinationRepositoryImpl): DestinationRepository

    @Binds @Singleton
    abstract fun bindWishlistRepository(impl: WishlistRepositoryImpl): WishlistRepository
}
