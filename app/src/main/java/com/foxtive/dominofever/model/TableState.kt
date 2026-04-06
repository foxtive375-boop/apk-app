package com.foxtive.dominofever.model

data class TableState(
    val placed: List<PlacedDomino> = emptyList(),
    val branches: List<BranchState> = emptyList(),
    val nextDominoId: Int = 1,
    val nextBranchId: Int = 1
) {
    val isEmpty: Boolean
        get() = placed.isEmpty()

    val openEndsSum: Int
        get() = branches.sumOf { it.openValue }
}
