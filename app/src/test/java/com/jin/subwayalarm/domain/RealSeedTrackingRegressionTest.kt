package com.jin.subwayalarm.domain

import com.jin.subwayalarm.data.local.SubwaySeed
import com.jin.subwayalarm.domain.model.Direction
import com.jin.subwayalarm.domain.model.DirectionPolarity
import com.jin.subwayalarm.domain.model.Station
import com.jin.subwayalarm.domain.model.StationGraph
import com.jin.subwayalarm.domain.model.SubwayNetwork
import com.jin.subwayalarm.domain.model.TrainPosition
import com.jin.subwayalarm.domain.model.TrainStatus
import com.jin.subwayalarm.domain.model.TripSession
import com.jin.subwayalarm.domain.model.TripState
import com.jin.subwayalarm.domain.usecase.EvaluateAlertUseCase
import com.jin.subwayalarm.domain.usecase.JourneyPlannerUseCase
import com.jin.subwayalarm.domain.usecase.MatchBoardedTrainUseCase
import com.jin.subwayalarm.domain.usecase.TrackingStepInput
import com.jin.subwayalarm.domain.usecase.TrackingStepUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 2026-10-12 재검토(HANDOFF §C)에서 확인한 결함들의 회귀 테스트 — 전부 실제 시드 데이터로.
 * 합성 그래프로는 순환선·분기·보행 환승 같은 실제 노선 모양이 재현되지 않아 이 결함들을 놓쳤다.
 */
class RealSeedTrackingRegressionTest {

    private fun line(lineId: String) = SubwaySeed.allLines.first { it.meta.lineId == lineId }
    private fun graph(lineId: String) = line(lineId).let { StationGraph(it.stations, it.edges) }
    private fun StationGraph.id(name: String): String = stations.first { it.name == name }.statnId

    private val line2 = graph("1002")
    private val line1 = graph("1001")

    // --- P0-7: 2호선 순환을 닫는 간선(충정로 42 ↔ 시청 0) ---

    @Test
    fun `2호선 시청에서 양방향이 서로 다른 방향으로 판정된다`() {
        val toEuljiro = line2.directionBetween(line2.id("시청"), line2.id("을지로입구"))
        val toHongdae = line2.directionBetween(line2.id("시청"), line2.id("홍대입구"))
        assertEquals(Direction.ASCENDING, toEuljiro)
        assertEquals("시청→충정로 쪽은 순번이 줄어드는 방향", Direction.DESCENDING, toHongdae)
    }

    @Test
    fun `2호선 충정로에서 시청으로 넘어가면 순번 증가 방향이다`() {
        assertEquals(Direction.ASCENDING, line2.directionBetween(line2.id("충정로"), line2.id("을지로입구")))
        assertEquals(Direction.DESCENDING, line2.directionBetween(line2.id("충정로"), line2.id("아현")))
    }

    @Test
    fun `2호선 시청과 충정로 사이 1정거장 여정도 방향이 맞다`() {
        assertEquals(Direction.DESCENDING, line2.directionBetween(line2.id("시청"), line2.id("충정로")))
        assertEquals(Direction.ASCENDING, line2.directionBetween(line2.id("충정로"), line2.id("시청")))
    }

    @Test
    fun `직선 노선과 1호선 분기의 방향 판정은 그대로다`() {
        assertEquals(Direction.ASCENDING, line1.directionBetween(line1.id("서울역"), line1.id("천안")))
        assertEquals(Direction.ASCENDING, line1.directionBetween(line1.id("서울역"), line1.id("부평")))
        assertEquals(Direction.DESCENDING, line1.directionBetween(line1.id("부평"), line1.id("서울역")))
    }

    // --- P0-6 / 2호선 종착 판정 ---

    private val match = MatchBoardedTrainUseCase()

    @Test
    fun `1호선 천안행은 부평에 가지 않는다`() {
        assertFalse(match.terminusServesDestination(line1, line1.id("서울역"), line1.id("부평"), "천안"))
        assertTrue(match.terminusServesDestination(line1, line1.id("서울역"), line1.id("부평"), "인천"))
    }

    @Test
    fun `2호선 순환에서 반대쪽이 더 짧은 종착이어도 목적지를 지나면 적합하다`() {
        // 강남→사당(교대 방향)으로 가는 성수행은 한 바퀴를 돌아 성수에 가므로 사당을 지난다.
        assertTrue(match.terminusServesDestination(line2, line2.id("강남"), line2.id("사당"), "성수"))
        // 종착이 출발역 바로 다음 반대편이면 목적지에 못 간다.
        assertFalse(match.terminusServesDestination(line2, line2.id("강남"), line2.id("사당"), "강남"))
    }

