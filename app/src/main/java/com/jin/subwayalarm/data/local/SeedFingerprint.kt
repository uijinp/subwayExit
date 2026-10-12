package com.jin.subwayalarm.data.local

import java.security.MessageDigest

/**
 * 역·노선 시드 전체의 지문(SHA-256). 기기에 저장된 지문과 다르면 Room 의 시드를 통째로 다시 넣는다.
 *
 * 예전엔 노선의 역 "개수"가 달라질 때만 재적재해서, 개수는 그대로인 수정 — 역 순서 재배열,
 * 역명 정정, 방향 극성(updnLine) 정정 — 이 이미 설치된 기기에 영영 반영되지 않았다
 * (극성은 Room 의 LineMetaEntity 에서 읽으므로, 2호선 극성을 고쳐도 기존 사용자는 계속 반대 방향 열차를 잡았다).
 *
 * enum 의 hashCode 는 실행마다 달라지므로 쓰지 않고, 모든 값을 이름·문자열로 직렬화해 해시한다.
 */
object SeedFingerprint {

    fun of(lines: List<SubwaySeed.LineSeed>): String {
        val text = buildString {
            for (line in lines.sortedBy { it.meta.lineId }) {
                val m = line.meta
                append("L|").append(m.lineId).append('|').append(m.lineName).append('|')
                    .append(m.supportsRealtime).append('|').append(m.topology.name).append('|')
                    .append(m.directionPolarity?.ascendingUpdnLine ?: "-").append('|')
                    .append(m.displayName).append('|')
                    .append(m.excludedTerminusNames.sorted().joinToString(",")).append('\n')
                for (s in line.stations.sortedBy { it.statnId }) {
                    append("S|").append(s.statnId).append('|').append(s.lineId).append('|')
                        .append(s.name).append('|').append(s.order).append('|')
                        .append(s.avgSecToNext).append('|').append(s.lat).append('|').append(s.lon).append('|')
                        .append(s.nextStatnId ?: "-").append('|').append(s.prevStatnId ?: "-").append('\n')
                }
                line.edges?.forEach { (a, b) -> append("E|").append(a).append('|').append(b).append('\n') }
            }
        }
        val digest = MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
