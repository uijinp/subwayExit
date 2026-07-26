package com.jin.subwayalarm.domain.usecase

import com.jin.subwayalarm.data.local.SubwaySeed
import com.jin.subwayalarm.domain.model.Station
import com.jin.subwayalarm.domain.model.TrainPosition
import com.jin.subwayalarm.domain.model.TrainStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NormalizePositionsUseCaseTest {

    private val useCase = NormalizePositionsUseCase()

    private val stations = listOf(
        Station("L100", "L", "가역", 0, 100),
        Station("L101", "L", "나역", 1, 100),
        Station("L102", "L", "다역", 2, 100),
    )

    private fun position(statnId: String, statnName: String, trainNo: String = "T1") = TrainPosition(
        lineId = "L", trainNo = trainNo, statnId = statnId, statnName = statnName,
        updnLine = "1", status = TrainStatus.ARRIVED, isExpress = false, isLastTrain = false,
        recptnDtEpochMillis = 0L,
    )

    @Test
    fun `시드에 있는 statnId 는 그대로 둔다`() {
        val input = listOf(position("L101", "나역"))
        assertEquals(input, useCase(input, stations))
    }

    @Test
    fun `statnId 가 어긋나면 역명으로 찾아 시드 ID 로 교정한다`() {
        // 실제 사례: 경의중앙선 옥수 — 시드 1063000114 vs API 1063075114
        val result = useCase(listOf(position("L999", "나역")), stations)
        assertEquals("L101", result.single().statnId)
        // 다른 필드는 보존
        assertEquals("나역", result.single().statnName)
        assertEquals("T1", result.single().trainNo)
    }

    @Test
    fun `괄호 부기나 공백만 다른 표기도 같은 역으로 인식한다`() {
        // 실제 사례: 시드 "청량리(서울시립대입구)" ↔ API "청량리", "4.19민주묘지" ↔ "4.19 민주묘지"
        val seed = listOf(
            Station("S1", "L", "청량리(서울시립대입구)", 0, 100),
            Station("S2", "L", "4.19민주묘지", 1, 100),
        )
        assertEquals("S1", useCase(listOf(position("X", "청량리")), seed).single().statnId)
        assertEquals("S2", useCase(listOf(position("X", "4.19 민주묘지")), seed).single().statnId)
    }

    @Test
    fun `API 가 서울역을 서울 로 줘도 같은 역으로 인식한다`() {
        val seed = listOf(Station("S1", "L", "서울역", 0, 100))
        assertEquals("S1", useCase(listOf(position("X", "서울")), seed).single().statnId)
    }

    @Test
    fun `괄호를 떼면 같아지는 역이 둘이면 추측하지 않고 원본 유지`() {
        val ambiguous = listOf(
            Station("S1", "L", "종합운동장(가)", 0, 100),
            Station("S2", "L", "종합운동장(나)", 1, 100),
        )
        val input = listOf(position("X", "종합운동장"))
        assertEquals(input, useCase(input, ambiguous))
    }

    @Test
    fun `statnId 도 역명도 모르면 원본을 유지한다 (시드 구간 밖 열차)`() {
        val input = listOf(position("L999", "모르는역"))
        assertEquals(input, useCase(input, stations))
    }

    @Test
    fun `빈 입력은 그대로 반환`() {
        assertTrue(useCase(emptyList(), stations).isEmpty())
        val input = listOf(position("L100", "가역"))
        assertEquals(input, useCase(input, emptyList()))
    }

    @Test
    fun `여러 열차를 섞어도 각각 올바르게 처리한다`() {
        val result = useCase(
            listOf(
                position("L100", "가역", "A"),      // 정상
                position("L888", "다역", "B"),      // 교정 대상
                position("L777", "없는역", "C"),    // 구간 밖
            ),
            stations,
        )
        assertEquals(listOf("L100", "L102", "L777"), result.map { it.statnId })
    }

    /**
     * 정규화가 역명 폴백에 의존하므로, 한 노선 안에서 역명이 유일해야 한다.
     * (노선 간 동명이역은 별개 문제 — `SeedStationNameCollisionTest` 가 담당)
     */
    @Test
    fun `노선 안에서 역명은 유일해야 한다`() {
        val offenders = mutableListOf<String>()
        for (line in SubwaySeed.allLines) {
            val dups = line.stations.groupBy { it.name }.filterValues { it.size > 1 }
            for ((name, group) in dups) {
                offenders += "${line.meta.lineName}: '$name' ${group.size}개 (${group.map { it.statnId }})"
            }
        }
        assertTrue(
            "한 노선에 같은 역명이 중복되면 statnId 역명 폴백이 엉뚱한 역을 고를 수 있다:\n" +
                offenders.joinToString("\n"),
            offenders.isEmpty(),
        )
    }
}
