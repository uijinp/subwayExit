package com.jin.subwayalarm.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "line_meta")
data class LineMetaEntity(
    @PrimaryKey val lineId: String,
    val lineName: String,
    val supportsRealtime: Boolean,
    /** LineTopology.name 저장 (LINEAR/LOOP/BRANCHED). */
    val topology: String,
    /** DirectionPolarity.ascendingUpdnLine. null = 작업순서 3 실측 검증 전(미확정). */
    val ascendingUpdnLine: String? = null,
)
