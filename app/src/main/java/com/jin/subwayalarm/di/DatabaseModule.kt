package com.jin.subwayalarm.di

import android.content.Context
import androidx.room.Room
import com.jin.subwayalarm.data.local.SubwayDatabase
import com.jin.subwayalarm.data.local.dao.LineMetaDao
import com.jin.subwayalarm.data.local.dao.StationDao
import com.jin.subwayalarm.data.local.dao.TripSessionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SubwayDatabase =
        Room.databaseBuilder(context, SubwayDatabase::class.java, SubwayDatabase.NAME)
            // MVP 개발 단계: 스키마 변경 시 파괴적 마이그레이션 허용.
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideStationDao(db: SubwayDatabase): StationDao = db.stationDao()

    @Provides
    fun provideLineMetaDao(db: SubwayDatabase): LineMetaDao = db.lineMetaDao()

    @Provides
    fun provideTripSessionDao(db: SubwayDatabase): TripSessionDao = db.tripSessionDao()
}
