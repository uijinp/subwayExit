package com.jin.subwayalarm.data.local

import com.jin.subwayalarm.data.local.entity.LineMetaEntity
import com.jin.subwayalarm.data.local.entity.StationEntity
import com.jin.subwayalarm.data.local.entity.TripSessionEntity
import com.jin.subwayalarm.domain.model.DirectionPolarity
import com.jin.subwayalarm.domain.model.LineMeta
import com.jin.subwayalarm.domain.model.LineTopology
import com.jin.subwayalarm.domain.model.Station
import com.jin.subwayalarm.domain.model.TrackingMode
import com.jin.subwayalarm.domain.model.TripSession
import com.jin.subwayalarm.domain.model.TripState

fun StationEntity.toDomain(): Station = Station(
    statnId = statnId,
    lineId = lineId,
    name = statnName,
    order = orderInLine,
    avgSecToNext = avgSecToNext,
    nextStatnId = nextStatnId,
    prevStatnId = prevStatnId,
)

fun Station.toEntity(): StationEntity = StationEntity(
    statnId = statnId,
    lineId = lineId,
    statnName = name,
    orderInLine = order,
    avgSecToNext = avgSecToNext,
    nextStatnId = nextStatnId,
    prevStatnId = prevStatnId,
)

fun LineMetaEntity.toDomain(): LineMeta = LineMeta(
    lineId = lineId,
    lineName = lineName,
    supportsRealtime = supportsRealtime,
    topology = runCatching { LineTopology.valueOf(topology) }.getOrDefault(LineTopology.LINEAR),
    directionPolarity = ascendingUpdnLine?.let { DirectionPolarity(it) },
)

fun LineMeta.toEntity(): LineMetaEntity = LineMetaEntity(
    lineId = lineId,
    lineName = lineName,
    supportsRealtime = supportsRealtime,
    topology = topology.name,
    ascendingUpdnLine = directionPolarity?.ascendingUpdnLine,
)

fun TripSessionEntity.toDomain(): TripSession = TripSession(
    originStatnId = originStatnId,
    destStatnId = destStatnId,
    lineId = lineId,
    currentStatnId = currentStatnId,
    lastSyncAtEpochMillis = lastSyncAtEpochMillis,
    boardedTrainNo = boardedTrainNo,
    mode = runCatching { TrackingMode.valueOf(mode) }.getOrDefault(TrackingMode.API),
    state = runCatching { TripState.valueOf(state) }.getOrDefault(TripState.MATCHING),
)

fun TripSession.toEntity(): TripSessionEntity = TripSessionEntity(
    originStatnId = originStatnId,
    destStatnId = destStatnId,
    lineId = lineId,
    currentStatnId = currentStatnId,
    lastSyncAtEpochMillis = lastSyncAtEpochMillis,
    boardedTrainNo = boardedTrainNo,
    mode = mode.name,
    state = state.name,
)
