package com.jin.subwayalarm.domain.usecase

import com.jin.subwayalarm.data.local.SubwaySeed
import com.jin.subwayalarm.domain.model.Direction
import com.jin.subwayalarm.domain.model.TrainPosition
import com.jin.subwayalarm.domain.model.TrainStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 노선 현황판 매핑 검증 — 실제 7호선 시드 데이터 기반.
 */
class BuildLineBoardUseCaseTest {

    private val useCase = BuildLineBoardUseCase()
    private val line7 = SubwaySeed.allLines.first { it.meta.lineId == "1007" }

    private fun position(
        statnId: String,
        trainNo: String = "7123",
        updnLine: String = line7.meta.directionPolarity!!.ascendingUpdnLine,
        status: TrainStatus = TrainStatus.ARRIVED,
        lineId: String = "1007",
        isExpress: Boolean = false,
    ) = TrainPosition(
        lineId = lineId,
        trainNo = trainNo,
        statnId = statnId,
        statnName = "",
        updnLine = updnLine,
        status = status,
        isExpress = isExpress,
        isLastTrain = false,
        recptnDtEpochMillis = 0L,
    )

    @Test
    fun `역 순서대로 전체 역이 나오고 열차는 해당 역 줄에 붙는다`() {
        val target = line7.stations[5]
        val rows = useCase(line7.stations, line7.meta.directionPolarity, listOf(position(target.statnId)), "1007")

        assertEquals(line7.stations.size, rows.size)
        assertEquals(line7.stations.sortedBy { it.order }.map { it.statnId }, rows.map { it.station.statnId })
        assertEquals(1, rows[5].trains.size)
        assertEquals("7123", rows[5].trains.first().trainNo)
        assertTrue(rows.filterIndexed { i, _ -> i != 5 }.all { it.trains.isEmpty() })
    }

    @Test
    fun `같은 역에 여러 열차가 있으면 전부 표시된다`() {
        val target = line7.stations[10]
        val rows = useCase(
            line7.stations,
            line7.meta.directionPolarity,
            listOf(position(target.statnId, trainNo = "7001"), position(target.statnId, trainNo = "7002", updnLine = "9")),
            "1007",
        )
        assertEquals(listOf("7001", "7002"), rows[10].trains.map { it.trainNo })
        // 극성과 일치하는 updnLine 은 ASCENDING, 그 외 코드는 DESCENDING.
        assertEquals(Direction.ASCENDING, rows[10].trains[0].direction)
        assertEquals(Direction.DESCENDING, rows[10].trains[1].direction)
    }

    @Test
    fun `다른 노선 열차는 제외된다`() {
        val target = line7.stations[3]
        val rows = useCase(line7.stations, line7.meta.directionPolarity, listOf(position(target.statnId, lineId = "1003")), "1007")
        assertTrue(rows.all { it.trains.isEmpty() })
    }

    @Test
    fun `시드 구간 밖 statnId 열차는 표시할 줄이 없어 제외된다`() {
        val rows = useCase(line7.stations, line7.meta.directionPolarity, listOf(position(statnId = "1007999999")), "1007")
        assertTrue(rows.all { it.trains.isEmpty() })
    }

    @Test
    fun `극성 미검증 노선이면 방향은 null 로 내려간다`() {
        val target = line7.stations[0]
        val rows = useCase(line7.stations, null, listOf(position(target.statnId)), "1007")
        assertNull(rows[0].trains.first().direction)
    }

    @Test
    fun `같은 trainNo 중복 행은 최신 recptnDt 한 건만 남는다`() {
        // 실측(1호선): API 가 같은 열차를 recptnDt 만 다른 중복 행으로 반환하는 경우가 있다.
        val older = line7.stations[4]
        val newer = line7.stations[5]
        val rows = useCase(
            line7.stations,
            line7.meta.directionPolarity,
            listOf(
                position(older.statnId, trainNo = "7100").copy(recptnDtEpochMillis = 1_000L),
                position(newer.statnId, trainNo = "7100").copy(recptnDtEpochMillis = 2_000L),
            ),
            "1007",
        )
        assertTrue(rows[4].trains.isEmpty())
        assertEquals(listOf("7100"), rows[5].trains.map { it.trainNo })
    }

    @Test
    fun `급행 플래그가 마커에 전달된다`() {
        val target = line7.stations[7]
        val rows = useCase(line7.stations, line7.meta.directionPolarity, listOf(position(target.statnId, isExpress = true)), "1007")
        assertTrue(rows[7].trains.first().isExpress)
    }
}
