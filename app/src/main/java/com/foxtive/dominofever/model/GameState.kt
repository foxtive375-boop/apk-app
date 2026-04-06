package com.foxtive.dominofever.model

data class GameState(
    val config: MatchConfig,
    val players: List<PlayerState>,
    val currentPlayerId: Int,
    val boneyard: List<Domino>,
    val table: TableState,
    val roundNumber: Int,
    val consecutivePasses: Int,
    val status: GameStatus,
    val message: String,
    val winnerId: Int? = null,
    val roundEndReason: RoundEndReason? = null,
    val lastPlacedDominoId: Int? = null
) {
    fun player(id: Int): PlayerState = players.first { it.id == id }

    fun indexOfPlayer(id: Int): Int = players.indexOfFirst { it.id == id }

    fun otherPlayerId(id: Int): Int = players.first { it.id != id }.id
}