    @Test
    fun `목적지에 안 가는 열차가 유일 후보여도 자동 확정하지 않는다`() {
        val step = TrackingStepUseCase(EvaluateAlertUseCase(), match)
        val origin = line1.id("서울역")
        val session = TripSession(
            originStatnId = origin, destStatnId = line1.id("부평"), lineId = "1001",
            currentStatnId = origin, lastSyncAtEpochMillis = 0L,
        )
        val cheonan = TrainPosition(
            lineId = "1001", trainNo = "K100", statnId = origin, statnName = "서울역",
            updnLine = "1", status = TrainStatus.ARRIVED, isExpress = false, isLastTrain = false,
            recptnDtEpochMillis = 0L, terminusName = "천안",
        )
        val result = step(TrackingStepInput(line1, session, listOf(cheonan), 0L, DirectionPolarity("1")))
        assertNull("천안행은 부평에 안 가므로 확정하면 알람이 영영 안 울린다", result.updatedSession.boardedTrainNo)
        assertEquals(TripState.MATCHING, result.updatedSession.state)
        assertEquals("수동 선택용으로는 보여준다", listOf("K100"), result.boardingCandidates.map { it.trainNo })
    }

    @Test
    fun `목적지에 가는 유일 후보는 그대로 자동 확정한다`() {
        val step = TrackingStepUseCase(EvaluateAlertUseCase(), match)
        val origin = line1.id("서울역")
        val session = TripSession(
            originStatnId = origin, destStatnId = line1.id("부평"), lineId = "1001",
            currentStatnId = origin, lastSyncAtEpochMillis = 0L,
        )
        val incheon = TrainPosition(
            lineId = "1001", trainNo = "K200", statnId = origin, statnName = "서울역",
            updnLine = "1", status = TrainStatus.ARRIVED, isExpress = false, isLastTrain = false,
            recptnDtEpochMillis = 0L, terminusName = "인천",
        )
        val result = step(TrackingStepInput(line1, session, listOf(incheon), 0L, DirectionPolarity("1")))
        assertEquals("K200", result.updatedSession.boardedTrainNo)
    }

    // --- P1-2: 시드 구간 밖 역의 열차 ---

    @Test
    fun `시드에 없는 역 ID 로는 현재역을 덮어쓰지 않는다`() {
        val step = TrackingStepUseCase(EvaluateAlertUseCase(), match)
        val origin = line1.id("서울역")
        val session = TripSession(
            originStatnId = origin, destStatnId = line1.id("부평"), lineId = "1001",
            currentStatnId = line1.id("구로"), lastSyncAtEpochMillis = 0L,
            boardedTrainNo = "K200", state = TripState.TRACKING,
        )
        val outside = TrainPosition(
            lineId = "1001", trainNo = "K200", statnId = "1001999999", statnName = "어딘가",
            updnLine = "1", status = TrainStatus.ARRIVED, isExpress = false, isLastTrain = false,
            recptnDtEpochMillis = 0L,
        )
        val result = step(TrackingStepInput(line1, session, listOf(outside), 1_000L, DirectionPolarity("1")))
        assertEquals(line1.id("구로"), result.updatedSession.currentStatnId)
        assertNull(result.alert)
    }

    @Test
    fun `그래프에 없는 역의 남은 정거장 계산은 예외 대신 0`() {
        assertEquals(0, line1.remainingStops("1001999999", line1.id("부평")))
    }

    // --- P0-5: 보행 환승역이 출발·도착일 때 ---

    private val network = SubwayNetwork(
        perLineStations = SubwaySeed.allLines.associate { it.meta.lineId to it.stations },
        lineMetas = SubwaySeed.allLines.map { it.meta },
        perLineEdges = SubwaySeed.allLines.mapNotNull { l -> l.edges?.let { l.meta.lineId to it } }.toMap(),
    )
    private val planner = JourneyPlannerUseCase()

    @Test
    fun `보행 환승역에서 출발하면 타지 않는 구간 없이 바로 환승 노선부터`() {
        val legs = planner(network, "이수", "사당")!!
        assertEquals(1, legs.size)
        assertEquals("1004", legs.single().lineId)
        assertEquals("총신대입구(이수)", legs.single().boardStationName)
        assertEquals("사당", legs.single().alightStationName)
    }

    @Test
    fun `보행 환승역이 도착이면 마지막 빈 구간이 붙지 않는다`() {
        val legs = planner(network, "내방", "총신대입구(이수)")!!
        assertEquals(1, legs.size)
        assertEquals("내방", legs.single().boardStationName)
        assertEquals("이수", legs.single().alightStationName)
    }

    @Test
    fun `어떤 경로도 출발과 도착이 같은 구간을 만들지 않는다`() {
        val names = SubwaySeed.allLines.flatMap { it.stations }.map(Station::name).distinct().sorted()
        val sample = names.filterIndexed { i, _ -> i % 37 == 0 }
        for (from in sample) for (to in sample) {
            if (from == to) continue
            val legs = planner(network, from, to) ?: continue
            assertTrue("$from→$to: $legs", legs.none { it.boardStationName == it.alightStationName })
        }
    }
}
