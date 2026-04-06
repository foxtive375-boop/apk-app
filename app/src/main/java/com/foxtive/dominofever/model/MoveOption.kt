package com.foxtive.dominofever.model

data class MoveOption(
    val playerId: Int,
    val dominoIndex: Int,
    val domino: Domino,
    val branchId: Int,
    val matchedValue: Int
)
