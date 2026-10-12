package com.jin.subwayalarm.alarm

/**
 * 알람 발화 시 보여줄 문구. 환승 구간이면 Transfer, 마지막 구간이면 FinalDestination.
 *
 * [stationName] 은 **내릴 역**(환승역 또는 목적지)이다. 예전엔 알람이 울린 역(직전역)을 넘겨서
 * "내방역에서 내려 4호선으로 갈아타세요" 처럼 한 역 일찍 내리라고 안내했다(실제 환승역은 이수).
 * 알람이 울린 역은 [passedStationName], 내릴 역까지 남은 정거장은 [stopsAway].
 */
sealed class AlarmContent {
    abstract val stationName: String
    abstract val passedStationName: String
    abstract val stopsAway: Int

    data class Transfer(
        override val stationName: String,
        override val passedStationName: String,
        override val stopsAway: Int,
        val nextLineName: String,
    ) : AlarmContent()

    data class FinalDestination(
        override val stationName: String,
        override val passedStationName: String,
        override val stopsAway: Int,
    ) : AlarmContent()

    /** "다음 정차역" / "2정거장 뒤" */
    val whenLabel: String get() = if (stopsAway <= 1) "다음 정차역" else "${stopsAway}정거장 뒤"

    val title: String
        get() = when (this) {
            is Transfer -> "${stationName}에서 환승하세요"
            is FinalDestination -> "${stationName}에서 내리세요"
        }

    val body: String
        get() = when (this) {
            is Transfer -> "$passedStationName 통과 · $whenLabel $stationName · ${nextLineName}으로 갈아타세요"
            is FinalDestination -> "$passedStationName 통과 · $whenLabel $stationName"
        }
}
