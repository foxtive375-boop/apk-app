package com.foxtive.dominofever.engine

import com.foxtive.dominofever.engine.scoring.AllFivesScorer
import com.foxtive.dominofever.engine.scoring.AllThreesScorer
import com.foxtive.dominofever.engine.scoring.ClassicScorer
import org.junit.Assert.assertEquals
import org.junit.Test

class ScoringTest {

    @Test
    fun allFivesMovePointsWork() {
        val scorer = AllFivesScorer()
        assertEquals(10, scorer.movePoints(10))
        assertEquals(0, scorer.movePoints(9))
    }

    @Test
    fun allThreesMovePointsWork() {
        val scorer = AllThreesScorer()
        assertEquals(12, scorer.movePoints(12))
        assertEquals(0, scorer.movePoints(10))
    }

    @Test
    fun classicBlockedUsesDifference() {
        val scorer = ClassicScorer()
        assertEquals(9, scorer.finishPointsWhenBlocked(3, 12))
    }
}
