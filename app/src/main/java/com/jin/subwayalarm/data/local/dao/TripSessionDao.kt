package com.jin.subwayalarm.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jin.subwayalarm.data.local.entity.TripSessionEntity

@Dao
interface TripSessionDao {

    @Query("SELECT * FROM trip_session WHERE id = 0 LIMIT 1")
    suspend fun get(): TripSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(session: TripSessionEntity)

    @Query("DELETE FROM trip_session")
    suspend fun clear()
}
