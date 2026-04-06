package com.foxtive.dominofever.engine.scoring

interface ModeScorer {
    fun movePoints(openEndsSum: Int): Int
    fun finishPointsWhenDominoOut(opponentPips: Int): Int
    fun finishPointsWhenBlocked(winnerPips: Int, loserPips: Int): Int
}
