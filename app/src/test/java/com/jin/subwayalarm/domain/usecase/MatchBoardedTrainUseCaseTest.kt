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
    )

    @Test
    fun `방향과 상태가 일치하는 후보가 1개면 확정`() {
        val positions = listOf(
            position("101", "ORIGIN", updnLine = "0", status = TrainStatus.ARRIVED),
            position("102", "ORIGIN", updnLine = "1", status = TrainStatus.ARRIVED), // 반대 방향
            position("103", "OTHER", updnLine = "0", status = TrainStatus.ARRIVED), // 다른 역
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
        assertEquals(2, (result as BoardingMatch.Ambiguous).candidateTrainNos.size)
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
}
