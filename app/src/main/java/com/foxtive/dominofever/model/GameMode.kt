package com.foxtive.dominofever.model

enum class ScoringKind {
    CLASSIC,
    ALL_FIVES,
    ALL_THREES
}

enum class GameMode(
    val title: String,
    val drawWhenNoMove: Boolean,
    val branchingEnabled: Boolean,
    val scoringKind: ScoringKind
) {
    DRAW(
        title = "Draw Dominoes",
        drawWhenNoMove = true,
        branchingEnabled = false,
        scoringKind = ScoringKind.CLASSIC
    ),
    BLOCK(
        title = "Block Dominoes",
        drawWhenNoMove = false,
        branchingEnabled = false,
        scoringKind = ScoringKind.CLASSIC
    ),
    ALL_FIVES(
        title = "All Fives",
        drawWhenNoMove = true,
        branchingEnabled = false,
        scoringKind = ScoringKind.ALL_FIVES
    ),
    ALL_THREES(
        title = "All Threes",
        drawWhenNoMove = true,
        branchingEnabled = false,
        scoringKind = ScoringKind.ALL_THREES
    ),
    CROSS(
        title = "Cross / Multi-branch",
        drawWhenNoMove = true,
        branchingEnabled = true,
        scoringKind = ScoringKind.CLASSIC
    );

    override fun toString(): String = title
}
