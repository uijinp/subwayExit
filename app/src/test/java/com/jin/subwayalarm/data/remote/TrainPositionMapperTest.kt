package com.jin.subwayalarm.data.remote

import com.jin.subwayalarm.data.remote.dto.TrainPositionDto
import com.jin.subwayalarm.domain.model.TrainStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TrainPositionMapperTest {

    private fun validDto() = TrainPositionDto(
        subwayId = "1007",
        trainNo = "7101",
        statnNm = "건대입구",
        statnId = "1007-07",
        updnLine = "0",
        trainSttus = "1",
        directAt = "0",
        lstcarAt = "0",
        recptnDt = "2026-07-01 08:30:00",
    )

    @Test
    fun `정상 DTO 는 도메인으로 매핑됨`() {
        val result = validDto().toDomainOrNull()
        assertNotNull(result)
        assertEquals("1007", result!!.lineId)
        assertEquals("7101", result.trainNo)
        assertEquals("1007-07", result.statnId)
        assertEquals("건대입구", result.statnName)
        assertEquals("0", result.updnLine)
        assertEquals(TrainStatus.ARRIVED, result.status)
        assertTrue(!result.isExpress)
        assertTrue(!result.isLastTrain)
        assertEquals(null, result.terminusName)
    }

    @Test
    fun `statnTnm 은 terminusName 으로 매핑`() {
        val result = validDto().copy(statnTnm = "인천").toDomainOrNull()
        assertEquals("인천", result!!.terminusName)
    }

    @Test
    fun `directAt lstcarAt 이 1 이면 true 로 매핑`() {
        val result = validDto().copy(directAt = "1", lstcarAt = "1").toDomainOrNull()
        assertNotNull(result)
        assertTrue(result!!.isExpress)
        assertTrue(result.isLastTrain)
    }

    @Test
    fun `필수 필드 누락이면 null`() {
        assertNull(validDto().copy(trainNo = null).toDomainOrNull())
        assertNull(validDto().copy(statnId = null).toDomainOrNull())
        assertNull(validDto().copy(statnNm = null).toDomainOrNull())
        assertNull(validDto().copy(subwayId = null).toDomainOrNull())
        assertNull(validDto().copy(updnLine = null).toDomainOrNull())
        assertNull(validDto().copy(recptnDt = null).toDomainOrNull())
    }

    @Test
    fun `trainSttus 코드가 불명이면 null`() {
        assertNull(validDto().copy(trainSttus = "9").toDomainOrNull())
        assertNull(validDto().copy(trainSttus = "abc").toDomainOrNull())
        assertNull(validDto().copy(trainSttus = null).toDomainOrNull())
    }

    @Test
    fun `recptnDt 형식이 잘못되면 null`() {
        assertNull(validDto().copy(recptnDt = "invalid").toDomainOrNull())
    }
}
