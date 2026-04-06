package com.foxtive.dominofever.model

data class MatchConfig(
    val mode: GameMode,
    val startRule: StartRule,
    val targetScore: Int,
    val sessionType: SessionType,
    val aiDifficulty: AiDifficulty,
    val humanName: String = "Игрок",
    val aiName: String = "Соперник",
    val stageName: String? = null
)
