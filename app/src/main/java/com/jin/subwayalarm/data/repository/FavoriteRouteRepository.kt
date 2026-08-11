package com.jin.subwayalarm.data.repository

import com.jin.subwayalarm.domain.model.FavoriteRoute
import kotlinx.coroutines.flow.Flow

interface FavoriteRouteRepository {

    /** 최근 사용 순. */
    fun observeAll(): Flow<List<FavoriteRoute>>

    suspend fun add(route: FavoriteRoute)

    suspend fun remove(route: FavoriteRoute)

    /** 즐겨찾기를 불러 쓸 때 호출 — 최근 사용 시각을 갱신해 목록 위로 올린다. */
    suspend fun markUsed(route: FavoriteRoute)
}
