package com.foxtive.dominofever.engine.scoring

import com.foxtive.dominofever.model.ScoringKind

object ScorerFactory {
    fun forKind(kind: ScoringKind): ModeScorer = when (kind) {
        ScoringKind.CLASSIC -> ClassicScorer()
        ScoringKind.ALL_FIVES -> AllFivesScorer()
        ScoringKind.ALL_THREES -> AllThreesScorer()
    }
}
