package com.jin.subwayalarm.ui.component

import org.junit.Assert.assertEquals
import org.junit.Test

class LineLabelTest {

    @Test
    fun `번호 노선은 숫자만`() {
        assertEquals("3", lineShortLabel("3호선"))
        assertEquals("1", lineShortLabel("1호선"))
    }

    @Test
    fun `그 외 노선은 선·라인 꼬리를 뗀다`() {
        assertEquals("신분당", lineShortLabel("신분당선"))
        assertEquals("김포골드", lineShortLabel("김포골드라인"))
        assertEquals("인천1", lineShortLabel("인천1호선"))
        assertEquals("공항철도", lineShortLabel("공항철도"))
        assertEquals("GTX-A", lineShortLabel("GTX-A"))
    }

    @Test
    fun `역 노선 힌트는 번호 노선을 묶는다`() {
        assertEquals("3·8호선", compactLineNames(listOf("3호선", "8호선")))
        assertEquals("2호선·신분당선", compactLineNames(listOf("2호선", "신분당선")))
        assertEquals("경의중앙선", compactLineNames(listOf("경의중앙선")))
    }
}
