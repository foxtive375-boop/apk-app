package com.foxtive.dominofever.ai

import com.foxtive.dominofever.model.Domino
import com.foxtive.dominofever.model.GameState

class AiMemory {
    private val seenDominoKeys = mutableSetOf<String>()

    fun refreshFromState(state: GameState, aiPlayerId: Int) {
        seenDominoKeys.clear()
        state.table.placed.forEach { seenDominoKeys += it.domino.normalizedKey() }
        state.player(aiPlayerId).hand.forEach { seenDominoKeys += it.normalizedKey() }
    }

    fun estimateOpponentHasValue(state: GameState, aiPlayerId: Int, value: Int): Double {
        val fullSet = buildFullSet()
        val unseen = fullSet.filter { it.normalizedKey() !in seenDominoKeys }
        if (unseen.isEmpty()) return 0.0

        val opponent = state.players.first { it.id != aiPlayerId }
        val matching = unseen.count { it.matches(value) }

        val density = matching.toDouble() / unseen.size.toDouble()
        val handPressure = opponent.hand.size.coerceAtMost(unseen.size).toDouble() / unseen.size.toDouble()
        return (density * handPressure * 1.8).coerceIn(0.0, 1.0)
    }

    private fun buildFullSet(): List<Domino> {
        val deck = mutableListOf<Domino>()
        for (left in 0..6) {
            for (right in left..6) {
                deck += Domino(left, right)
            }
        }
        return deck
    }
}
