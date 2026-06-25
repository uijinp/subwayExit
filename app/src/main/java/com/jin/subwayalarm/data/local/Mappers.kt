package com.jin.subwayalarm.data.local

import com.jin.subwayalarm.data.local.entity.LineMetaEntity
import com.jin.subwayalarm.data.local.entity.StationEntity
import com.jin.subwayalarm.domain.model.LineMeta
import com.jin.subwayalarm.domain.model.LineTopology
import com.jin.subwayalarm.domain.model.Station

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
)

fun LineMeta.toEntity(): LineMetaEntity = LineMetaEntity(
    lineId = lineId,
    lineName = lineName,
    supportsRealtime = supportsRealtime,
    topology = topology.name,
)
