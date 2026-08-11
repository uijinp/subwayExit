package com.jin.subwayalarm.di

import android.content.Context
import androidx.room.Room
import com.jin.subwayalarm.data.local.SubwayDatabase
import com.jin.subwayalarm.data.local.dao.FavoriteRouteDao
import com.jin.subwayalarm.data.local.dao.JourneyPlanDao
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
            // 즐겨찾기(사용자 데이터)를 지키기 위해 실제 마이그레이션을 우선 사용한다.
            .addMigrations(SubwayDatabase.MIGRATION_4_5)
            // 경로가 없는 버전 조합에 한해서만 파괴적 재생성(역·노선 시드는 재시드됨).
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideStationDao(db: SubwayDatabase): StationDao = db.stationDao()

    @Provides
    fun provideLineMetaDao(db: SubwayDatabase): LineMetaDao = db.lineMetaDao()

    @Provides
    fun provideTripSessionDao(db: SubwayDatabase): TripSessionDao = db.tripSessionDao()

    @Provides
    fun provideJourneyPlanDao(db: SubwayDatabase): JourneyPlanDao = db.journeyPlanDao()

    @Provides
    fun provideFavoriteRouteDao(db: SubwayDatabase): FavoriteRouteDao = db.favoriteRouteDao()
}
