package com.jin.subwayalarm.data.repository

import com.jin.subwayalarm.data.local.SubwaySeed
import com.jin.subwayalarm.data.local.dao.LineMetaDao
import com.jin.subwayalarm.data.local.dao.StationDao
import com.jin.subwayalarm.data.local.toDomain
import com.jin.subwayalarm.data.local.toEntity
import com.jin.subwayalarm.domain.model.LineMeta
import com.jin.subwayalarm.domain.model.Station
import com.jin.subwayalarm.domain.model.StationGraph
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StationRepositoryImpl @Inject constructor(
    private val stationDao: StationDao,
    private val lineMetaDao: LineMetaDao,
) : StationRepository {

    override suspend fun ensureSeeded() {
        // 노선별 개별 시드(누락된 노선만) — 시드 목록이 늘어도 재설치 없이 반영됨.
        SubwaySeed.allLines.forEach { seed ->
            if (stationDao.countForLine(seed.meta.lineId) == 0) {
                lineMetaDao.upsertAll(listOf(seed.meta.toEntity()))
                stationDao.upsertAll(seed.stations.map { it.toEntity() })
            }
        }
    }

    override suspend fun lineMetas(): List<LineMeta> =
        lineMetaDao.all().map { it.toDomain() }

    override suspend fun stations(lineId: String): List<Station> =
        stationDao.stationsForLine(lineId).map { it.toDomain() }

    override suspend fun stationGraph(lineId: String): StationGraph? {
        val stations = stations(lineId)
        return if (stations.isEmpty()) null else StationGraph(stations)
    }
}
