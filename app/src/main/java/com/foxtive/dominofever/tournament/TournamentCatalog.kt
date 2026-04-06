package com.foxtive.dominofever.tournament

import com.foxtive.dominofever.model.AiDifficulty
import com.foxtive.dominofever.model.GameMode

object TournamentCatalog {
    val stages: List<TournamentStage> = listOf(
        TournamentStage(
            index = 0,
            city = "Havana Warm-up",
            mode = GameMode.DRAW,
            aiDifficulty = AiDifficulty.EASY,
            targetScore = 60,
            ratingReward = 25
        ),
        TournamentStage(
            index = 1,
            city = "Lisbon Open",
            mode = GameMode.BLOCK,
            aiDifficulty = AiDifficulty.MEDIUM,
            targetScore = 70,
            ratingReward = 35
        ),
        TournamentStage(
            index = 2,
            city = "Milan Five Masters",
            mode = GameMode.ALL_FIVES,
            aiDifficulty = AiDifficulty.MEDIUM,
            targetScore = 90,
            ratingReward = 50
        ),
        TournamentStage(
            index = 3,
            city = "Kyoto Triple Cup",
            mode = GameMode.ALL_THREES,
            aiDifficulty = AiDifficulty.HARD,
            targetScore = 100,
            ratingReward = 70
        ),
        TournamentStage(
            index = 4,
            city = "Rio Final Crown",
            mode = GameMode.CROSS,
            aiDifficulty = AiDifficulty.HARD,
            targetScore = 120,
            ratingReward = 120
        )
    )

    fun currentStage(progress: CareerProgress): TournamentStage? {
        if (progress.finished) return null
        if (progress.currentStageIndex !in stages.indices) return null
        return stages[progress.currentStageIndex]
    }
}
