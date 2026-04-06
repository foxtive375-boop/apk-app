package com.foxtive.dominofever.model

data class PlacedDomino(
    val id: Int,
    val domino: Domino,
    val ownerId: Int,
    val branchId: Int?,
    val matchedValue: Int?
)
