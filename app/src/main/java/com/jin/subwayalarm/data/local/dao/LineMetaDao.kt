package com.jin.subwayalarm.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jin.subwayalarm.data.local.entity.LineMetaEntity

@Dao
interface LineMetaDao {

    @Query("SELECT * FROM line_meta ORDER BY lineId ASC")
    suspend fun all(): List<LineMetaEntity>

    @Query("SELECT * FROM line_meta WHERE lineId = :lineId LIMIT 1")
    suspend fun byId(lineId: String): LineMetaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(lines: List<LineMetaEntity>)

    @Query("DELETE FROM line_meta")
    suspend fun clearAll()
}
