package com.jin.subwayalarm.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "station",
    indices = [Index("lineId"), Index(value = ["lineId", "orderInLine"], unique = true)],
)
data class StationEntity(
    @PrimaryKey val statnId: String,
    val lineId: String,
    val statnName: String,
    val orderInLine: Int,
    val avgSecToNext: Int,
    val nextStatnId: String?,
    val prevStatnId: String?,
    val lat: Double = 0.0,
    val lon: Double = 0.0,
)
