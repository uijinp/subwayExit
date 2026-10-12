package com.jin.subwayalarm.domain

import org.junit.Assert.assertEquals
import org.junit.Test

/** 노선명에 붙는 "(으)로" — 알람·환승 안내 문구에 그대로 쓰인다. */
class JosaTest {

    @Test
    fun `받침 있으면 으로`() {
        assertEquals("6호선으로", Josa.ro("6호선"))
        assertEquals("경의중앙선으로", Josa.ro("경의중앙선"))
    }

    @Test
    fun `받침 없으면 로`() {
        assertEquals("공항철도로", Josa.ro("공항철도"))
    }

    @Test
    fun `ㄹ 받침은 로`() {
        assertEquals("경전철로", Josa.ro("경전철"))
    }

    @Test
    fun `한글이 아니면 로`() {
        assertEquals("GTX-A로", Josa.ro("GTX-A"))
    }
}
