package com.jin.subwayalarm.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jin.subwayalarm.data.local.entity.StationEntity

@Dao
interface StationDao {

    @Query("SELECT * FROM station WHERE lineId = :lineId ORDER BY orderInLine ASC")
    suspend fun stationsForLine(lineId: String): List<StationEntity>

    @Query("SELECT COUNT(*) FROM station WHERE lineId = :lineId")
    suspend fun countForLine(lineId: String): Int

    @Query("SELECT * FROM station WHERE statnId = :statnId LIMIT 1")
    suspend fun byId(statnId: String): StationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(stations: List<StationEntity>)

    @Query("DELETE FROM station WHERE lineId = :lineId")
    suspend fun clearLine(lineId: String)
}
