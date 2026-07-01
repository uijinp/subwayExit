package com.jin.subwayalarm.alarm

/** 알람 발화 시 보여줄 문구 종류. 환승 구간이면 Transfer, 마지막 구간이면 FinalDestination. */
sealed class AlarmContent {
    data class Transfer(val transferStationName: String, val nextLineName: String) : AlarmContent()
    data class FinalDestination(val destinationName: String) : AlarmContent()
}
