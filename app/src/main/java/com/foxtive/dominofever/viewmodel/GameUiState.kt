package com.foxtive.dominofever.viewmodel

import com.foxtive.dominofever.model.Domino
import com.foxtive.dominofever.model.MoveOption
import com.foxtive.dominofever.model.TableState

data class GameUiState(
    val hand: List<Domino> = emptyList(),
    val playableIndexes: Set<Int> = emptySet(),
    val selectedDominoIndex: Int? = null,
    val highlightedBranchIds: Set<Int> = emptySet(),
    val pendingBranchChoices: List<MoveOption> = emptyList(),
    val branchPromptToken: Int = 0,
    val table: TableState = TableState(),
    val lastPlacedDominoId: Int? = null,
    val statusText: String = "",
    val scoreText: String = "",
    val modeText: String = "",
    val tournamentText: String = "",
    val canDrawOrPass: Boolean = false,
    val canAdvance: Boolean = false,
    val transientMessage: String? = null
)
