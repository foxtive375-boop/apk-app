package com.foxtive.dominofever.model

data class BranchState(
    val id: Int,
    val parentDominoId: Int,
    val direction: BranchDirection,
    val openValue: Int,
    val chain: List<Int> = emptyList()
)
