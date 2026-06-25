package com.jin.subwayalarm.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.jin.subwayalarm.data.local.dao.LineMetaDao
import com.jin.subwayalarm.data.local.dao.StationDao
import com.jin.subwayalarm.data.local.entity.LineMetaEntity
import com.jin.subwayalarm.data.local.entity.StationEntity

@Database(
    entities = [StationEntity::class, LineMetaEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class SubwayDatabase : RoomDatabase() {
    abstract fun stationDao(): StationDao
    abstract fun lineMetaDao(): LineMetaDao

    companion object {
        const val NAME = "subway.db"
    }
}
