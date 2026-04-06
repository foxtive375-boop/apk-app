package com.foxtive.dominofever.engine

import com.foxtive.dominofever.model.Domino
import kotlin.random.Random

class DominoDeckFactory {
    fun createShuffled(random: Random): List<Domino> {
        val deck = mutableListOf<Domino>()
        for (left in 0..6) {
            for (right in left..6) {
                deck += Domino(left, right)
            }
        }
        return deck.shuffled(random)
    }
}
