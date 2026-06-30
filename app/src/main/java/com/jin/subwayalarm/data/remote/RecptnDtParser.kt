package com.jin.subwayalarm.data.remote

import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * recptnDt("yyyy-MM-dd HH:mm:ss", KST 로컬시간 가정) → epoch millis 순수 변환.
 * minSdk 26 이상이라 java.time 네이티브 사용(데서거링 불요).
 */
object RecptnDtParser {
    private val FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    private val ZONE: ZoneId = ZoneId.of("Asia/Seoul")

    fun parseToEpochMillis(raw: String): Long =
        LocalDateTime.parse(raw.trim(), FORMATTER).atZone(ZONE).toInstant().toEpochMilli()
}
