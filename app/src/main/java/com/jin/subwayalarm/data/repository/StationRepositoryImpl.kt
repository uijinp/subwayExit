package com.jin.subwayalarm.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.jin.subwayalarm.data.local.SeedFingerprint
import com.jin.subwayalarm.data.local.SubwayDatabase
import com.jin.subwayalarm.data.local.SubwaySeed
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import com.jin.subwayalarm.data.local.dao.LineMetaDao
import com.jin.subwayalarm.data.local.dao.StationDao
import com.jin.subwayalarm.data.local.toDomain
import com.jin.subwayalarm.data.local.toEntity
import com.jin.subwayalarm.domain.model.LineMeta
import com.jin.subwayalarm.domain.model.Station
import com.jin.subwayalarm.domain.model.StationGraph
import com.jin.subwayalarm.domain.model.SubwayNetwork
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StationRepositoryImpl @Inject constructor(
    private val database: SubwayDatabase,
    private val stationDao: StationDao,
    private val lineMetaDao: LineMetaDao,
    @ApplicationContext context: Context,
) : StationRepository {

    private val appContext = context.applicationContext
    private val prefs by lazy { appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    private val seedLock = Mutex()

    /** 이 프로세스에서 이미 확인·적재를 끝냈는지. 시드는 코드에 든 정적 데이터라 한 번이면 된다. */
    @Volatile private var seededThisProcess = false

    /** 역·노선은 적재 후 바뀌지 않으므로 그래프·전체 노선망을 캐시한다(추적 화면이 매 갱신마다 다시 만들던 것). */
    private val graphCache = ConcurrentHashMap<String, StationGraph>()
    @Volatile private var networkCache: SubwayNetwork? = null

    /**
     * 시드 지문이 바뀌었거나 역 수가 어긋나면 역·노선 전체를 한 트랜잭션으로 다시 넣는다.
     * 역·노선은 코드에 든 정적 데이터라 Room 은 캐시일 뿐 — 통째 재적재해도 수 ms 이고, 사용자
     * 데이터(즐겨찾기·진행 중 여정)는 다른 테이블이라 건드리지 않는다.
     *
     * 시드 객체 초기화와 지문(SHA-256) 계산은 무거워서 메인 스레드에서 돌면 첫 화면이 수 초 늦게 뜬다
     * (viewModelScope 는 첫 suspend 전까지 호출 스레드에서 바로 실행) — Default 디스패처에서 한다.
     */
    override suspend fun ensureSeeded() {
        if (seededThisProcess) return
        withContext(Dispatchers.Default) {
            seedLock.withLock {
                if (seededThisProcess) return@withLock
                val lines = SubwaySeed.allLines
                val fingerprint = SeedFingerprint.of(lines)
                val upToDate = prefs.getString(KEY_FINGERPRINT, null) == fingerprint &&
                    lines.all { stationDao.countForLine(it.meta.lineId) == it.stations.size }
                if (!upToDate) {
                    database.withTransaction {
                        stationDao.clearAll()
                        lineMetaDao.clearAll()
                        lineMetaDao.upsertAll(lines.map { it.meta.toEntity() })
                        stationDao.upsertAll(lines.flatMap { it.stations }.map { it.toEntity() })
                    }
                    prefs.edit().putString(KEY_FINGERPRINT, fingerprint).apply()
                    graphCache.clear()
                    networkCache = null
                }
                seededThisProcess = true
            }
        }
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
        graphCache[lineId]?.let { return it }
        val stations = stations(lineId)
        if (stations.isEmpty()) return null
        val graph = withContext(Dispatchers.Default) { StationGraph(stations, SubwaySeed.edgesFor(lineId)) }
        // 적재 확인 전에 만든 그래프는 재적재로 낡을 수 있어 캐시하지 않는다.
        if (seededThisProcess) graphCache[lineId] = graph
        return graph
    }

    override suspend fun buildNetwork(): SubwayNetwork {
        networkCache?.let { return it }
        val metas = lineMetas()
        val perLine = metas.associate { it.lineId to stations(it.lineId) }
        val network = withContext(Dispatchers.Default) {
            val edges = metas.associate { it.lineId to (SubwaySeed.edgesFor(it.lineId) ?: emptyList()) }
                .filterValues { it.isNotEmpty() }
            SubwayNetwork(perLine, metas, perLineEdges = edges)
        }
        if (seededThisProcess) networkCache = network
        return network
    }
}
