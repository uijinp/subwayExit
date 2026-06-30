package com.jin.subwayalarm.data.remote

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class RecptnDtParserTest {

    @Test
    fun `KST 로컬시간 문자열을 epoch millis 로 변환`() {
        val expected = ZonedDateTime.of(2026, 7, 1, 8, 30, 0, 0, ZoneId.of("Asia/Seoul"))
            .toInstant().toEpochMilli()

        assertEquals(expected, RecptnDtParser.parseToEpochMillis("2026-07-01 08:30:00"))
    }

    @Test
    fun `앞뒤 공백은 trim 되어 파싱`() {
        val a = RecptnDtParser.parseToEpochMillis("2026-07-01 08:30:00")
        val b = RecptnDtParser.parseToEpochMillis("  2026-07-01 08:30:00  ")
        assertEquals(a, b)
    }

    @Test(expected = Exception::class)
    fun `형식이 다르면 예외`() {
        RecptnDtParser.parseToEpochMillis("2026/07/01 08:30:00")
    }
}
