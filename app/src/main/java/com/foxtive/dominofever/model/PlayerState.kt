package com.foxtive.dominofever.model

data class PlayerState(
    val id: Int,
    val name: String,
    val isHuman: Boolean,
    val difficulty: AiDifficulty,
    val hand: List<Domino>,
    val score: Int = 0,
    val roundsWon: Int = 0
) {
    val handPips: Int
        get() = hand.sumOf { it.pipSum }
}
