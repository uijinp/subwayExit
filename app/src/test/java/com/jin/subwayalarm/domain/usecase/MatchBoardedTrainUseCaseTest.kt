package com.jin.subwayalarm.domain.usecase

import com.jin.subwayalarm.domain.model.Direction
import com.jin.subwayalarm.domain.model.DirectionPolarity
import com.jin.subwayalarm.domain.model.TrainPosition
import com.jin.subwayalarm.domain.model.TrainStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MatchBoardedTrainUseCaseTest {

    private val useCase = MatchBoardedTrainUseCase()
    private val polarity = DirectionPolarity(ascendingUpdnLine = "0")

    private fun position(
        trainNo: String,
        statnId: String,
        updnLine: String,
        status: TrainStatus,
        lineId: String = "L",
        terminusName: String? = null,
    ) = TrainPosition(
        lineId = lineId,
        trainNo = trainNo,
        statnId = statnId,
        statnName = "역",
        updnLine = updnLine,
        status = status,
        isExpress = false,
        isLastTrain = false,
        recptnDtEpochMillis = 0L,
        terminusName = terminusName,
    )

    @Test
    fun `방향과 상태가 일치하는 후보가 1개면 확정`() {
        val positions = listOf(
            position("101", "ORIGIN", updnLine = "0", status = TrainStatus.ARRIVED),
            position("102", "ORIGIN", updnLine = "1", status = TrainStatus.ARRIVED),
            position("103", "OTHER", updnLine = "0", status = TrainStatus.ARRIVED),
        )
        val result = useCase(positions, "L", "ORIGIN", Direction.ASCENDING, polarity)
        assertTrue(result is BoardingMatch.Matched)
        assertEquals("101", (result as BoardingMatch.Matched).trainNo)
    }

    @Test
    fun `후보가 없으면 NoCandidate`() {
        val result = useCase(emptyList(), "L", "ORIGIN", Direction.ASCENDING, polarity)
        assertEquals(BoardingMatch.NoCandidate, result)
    }

    @Test
    fun `후보가 여러개면 Ambiguous`() {
        val positions = listOf(
            position("101", "ORIGIN", updnLine = "0", status = TrainStatus.ARRIVED),
            position("102", "ORIGIN", updnLine = "0", status = TrainStatus.APPROACHING),
        )
        val result = useCase(positions, "L", "ORIGIN", Direction.ASCENDING, polarity)
        assertTrue(result is BoardingMatch.Ambiguous)
        assertEquals(2, (result as BoardingMatch.Ambiguous).candidates.size)
    }

    @Test
    fun `출발(DEPARTED) 상태 후보도 인정`() {
        val positions = listOf(position("101", "ORIGIN", updnLine = "0", status = TrainStatus.DEPARTED))
        val result = useCase(positions, "L", "ORIGIN", Direction.ASCENDING, polarity)
        assertTrue(result is BoardingMatch.Matched)
    }

    @Test
    fun `다른 노선 열차는 후보에서 제외`() {
        val positions = listOf(position("101", "ORIGIN", updnLine = "0", status = TrainStatus.ARRIVED, lineId = "OTHER_LINE"))
        val result = useCase(positions, "L", "ORIGIN", Direction.ASCENDING, polarity)
        assertEquals(BoardingMatch.NoCandidate, result)
    }

    @Test
    fun `경부 종착 열차는 제외 목록으로 걸러진다`() {
        val positions = listOf(
            position("101", "ORIGIN", updnLine = "0", status = TrainStatus.ARRIVED, terminusName = "수원"),
            position("102", "ORIGIN", updnLine = "0", status = TrainStatus.ARRIVED, terminusName = "인천"),
        )
        val result = useCase(
            positions, "L", "ORIGIN", Direction.ASCENDING, polarity,
            excludedTerminusNames = setOf("수원", "천안"),
        )
        assertTrue(result is BoardingMatch.Matched)
        assertEquals("102", (result as BoardingMatch.Matched).trainNo)
    }

    @Test
    fun `1호선 동두천-천안 - 인천행은 걸러지고 천안행만 남는다`() {
        val seed = com.jin.subwayalarm.data.local.SubwaySeed.line1
        val graph = com.jin.subwayalarm.domain.model.StationGraph(seed.stations, seed.edges)
        val origin = seed.stations.first { it.name == "동두천" }
        val dest = seed.stations.first { it.name == "천안" }
        val polarityL1 = DirectionPolarity("1") // order 증가 = updn "1"
        val direction = graph.directionBetween(origin.statnId, dest.statnId)
        val positions = listOf(
            TrainPosition(
                lineId = "1001", trainNo = "INCHEON", statnId = origin.statnId, statnName = "동두천",
                updnLine = "1", status = TrainStatus.ARRIVED, isExpress = false, isLastTrain = false,
                recptnDtEpochMillis = 0L, terminusName = "인천",
            ),
            TrainPosition(
                lineId = "1001", trainNo = "CHEONAN", statnId = origin.statnId, statnName = "동두천",
                updnLine = "1", status = TrainStatus.ARRIVED, isExpress = false, isLastTrain = false,
                recptnDtEpochMillis = 0L, terminusName = "천안",
            ),
        )
        val list = useCase.candidates(
            positions, "1001", origin.statnId, direction, polarityL1,
            graph = graph, destStatnId = dest.statnId,
        )
        assertEquals(listOf("CHEONAN"), list.map { it.trainNo })
    }

    @Test
    fun `1호선 동두천-인천 - 천안행은 걸러지고 인천행만 남는다`() {
        val seed = com.jin.subwayalarm.data.local.SubwaySeed.line1
        val graph = com.jin.subwayalarm.domain.model.StationGraph(seed.stations, seed.edges)
        val origin = seed.stations.first { it.name == "동두천" }
        val dest = seed.stations.first { it.name == "인천" }
        val polarityL1 = DirectionPolarity("1")
        val direction = graph.directionBetween(origin.statnId, dest.statnId)
        val positions = listOf(
            TrainPosition(
                lineId = "1001", trainNo = "INCHEON", statnId = origin.statnId, statnName = "동두천",
                updnLine = "1", status = TrainStatus.ARRIVED, isExpress = false, isLastTrain = false,
                recptnDtEpochMillis = 0L, terminusName = "인천",
            ),
            TrainPosition(
                lineId = "1001", trainNo = "CHEONAN", statnId = origin.statnId, statnName = "동두천",
                updnLine = "1", status = TrainStatus.ARRIVED, isExpress = false, isLastTrain = false,
                recptnDtEpochMillis = 0L, terminusName = "천안",
            ),
        )
        val list = useCase.candidates(
            positions, "1001", origin.statnId, direction, polarityL1,
            graph = graph, destStatnId = dest.statnId,
        )
        assertEquals(listOf("INCHEON"), list.map { it.trainNo })
    }
}
