package com.jin.subwayalarm.ui.screen.home

import com.jin.subwayalarm.domain.model.FavoriteRoute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 별 아이콘 채움 여부를 좌우하는 UiState 파생 로직 (순수). */
class HomeUiStateFavoriteTest {

    private val seoulToInchon = FavoriteRoute("서울역", "인천")

    @Test
    fun `출발과 도착이 모두 있어야 현재 경로가 생긴다`() {
        assertNull(HomeViewModel.UiState().currentRoute)
        assertNull(HomeViewModel.UiState(originName = "서울역").currentRoute)
        assertNull(HomeViewModel.UiState(destinationName = "인천").currentRoute)
        assertEquals(
            seoulToInchon,
            HomeViewModel.UiState(originName = "서울역", destinationName = "인천").currentRoute,
        )
    }

    @Test
    fun `저장된 경로면 즐겨찾기로 인식한다`() {
        val state = HomeViewModel.UiState(
            originName = "서울역",
            destinationName = "인천",
            favorites = listOf(seoulToInchon),
        )
        assertTrue(state.isCurrentRouteFavorite)
    }

    @Test
    fun `방향이 반대면 다른 경로로 본다`() {
        val state = HomeViewModel.UiState(
            originName = "인천",
            destinationName = "서울역",
            favorites = listOf(seoulToInchon),
        )
        assertFalse("왕복은 각각 따로 저장돼야 한다", state.isCurrentRouteFavorite)
    }

    @Test
    fun `저장 목록이 비었거나 다른 경로만 있으면 즐겨찾기가 아니다`() {
        assertFalse(
            HomeViewModel.UiState(originName = "서울역", destinationName = "인천").isCurrentRouteFavorite,
        )
        assertFalse(
            HomeViewModel.UiState(
                originName = "서울역",
                destinationName = "인천",
                favorites = listOf(FavoriteRoute("강남", "판교")),
            ).isCurrentRouteFavorite,
        )
    }

    @Test
    fun `역이 하나만 골라진 상태면 즐겨찾기 판정도 false`() {
        assertFalse(
            HomeViewModel.UiState(originName = "서울역", favorites = listOf(seoulToInchon))
                .isCurrentRouteFavorite,
        )
    }
}
