package com.jin.subwayalarm.data.repository

import com.jin.subwayalarm.data.local.dao.FavoriteRouteDao
import com.jin.subwayalarm.data.local.entity.FavoriteRouteEntity
import com.jin.subwayalarm.domain.model.FavoriteRoute
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FavoriteRouteRepositoryImpl @Inject constructor(
    private val dao: FavoriteRouteDao,
) : FavoriteRouteRepository {

    override fun observeAll(): Flow<List<FavoriteRoute>> =
        dao.observeAll().map { list -> list.map { FavoriteRoute(it.originName, it.destinationName) } }

    override suspend fun add(route: FavoriteRoute) {
        val now = System.currentTimeMillis()
        dao.upsert(
            FavoriteRouteEntity(
                originName = route.originName,
                destinationName = route.destinationName,
                createdAtEpochMillis = now,
                lastUsedAtEpochMillis = now,
            ),
        )
    }

    override suspend fun remove(route: FavoriteRoute) =
        dao.delete(route.originName, route.destinationName)

    override suspend fun markUsed(route: FavoriteRoute) =
        dao.touch(route.originName, route.destinationName, System.currentTimeMillis())
}
