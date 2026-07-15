package com.jin.subwayalarm.data.repository

import com.jin.subwayalarm.data.local.SubwaySeed
import com.jin.subwayalarm.data.local.dao.LineMetaDao
import com.jin.subwayalarm.data.local.dao.StationDao
import com.jin.subwayalarm.data.local.toDomain
import com.jin.subwayalarm.data.local.toEntity
import com.jin.subwayalarm.domain.model.LineMeta
import com.jin.subwayalarm.domain.model.Station
import com.jin.subwayalarm.domain.model.StationGraph
import com.jin.subwayalarm.domain.model.SubwayNetwork
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StationRepositoryImpl @Inject constructor(
    private val stationDao: StationDao,
    private val lineMetaDao: LineMetaDao,
) : StationRepository {

    override suspend fun ensureSeeded() {
        // 역 개수가 시드와 다르면 해당 노선 재적재 (1호선 확장 등 반영).
        SubwaySeed.allLines.forEach { seed ->
            val count = stationDao.countForLine(seed.meta.lineId)
            if (count != seed.stations.size) {
                stationDao.clearLine(seed.meta.lineId)
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
        if (stations.isEmpty()) return null
        return StationGraph(stations, SubwaySeed.edgesFor(lineId))
    }

    override suspend fun buildNetwork(): SubwayNetwork {
        val metas = lineMetas()
        val perLine = metas.associate { it.lineId to stations(it.lineId) }
        val edges = metas.associate { it.lineId to (SubwaySeed.edgesFor(it.lineId) ?: emptyList()) }
            .filterValues { it.isNotEmpty() }
        return SubwayNetwork(perLine, metas, perLineEdges = edges)
    }
}
