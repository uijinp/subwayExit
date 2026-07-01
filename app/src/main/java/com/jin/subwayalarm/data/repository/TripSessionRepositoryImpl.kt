package com.jin.subwayalarm.data.repository

import com.jin.subwayalarm.data.local.dao.TripSessionDao
import com.jin.subwayalarm.data.local.toDomain
import com.jin.subwayalarm.data.local.toEntity
import com.jin.subwayalarm.domain.model.TripSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TripSessionRepositoryImpl @Inject constructor(
    private val dao: TripSessionDao,
) : TripSessionRepository {

    override suspend fun current(): TripSession? = dao.get()?.toDomain()

    override fun observeCurrent(): Flow<TripSession?> = dao.observe().map { it?.toDomain() }

    override suspend fun save(session: TripSession) = dao.upsert(session.toEntity())

    override suspend fun clear() = dao.clear()
}
