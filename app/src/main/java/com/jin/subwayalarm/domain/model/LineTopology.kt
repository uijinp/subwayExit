package com.jin.subwayalarm.domain.model

/**
 * 노선 위상. MVP 는 LINEAR(7호선)만 지원.
 * LOOP(2호선 본선)·BRANCHED(2호선 지선 등)는 작업순서 12에서 확장.
 */
enum class LineTopology {
    LINEAR,
    LOOP,
    BRANCHED,
}
