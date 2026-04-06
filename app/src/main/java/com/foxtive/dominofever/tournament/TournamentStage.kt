package com.foxtive.dominofever.tournament

import com.foxtive.dominofever.model.AiDifficulty
import com.foxtive.dominofever.model.GameMode

data class TournamentStage(
    val index: Int,
    val city: String,
    val mode: GameMode,
    val aiDifficulty: AiDifficulty,
    val targetScore: Int,
    val ratingReward: Int
)

data class CareerProgress(
    val currentStageIndex: Int = 0,
    val rating: Int = 0,
    val finished: Boolean = false
)
