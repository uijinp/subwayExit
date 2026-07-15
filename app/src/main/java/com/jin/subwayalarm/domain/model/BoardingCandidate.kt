package com.jin.subwayalarm.domain.model

/** 탑승 후보 열차 1대 (재매칭 UI·MATCHING 단계). */
data class BoardingCandidate(
    val trainNo: String,
    val statnId: String,
    val statnName: String,
    val status: TrainStatus,
    val updnLine: String,
    val direction: Direction?,
    val terminusName: String?,
    val isExpress: Boolean,
    val recptnDtEpochMillis: Long,
)
