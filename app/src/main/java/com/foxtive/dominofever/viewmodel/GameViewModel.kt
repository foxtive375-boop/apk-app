package com.foxtive.dominofever.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.foxtive.dominofever.ai.DominoAiEngine
import com.foxtive.dominofever.data.CareerRepository
import com.foxtive.dominofever.engine.DominoGameEngine
import com.foxtive.dominofever.model.AiDifficulty
import com.foxtive.dominofever.model.GameMode
import com.foxtive.dominofever.model.GameState
import com.foxtive.dominofever.model.GameStatus
import com.foxtive.dominofever.model.MatchConfig
import com.foxtive.dominofever.model.MoveOption
import com.foxtive.dominofever.model.SessionType
import com.foxtive.dominofever.model.StartRule
import com.foxtive.dominofever.tournament.CareerProgress
import com.foxtive.dominofever.tournament.TournamentCatalog
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val engine = DominoGameEngine()
    private val aiEngine = DominoAiEngine()
    private val careerRepository = CareerRepository(application)

    private val _uiState = MutableStateFlow(
        GameUiState(
            statusText = "Выберите режим и запустите матч",
            modeText = "Quick Play / Tournament",
            tournamentText = "Карьера: не начата"
        )
    )
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private var gameState: GameState? = null
    private var selectedDominoIndex: Int? = null
    private var selectedMoves: List<MoveOption> = emptyList()
    private var promptTokenCounter: Int = 0
    private var aiJob: Job? = null

    private var careerProgress: CareerProgress = careerRepository.load()
    private var lastQuickConfig: MatchConfig = MatchConfig(
        mode = GameMode.DRAW,
        startRule = StartRule.DOUBLE_FIRST,
        targetScore = 100,
        sessionType = SessionType.QUICK_PLAY,
        aiDifficulty = AiDifficulty.MEDIUM
    )
    private var processedMatchKey: String? = null

    init {
        publishState()
    }

    fun startQuickPlay(mode: GameMode, startRule: StartRule, difficulty: AiDifficulty) {
        aiJob?.cancel()
        selectedDominoIndex = null
        selectedMoves = emptyList()
        processedMatchKey = null

        lastQuickConfig = MatchConfig(
            mode = mode,
            startRule = startRule,
            targetScore = 100,
            sessionType = SessionType.QUICK_PLAY,
            aiDifficulty = difficulty
        )

        gameState = engine.startMatch(lastQuickConfig)
        publishState(transientMessage = "Quick Play: ${mode.title}")
        maybeRunAiTurns()
    }

    fun startTournament() {
        aiJob?.cancel()
        selectedDominoIndex = null
        selectedMoves = emptyList()
        processedMatchKey = null

        val stage = TournamentCatalog.currentStage(careerProgress)
        if (stage == null) {
            publishState(transientMessage = "Карьера завершена. Сбросьте прогресс для нового цикла.")
            return
        }

        val config = MatchConfig(
            mode = stage.mode,
            startRule = StartRule.DOUBLE_FIRST,
            targetScore = stage.targetScore,
            sessionType = SessionType.TOURNAMENT,
            aiDifficulty = stage.aiDifficulty,
            stageName = stage.city
        )

        gameState = engine.startMatch(config)
        publishState(transientMessage = "Турнир: ${stage.city}")
        maybeRunAiTurns()
    }

    fun resetCareer() {
        careerRepository.reset()
        careerProgress = CareerProgress()
        publishState(transientMessage = "Прогресс карьеры сброшен")
    }

    fun onDominoClicked(index: Int) {
        val state = gameState ?: return
        if (state.status != GameStatus.IN_PROGRESS || state.currentPlayerId != HUMAN_ID) return

        val moves = engine.availableMoves(state, HUMAN_ID).filter { it.dominoIndex == index }
        if (moves.isEmpty()) {
            publishState(transientMessage = "Эту кость нельзя поставить")
            return
        }

        selectedDominoIndex = index
        selectedMoves = moves

        if (moves.size == 1) {
            applyHumanMove(moves.first())
        } else {
            promptTokenCounter += 1
            publishState()
        }
    }

    fun placeSelectedDomino(branchId: Int) {
        val move = selectedMoves.firstOrNull { it.branchId == branchId } ?: return
        applyHumanMove(move)
    }

    fun onDrawOrPassClicked() {
        var state = gameState ?: return
        if (state.status != GameStatus.IN_PROGRESS || state.currentPlayerId != HUMAN_ID) return

        val currentMoves = engine.availableMoves(state, HUMAN_ID)
        if (currentMoves.isNotEmpty()) {
            publishState(transientMessage = "У вас есть доступный ход")
            return
        }

        selectedDominoIndex = null
        selectedMoves = emptyList()

        if (state.config.mode.drawWhenNoMove) {
            while (engine.availableMoves(state, HUMAN_ID).isEmpty() && state.boneyard.isNotEmpty()) {
                state = engine.drawOne(state, HUMAN_ID)
            }
            if (engine.availableMoves(state, HUMAN_ID).isEmpty()) {
                state = engine.passTurn(state, HUMAN_ID, "Игрок не может ходить и пропускает")
            } else {
                state = state.copy(message = "Вы добрали до возможного хода")
            }
        } else {
            state = engine.passTurn(state, HUMAN_ID, "Игрок пропускает ход")
        }

        gameState = state
        processTerminalStateIfNeeded()
        publishState()
        maybeRunAiTurns()
    }

    fun onNextClicked() {
        val state = gameState ?: return
        when (state.status) {
            GameStatus.IN_PROGRESS -> Unit
            GameStatus.ROUND_OVER -> {
                gameState = engine.startNextRound(state)
                selectedDominoIndex = null
                selectedMoves = emptyList()
                publishState(transientMessage = "Новый раунд")
                maybeRunAiTurns()
            }
            GameStatus.MATCH_OVER -> {
                if (state.config.sessionType == SessionType.TOURNAMENT) {
                    startTournament()
                } else {
                    startQuickPlay(
                        mode = lastQuickConfig.mode,
                        startRule = lastQuickConfig.startRule,
                        difficulty = lastQuickConfig.aiDifficulty
                    )
                }
            }
        }
    }

    fun consumeTransientMessage() {
        _uiState.value = _uiState.value.copy(transientMessage = null)
    }

    private fun applyHumanMove(move: MoveOption) {
        val state = gameState ?: return
        if (state.status != GameStatus.IN_PROGRESS || state.currentPlayerId != HUMAN_ID) return

        selectedDominoIndex = null
        selectedMoves = emptyList()

        gameState = engine.applyMove(state, move)
        processTerminalStateIfNeeded()
        publishState()
        maybeRunAiTurns()
    }

    private fun maybeRunAiTurns() {
        aiJob?.cancel()
        aiJob = viewModelScope.launch {
            delay(350)
            while (true) {
                val snapshot = gameState ?: break
                if (snapshot.status != GameStatus.IN_PROGRESS || snapshot.currentPlayerId != AI_ID) {
                    break
                }

                var state = snapshot
                var moves = engine.availableMoves(state, AI_ID)

                if (moves.isEmpty()) {
                    if (state.config.mode.drawWhenNoMove) {
                        while (moves.isEmpty() && state.boneyard.isNotEmpty()) {
                            state = engine.drawOne(state, AI_ID)
                            gameState = state
                            publishState()
                            delay(180)
                            moves = engine.availableMoves(state, AI_ID)
                        }
                    }

                    if (moves.isEmpty()) {
                        state = engine.passTurn(state, AI_ID, "Соперник пропускает ход")
                        gameState = state
                        processTerminalStateIfNeeded()
                        publishState()
                        delay(280)
                        continue
                    }
                }

                val choice = aiEngine.chooseMove(state, AI_ID, moves) ?: moves.first()
                state = engine.applyMove(state, choice)
                gameState = state
                processTerminalStateIfNeeded()
                publishState()
                delay(450)
            }
        }
    }

    private fun processTerminalStateIfNeeded() {
        val state = gameState ?: return
        if (state.status != GameStatus.MATCH_OVER || state.config.sessionType != SessionType.TOURNAMENT) {
            return
        }

        val key = listOf(
            state.roundNumber,
            state.players[0].score,
            state.players[1].score,
            state.winnerId,
            state.config.stageName
        ).joinToString("|")
        if (processedMatchKey == key) return
        processedMatchKey = key

        val stage = TournamentCatalog.currentStage(careerProgress) ?: return
        careerProgress = if (state.winnerId == HUMAN_ID) {
            val nextIndex = careerProgress.currentStageIndex + 1
            val finished = nextIndex >= TournamentCatalog.stages.size
            CareerProgress(
                currentStageIndex = if (finished) careerProgress.currentStageIndex else nextIndex,
                rating = careerProgress.rating + stage.ratingReward,
                finished = finished
            )
        } else {
            CareerProgress(
                currentStageIndex = careerProgress.currentStageIndex,
                rating = (careerProgress.rating - stage.ratingReward / 3).coerceAtLeast(0),
                finished = false
            )
        }
        careerRepository.save(careerProgress)
    }

    private fun publishState(transientMessage: String? = null) {
        val state = gameState
        if (state == null) {
            _uiState.value = _uiState.value.copy(
                statusText = "Выберите режим и запустите матч",
                scoreText = "",
                modeText = "Quick Play / Tournament",
                tournamentText = tournamentLabel(),
                transientMessage = transientMessage
            )
            return
        }

        val human = state.player(HUMAN_ID)
        val ai = state.player(AI_ID)
        val humanMoves = if (state.status == GameStatus.IN_PROGRESS) {
            engine.availableMoves(state, HUMAN_ID)
        } else {
            emptyList()
        }

        val playableIndexes = humanMoves.map { it.dominoIndex }.toSet()
        val highlightedBranches = selectedMoves.map { it.branchId }.toSet()

        _uiState.value = GameUiState(
            hand = human.hand,
            playableIndexes = playableIndexes,
            selectedDominoIndex = selectedDominoIndex,
            highlightedBranchIds = highlightedBranches,
            pendingBranchChoices = if (selectedMoves.size > 1) selectedMoves else emptyList(),
            branchPromptToken = promptTokenCounter,
            table = state.table,
            lastPlacedDominoId = state.lastPlacedDominoId,
            statusText = state.message,
            scoreText = "Счет: ${human.name} ${human.score} (${human.roundsWon})  |  ${ai.name} ${ai.score} (${ai.roundsWon})",
            modeText = "Режим: ${state.config.mode.title} • Раунд ${state.roundNumber} • Базар: ${state.boneyard.size}",
            tournamentText = tournamentLabel(state),
            canDrawOrPass = state.status == GameStatus.IN_PROGRESS && state.currentPlayerId == HUMAN_ID,
            canAdvance = state.status != GameStatus.IN_PROGRESS,
            transientMessage = transientMessage
        )
    }

    private fun tournamentLabel(state: GameState? = null): String {
        val activeState = state ?: gameState
        return if (activeState?.config?.sessionType == SessionType.TOURNAMENT) {
            val stageName = activeState.config.stageName ?: "Tournament"
            "Карьера: $stageName • Рейтинг ${careerProgress.rating}"
        } else {
            val next = TournamentCatalog.currentStage(careerProgress)
            if (next == null || careerProgress.finished) {
                "Карьера завершена • Рейтинг ${careerProgress.rating}"
            } else {
                "Карьера: следующий этап ${next.city} • Рейтинг ${careerProgress.rating}"
            }
        }
    }

    private companion object {
        const val HUMAN_ID = 0
        const val AI_ID = 1
    }
}
