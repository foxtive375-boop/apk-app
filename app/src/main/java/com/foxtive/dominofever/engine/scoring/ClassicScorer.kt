package com.foxtive.dominofever.engine.scoring

class ClassicScorer : ModeScorer {
    override fun movePoints(openEndsSum: Int): Int = 0

    override fun finishPointsWhenDominoOut(opponentPips: Int): Int = opponentPips

    override fun finishPointsWhenBlocked(winnerPips: Int, loserPips: Int): Int {
        return (loserPips - winnerPips).coerceAtLeast(0)
    }
}
