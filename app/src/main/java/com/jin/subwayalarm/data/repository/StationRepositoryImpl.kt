package com.jin.subwayalarm.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.jin.subwayalarm.data.local.SeedFingerprint
import com.jin.subwayalarm.data.local.SubwayDatabase
import com.jin.subwayalarm.data.local.SubwaySeed
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    private val database: SubwayDatabase,
    private val stationDao: StationDao,
    private val lineMetaDao: LineMetaDao,
    @ApplicationContext context: Context,
) : StationRepository {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val seedLock = Mutex()

    /**
     * 시드 지문이 바뀌었거나 역 수가 어긋나면 역·노선 전체를 한 트랜잭션으로 다시 넣는다.
     * 역·노선은 코드에 든 정적 데이터라 Room 은 캐시일 뿐 — 통째 재적재해도 수 ms 이고, 사용자
     * 데이터(즐겨찾기·진행 중 여정)는 다른 테이블이라 건드리지 않는다.
     */
    override suspend fun ensureSeeded() = seedLock.withLock {
        val lines = SubwaySeed.allLines
        val fingerprint = SeedFingerprint.of(lines)
        val upToDate = prefs.getString(KEY_FINGERPRINT, null) == fingerprint &&
            lines.all { stationDao.countForLine(it.meta.lineId) == it.stations.size }
        if (upToDate) return@withLock
        database.withTransaction {
            stationDao.clearAll()
            lineMetaDao.clearAll()
            lineMetaDao.upsertAll(lines.map { it.meta.toEntity() })
            stationDao.upsertAll(lines.flatMap { it.stations }.map { it.toEntity() })
        }
        prefs.edit().putString(KEY_FINGERPRINT, fingerprint).apply()
    }

    private companion object {
        const val PREFS_NAME = "seed"
        const val KEY_FINGERPRINT = "fingerprint"
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
