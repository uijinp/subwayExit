package com.jin.subwayalarm.domain

/**
 * 한국어 조사 선택. 노선명이 "6호선"(받침 있음)·"공항철도"(받침 없음)·"GTX-A"(라틴) 처럼 섞여 있어서
 * "${line}으로" 를 고정으로 쓰면 "공항철도으로" 가 된다.
 */
object Josa {

    /** "6호선으로", "공항철도로", "경의중앙선으로", "GTX-A로". ㄹ 받침은 "로"(예: "…철로"). */
    fun ro(word: String): String {
        val jong = jongseong(word) ?: return word + "로"
        return if (jong == 0 || jong == RIEUL) word + "로" else word + "으로"
    }

    /** 마지막 글자가 한글 음절이면 종성 인덱스(0 = 받침 없음), 아니면 null. */
    private fun jongseong(word: String): Int? {
        val c = word.trimEnd().lastOrNull() ?: return null
        if (c !in '가'..'힣') return null
        return (c - '가') % 28
    }

    private const val RIEUL = 8
}
