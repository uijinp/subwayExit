package com.jin.subwayalarm.data.repository

import com.jin.subwayalarm.domain.model.LineMeta
import com.jin.subwayalarm.domain.model.Station
import com.jin.subwayalarm.domain.model.StationGraph
import com.jin.subwayalarm.domain.model.SubwayNetwork

interface StationRepository {

    /** 로컬에 노선 데이터가 없으면 시드(노선별)를 적재. 작업 3에서 API 동기화로 대체/보강. */
    suspend fun ensureSeeded()

    suspend fun lineMetas(): List<LineMeta>

    suspend fun stations(lineId: String): List<Station>

    /** 노선 그래프 구성. 역이 없으면 null. */
    suspend fun stationGraph(lineId: String): StationGraph?

    /** 전 노선을 역명 기준으로 연결한 네트워크(환승 경로 탐색용). */
    suspend fun buildNetwork(): SubwayNetwork
}
