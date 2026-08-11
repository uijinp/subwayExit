package com.jin.subwayalarm.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 즐겨찾기 경로. 역 **이름**으로 저장한다 — statnId 는 노선별 네임스페이스라 시드가 바뀌면
 * 깨질 수 있고, 실제 경로(구간·환승)는 불러올 때 다시 계산하므로 이름만 있으면 충분하다.
 *
 * (originName, destinationName) 유니크 인덱스로 같은 경로가 중복 저장되지 않게 한다.
 */
@Entity(
    tableName = "favorite_route",
    indices = [Index(value = ["originName", "destinationName"], unique = true)],
)
data class FavoriteRouteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val originName: String,
    val destinationName: String,
    val createdAtEpochMillis: Long,
    /** 최근 사용 순으로 보여주기 위한 값. 즐겨찾기를 눌러 경로를 불러올 때 갱신한다. */
    val lastUsedAtEpochMillis: Long,
)
