package com.foxtive.dominofever.ai

import com.foxtive.dominofever.model.AiDifficulty
import com.foxtive.dominofever.model.GameState
import com.foxtive.dominofever.model.MoveOption
import com.foxtive.dominofever.model.ScoringKind
import kotlin.random.Random

class DominoAiEngine(
    private val random: Random = Random(System.currentTimeMillis())
) {
    private val memory = AiMemory()

    fun chooseMove(state: GameState, aiPlayerId: Int, options: List<MoveOption>): MoveOption? {
        if (options.isEmpty()) return null

        val difficulty = state.player(aiPlayerId).difficulty
        if (difficulty == AiDifficulty.EASY) {
            return options.random(random)
        }

        memory.refreshFromState(state, aiPlayerId)

        val scored = options.map { option ->
            val score = evaluateOption(state, aiPlayerId, option, difficulty)
            option to score
        }.sortedByDescending { it.second }

        return when (difficulty) {
            AiDifficulty.EASY -> scored.first().first
            AiDifficulty.MEDIUM -> {
                val shortlist = scored.take(3)
                weightedChoice(shortlist)
            }
            AiDifficulty.HARD -> {
                // High level AI mostly plays best line, but occasionally keeps a trap branch open.
                if (scored.size > 1 && random.nextDouble() < 0.2) scored[1].first else scored.first().first
            }
        }
    }

    private fun weightedChoice(scored: List<Pair<MoveOption, Double>>): MoveOption {
        if (scored.isEmpty()) error("scored cannot be empty")
        val normalized = scored.mapIndexed { index, pair ->
            val boost = (scored.size - index).toDouble()
            pair.first to (pair.second.coerceAtLeast(0.1) * boost)
        }
        val total = normalized.sumOf { it.second }
        var cursor = random.nextDouble() * total
        normalized.forEach { (move, value) ->
            cursor -= value
            if (cursor <= 0.0) return move
        }
        return normalized.last().first
    }

    private fun evaluateOption(
        state: GameState,
        aiPlayerId: Int,
        option: MoveOption,
        difficulty: AiDifficulty
    ): Double {
        val currentBranch = state.table.branches.firstOrNull { it.id == option.branchId }
        val openAfter = if (currentBranch == null) {
            0
        } else {
            val branchNewValue = option.domino.other(currentBranch.openValue)
            var sum = state.table.branches.sumOf { branch ->
                if (branch.id == option.branchId) branchNewValue else branch.openValue
            }
            if (state.config.mode.branchingEnabled && option.domino.isDouble) {
                sum += option.domino.left
            }
            sum
        }

        val immediateModePoints = when (state.config.mode.scoringKind) {
            ScoringKind.CLASSIC -> 0
            ScoringKind.ALL_FIVES -> if (openAfter % 5 == 0) openAfter else 0
            ScoringKind.ALL_THREES -> if (openAfter % 3 == 0) openAfter else 0
        }

        val opponentRisk = memory.estimateOpponentHasValue(
            state = state,
            aiPlayerId = aiPlayerId,
            value = option.domino.other(option.matchedValue)
        )

        val closingBonus = (1.0 - opponentRisk) * when (state.config.mode.scoringKind) {
            ScoringKind.ALL_FIVES,
            ScoringKind.ALL_THREES -> 12.0
            ScoringKind.CLASSIC -> 8.0
        }

        val handUnload = option.domino.pipSum * when (difficulty) {
            AiDifficulty.EASY -> 0.3
            AiDifficulty.MEDIUM -> 0.5
            AiDifficulty.HARD -> 0.8
        }

        val bluffFactor = if (difficulty == AiDifficulty.HARD) {
            if (opponentRisk < 0.35) 4.5 else -2.0
        } else {
            if (opponentRisk < 0.4) 2.0 else 0.0
        }

        val branchControl = if (state.config.mode.branchingEnabled && option.domino.isDouble) 6.0 else 0.0

        return immediateModePoints * 2.0 + closingBonus + handUnload + bluffFactor + branchControl
    }
}
