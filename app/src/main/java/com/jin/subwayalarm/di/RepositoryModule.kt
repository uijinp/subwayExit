package com.jin.subwayalarm.di

import com.jin.subwayalarm.data.repository.JourneyPlanRepository
import com.jin.subwayalarm.data.repository.JourneyPlanRepositoryImpl
import com.jin.subwayalarm.data.repository.StationRepository
import com.jin.subwayalarm.data.repository.StationRepositoryImpl
import com.jin.subwayalarm.data.repository.TrainPositionRepository
import com.jin.subwayalarm.data.repository.TrainPositionRepositoryImpl
import com.jin.subwayalarm.data.repository.TripSessionRepository
import com.jin.subwayalarm.data.repository.TripSessionRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindStationRepository(impl: StationRepositoryImpl): StationRepository

    @Binds
    @Singleton
    abstract fun bindTrainPositionRepository(impl: TrainPositionRepositoryImpl): TrainPositionRepository

    @Binds
    @Singleton
    abstract fun bindTripSessionRepository(impl: TripSessionRepositoryImpl): TripSessionRepository

    @Binds
    @Singleton
    abstract fun bindJourneyPlanRepository(impl: JourneyPlanRepositoryImpl): JourneyPlanRepository
}
