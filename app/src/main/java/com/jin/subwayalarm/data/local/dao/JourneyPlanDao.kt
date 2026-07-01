package com.jin.subwayalarm.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jin.subwayalarm.data.local.entity.JourneyPlanEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface JourneyPlanDao {

    @Query("SELECT * FROM journey_plan WHERE id = 0 LIMIT 1")
    suspend fun get(): JourneyPlanEntity?

    @Query("SELECT * FROM journey_plan WHERE id = 0 LIMIT 1")
    fun observe(): Flow<JourneyPlanEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(plan: JourneyPlanEntity)

    @Query("DELETE FROM journey_plan")
    suspend fun clear()
}
