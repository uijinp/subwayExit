package com.jin.subwayalarm.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.jin.subwayalarm.data.local.dao.JourneyPlanDao
import com.jin.subwayalarm.data.local.dao.LineMetaDao
import com.jin.subwayalarm.data.local.dao.StationDao
import com.jin.subwayalarm.data.local.dao.TripSessionDao
import com.jin.subwayalarm.data.local.entity.JourneyPlanEntity
import com.jin.subwayalarm.data.local.entity.LineMetaEntity
import com.jin.subwayalarm.data.local.entity.StationEntity
import com.jin.subwayalarm.data.local.entity.TripSessionEntity

@Database(
    entities = [StationEntity::class, LineMetaEntity::class, TripSessionEntity::class, JourneyPlanEntity::class],
    version = 4,
    exportSchema = false,
)
abstract class SubwayDatabase : RoomDatabase() {
    abstract fun stationDao(): StationDao
    abstract fun lineMetaDao(): LineMetaDao
    abstract fun tripSessionDao(): TripSessionDao
    abstract fun journeyPlanDao(): JourneyPlanDao

    companion object {
        const val NAME = "subway.db"
    }
}
