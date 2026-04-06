package com.foxtive.dominofever.engine

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeckFactoryTest {

    @Test
    fun factoryBuildsStandardDoubleSixSet() {
        val deck = DominoDeckFactory().createShuffled(Random(0))
        assertEquals(28, deck.size)
        assertEquals(28, deck.map { it.normalizedKey() }.toSet().size)
        assertTrue(deck.all { it.left in 0..6 && it.right in 0..6 })
    }
}
