package com.jin.subwayalarm.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 진행 중인 여정 1건 (사망 복구 SoT, 4-1). MVP 는 동시 1건만 지원 → 단일 행(id=0)만 사용.
 */
@Entity(tableName = "trip_session")
data class TripSessionEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val originStatnId: String,
    val destStatnId: String,
    val lineId: String,
    val currentStatnId: String,
    val lastSyncAtEpochMillis: Long,
    val boardedTrainNo: String?,
    /** TrackingMode.name */
    val mode: String,
    /** TripState.name */
    val state: String,
) {
    companion object {
        const val SINGLETON_ID = 0
    }
}
