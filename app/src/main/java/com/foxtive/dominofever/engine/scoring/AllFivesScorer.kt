package com.foxtive.dominofever.engine.scoring

class AllFivesScorer : ModeScorer {
    override fun movePoints(openEndsSum: Int): Int = if (openEndsSum % 5 == 0) openEndsSum else 0

    override fun finishPointsWhenDominoOut(opponentPips: Int): Int = opponentPips - (opponentPips % 5)

    override fun finishPointsWhenBlocked(winnerPips: Int, loserPips: Int): Int {
        val diff = (loserPips - winnerPips).coerceAtLeast(0)
        return diff - (diff % 5)
    }
}
