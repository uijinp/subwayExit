package com.jin.subwayalarm.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jin.subwayalarm.data.local.entity.FavoriteRouteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteRouteDao {

    /** 최근 사용 순 — 자주 쓰는 경로가 위로 올라온다. */
    @Query("SELECT * FROM favorite_route ORDER BY lastUsedAtEpochMillis DESC")
    fun observeAll(): Flow<List<FavoriteRouteEntity>>

    /** 같은 (출발, 도착) 이 이미 있으면 REPLACE — 유니크 인덱스 덕에 중복이 쌓이지 않는다. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(route: FavoriteRouteEntity)

    @Query("DELETE FROM favorite_route WHERE originName = :origin AND destinationName = :destination")
    suspend fun delete(origin: String, destination: String)

    @Query("UPDATE favorite_route SET lastUsedAtEpochMillis = :nowEpochMillis WHERE originName = :origin AND destinationName = :destination")
    suspend fun touch(origin: String, destination: String, nowEpochMillis: Long)
}
