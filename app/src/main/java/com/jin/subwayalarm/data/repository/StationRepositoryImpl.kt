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
        if (stationDao.countForLine(SubwaySeed.LINE_7_ID) > 0) return
        lineMetaDao.upsertAll(listOf(SubwaySeed.line7Meta.toEntity()))
        stationDao.upsertAll(SubwaySeed.line7Stations.map { it.toEntity() })
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
