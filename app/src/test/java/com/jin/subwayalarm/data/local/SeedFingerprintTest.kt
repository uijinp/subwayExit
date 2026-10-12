package com.jin.subwayalarm.data.local

import com.jin.subwayalarm.domain.model.DirectionPolarity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * 시드 지문이 "기존 설치에 반영돼야 하는 변경"을 빠짐없이 감지하는지.
 * 예전엔 역 개수만 봐서 순서·이름·극성 수정이 기존 기기에 안 들어갔다(HANDOFF §C P0-2).
 */
class SeedFingerprintTest {

    private val lines = SubwaySeed.allLines

    @Test
    fun `같은 시드면 지문이 같다 - 실행마다 흔들리지 않는다`() {
        assertEquals(SeedFingerprint.of(lines), SeedFingerprint.of(lines.map { it.copy() }))
    }

    @Test
    fun `방향 극성만 바뀌어도 지문이 달라진다`() {
        val flipped = lines.map { l ->
            if (l.meta.lineId != "1002") l
            else l.copy(meta = l.meta.copy(directionPolarity = DirectionPolarity("1")))
        }
        assertNotEquals(SeedFingerprint.of(lines), SeedFingerprint.of(flipped))
    }

    @Test
    fun `역 개수는 그대로고 순서만 바뀌어도 지문이 달라진다`() {
        val reordered = lines.map { l ->
            if (l.meta.lineId != "1077") l
            else l.copy(stations = l.stations.map { it.copy(order = l.stations.size - 1 - it.order) })
        }
        assertNotEquals(SeedFingerprint.of(lines), SeedFingerprint.of(reordered))
    }

    @Test
    fun `역명만 바뀌어도 지문이 달라진다`() {
        val renamed = lines.map { l ->
            if (l.meta.lineId != "1063") l
            else l.copy(stations = l.stations.map { if (it.order == 0) it.copy(name = it.name + "X") else it })
        }
        assertNotEquals(SeedFingerprint.of(lines), SeedFingerprint.of(renamed))
    }

    @Test
    fun `노선 나열 순서는 지문에 영향이 없다`() {
        assertEquals(SeedFingerprint.of(lines), SeedFingerprint.of(lines.reversed()))
    }
}
