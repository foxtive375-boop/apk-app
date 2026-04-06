package com.foxtive.dominofever.engine

import com.foxtive.dominofever.engine.scoring.ScorerFactory
import com.foxtive.dominofever.model.AiDifficulty
import com.foxtive.dominofever.model.BranchDirection
import com.foxtive.dominofever.model.BranchState
import com.foxtive.dominofever.model.Domino
import com.foxtive.dominofever.model.GameState
import com.foxtive.dominofever.model.GameStatus
import com.foxtive.dominofever.model.MatchConfig
import com.foxtive.dominofever.model.MoveOption
import com.foxtive.dominofever.model.PlacedDomino
import com.foxtive.dominofever.model.PlayerState
import com.foxtive.dominofever.model.RoundEndReason
import com.foxtive.dominofever.model.TableState
import kotlin.random.Random

class DominoGameEngine(
    private val random: Random = Random(System.currentTimeMillis()),
    private val deckFactory: DominoDeckFactory = DominoDeckFactory()
) {

    private data class OpeningSelection(val playerId: Int, val dominoIndex: Int)

    fun startMatch(config: MatchConfig): GameState {
        return createRound(
            config = config,
            previousPlayers = null,
            roundNumber = 1
        )
    }

    fun startNextRound(previous: GameState): GameState {
        return createRound(
            config = previous.config,
            previousPlayers = previous.players,
            roundNumber = previous.roundNumber + 1
        )
    }

    fun availableMoves(state: GameState, playerId: Int): List<MoveOption> {
        if (state.status != GameStatus.IN_PROGRESS) return emptyList()
        val player = state.player(playerId)

        if (state.table.isEmpty) {
            return player.hand.mapIndexed { index, domino ->
                MoveOption(
                    playerId = playerId,
                    dominoIndex = index,
                    domino = domino,
                    branchId = -1,
                    matchedValue = domino.left
                )
            }
        }

        val moves = mutableListOf<MoveOption>()
        player.hand.forEachIndexed { index, domino ->
            state.table.branches.forEach { branch ->
                if (domino.matches(branch.openValue)) {
                    moves += MoveOption(
                        playerId = playerId,
                        dominoIndex = index,
                        domino = domino,
                        branchId = branch.id,
                        matchedValue = branch.openValue
                    )
                }
            }
        }
        return moves
    }

    fun drawOne(state: GameState, playerId: Int): GameState {
        if (state.boneyard.isEmpty() || state.status != GameStatus.IN_PROGRESS) return state
        val tile = state.boneyard.first()
        val players = updatePlayer(state.players, playerId) { player ->
            player.copy(hand = player.hand + tile)
        }
        return state.copy(
            players = players,
            boneyard = state.boneyard.drop(1),
            message = "${state.player(playerId).name} добрал ${tile.left}|${tile.right}"
        )
    }

    fun applyMove(state: GameState, move: MoveOption): GameState {
        require(state.status == GameStatus.IN_PROGRESS) { "Round is not active" }
        val player = state.player(move.playerId)
        require(move.dominoIndex in player.hand.indices) { "Invalid domino index" }

        return if (state.table.isEmpty) {
            placeOpeningDomino(state, move.playerId, move.dominoIndex)
        } else {
            placeOnBranch(state, move)
        }
    }

    fun passTurn(state: GameState, playerId: Int, reason: String): GameState {
        if (state.status != GameStatus.IN_PROGRESS) return state

        val nextPassCount = state.consecutivePasses + 1
        if (nextPassCount >= state.players.size) {
            return finishBlockedRound(
                state.copy(
                    consecutivePasses = nextPassCount,
                    message = reason
                )
            )
        }

        return state.copy(
            currentPlayerId = state.otherPlayerId(playerId),
            consecutivePasses = nextPassCount,
            message = reason
        )
    }

    fun resolveRoundIfCurrentPlayerCannotMove(state: GameState): GameState {
        if (state.status != GameStatus.IN_PROGRESS) return state
        val playerId = state.currentPlayerId
        val moves = availableMoves(state, playerId)
        return if (moves.isEmpty() && !state.config.mode.drawWhenNoMove && state.boneyard.isEmpty()) {
            passTurn(state, playerId, "${state.player(playerId).name} пропускает ход")
        } else {
            state
        }
    }

    private fun createRound(
        config: MatchConfig,
        previousPlayers: List<PlayerState>?,
        roundNumber: Int
    ): GameState {
        val deck = deckFactory.createShuffled(random)

        val humanHand = deck.subList(0, 7)
        val aiHand = deck.subList(7, 14)
        val boneyard = deck.subList(14, deck.size)

        val previousById = previousPlayers?.associateBy { it.id }
        val players = listOf(
            PlayerState(
                id = 0,
                name = config.humanName,
                isHuman = true,
                difficulty = AiDifficulty.EASY,
                hand = humanHand,
                score = previousById?.get(0)?.score ?: 0,
                roundsWon = previousById?.get(0)?.roundsWon ?: 0
            ),
            PlayerState(
                id = 1,
                name = config.aiName,
                isHuman = false,
                difficulty = config.aiDifficulty,
                hand = aiHand,
                score = previousById?.get(1)?.score ?: 0,
                roundsWon = previousById?.get(1)?.roundsWon ?: 0
            )
        )

        val initialState = GameState(
            config = config,
            players = players,
            currentPlayerId = 0,
            boneyard = boneyard,
            table = TableState(),
            roundNumber = roundNumber,
            consecutivePasses = 0,
            status = GameStatus.IN_PROGRESS,
            message = "Раунд $roundNumber начат"
        )

        val opening = determineOpeningSelection(initialState, config.startRule)
        return placeOpeningDomino(initialState, opening.playerId, opening.dominoIndex)
    }

    private fun determineOpeningSelection(state: GameState, rule: com.foxtive.dominofever.model.StartRule): OpeningSelection {
        val candidates = state.players.flatMap { player ->
            player.hand.mapIndexed { index, domino ->
                Triple(player.id, index, domino)
            }
        }

        if (rule == com.foxtive.dominofever.model.StartRule.DOUBLE_FIRST) {
            val bestDouble = candidates
                .filter { it.third.isDouble }
                .maxWithOrNull(compareBy<Triple<Int, Int, Domino>> { it.third.left }.thenBy { -it.first })
            if (bestDouble != null) {
                return OpeningSelection(bestDouble.first, bestDouble.second)
            }
        }

        val minTile = candidates.minWithOrNull(
            compareBy<Triple<Int, Int, Domino>> { it.third.pipSum }
                .thenBy { maxOf(it.third.left, it.third.right) }
                .thenBy { minOf(it.third.left, it.third.right) }
                .thenBy { it.first }
        ) ?: candidates.first()

        return OpeningSelection(minTile.first, minTile.second)
    }

    private fun placeOpeningDomino(state: GameState, playerId: Int, dominoIndex: Int): GameState {
        val player = state.player(playerId)
        val domino = player.hand[dominoIndex]
        val playerAfterMove = player.copy(hand = player.hand.removeAt(dominoIndex))
        val basePlayers = replacePlayer(state.players, playerAfterMove)

        val placedDominoId = state.table.nextDominoId
        val placed = PlacedDomino(
            id = placedDominoId,
            domino = domino,
            ownerId = playerId,
            branchId = null,
            matchedValue = null
        )

        val openingBranches = createOpeningBranches(
            domino = domino,
            mode = state.config.mode,
            parentDominoId = placedDominoId,
            startingBranchId = state.table.nextBranchId
        )

        val table = TableState(
            placed = listOf(placed),
            branches = openingBranches,
            nextDominoId = placedDominoId + 1,
            nextBranchId = openingBranches.maxOfOrNull { it.id }?.plus(1) ?: state.table.nextBranchId
        )

        val scorer = ScorerFactory.forKind(state.config.mode.scoringKind)
        val movePoints = scorer.movePoints(table.openEndsSum)
        val finalPlayers = updatePlayer(basePlayers, playerId) { p ->
            p.copy(score = p.score + movePoints)
        }

        return state.copy(
            players = finalPlayers,
            currentPlayerId = state.otherPlayerId(playerId),
            table = table,
            consecutivePasses = 0,
            message = buildString {
                append("${player.name} начинает раунд: ${domino.left}|${domino.right}")
                if (movePoints > 0) append(" (+$movePoints)")
            },
            lastPlacedDominoId = placedDominoId
        )
    }

    private fun createOpeningBranches(
        domino: Domino,
        mode: com.foxtive.dominofever.model.GameMode,
        parentDominoId: Int,
        startingBranchId: Int
    ): List<BranchState> {
        val directions = if (domino.isDouble && mode.branchingEnabled) {
            listOf(
                BranchDirection.LEFT,
                BranchDirection.RIGHT,
                BranchDirection.UP,
                BranchDirection.DOWN
            )
        } else {
            listOf(BranchDirection.LEFT, BranchDirection.RIGHT)
        }

        return directions.mapIndexed { index, direction ->
            val openValue = when {
                domino.isDouble -> domino.left
                direction == BranchDirection.LEFT -> domino.left
                else -> domino.right
            }
            BranchState(
                id = startingBranchId + index,
                parentDominoId = parentDominoId,
                direction = direction,
                openValue = openValue,
                chain = emptyList()
            )
        }
    }

    private fun placeOnBranch(state: GameState, move: MoveOption): GameState {
        val player = state.player(move.playerId)
        val domino = player.hand[move.dominoIndex]
        val branch = state.table.branches.firstOrNull { it.id == move.branchId }
            ?: return state

        if (!domino.matches(branch.openValue)) return state

        val dominoId = state.table.nextDominoId
        val matchedValue = branch.openValue
        val newOpenValue = domino.other(matchedValue)

        val placedDomino = PlacedDomino(
            id = dominoId,
            domino = domino,
            ownerId = move.playerId,
            branchId = branch.id,
            matchedValue = matchedValue
        )

        val updatedBranch = branch.copy(
            openValue = newOpenValue,
            chain = branch.chain + dominoId
        )

        var newBranches = state.table.branches.map { existing ->
            if (existing.id == branch.id) updatedBranch else existing
        }
        var nextBranchId = state.table.nextBranchId

        if (state.config.mode.branchingEnabled && domino.isDouble) {
            val extraDirection = chooseExtraDirection(
                existingBranches = newBranches,
                parentDominoId = dominoId,
                sourceDirection = branch.direction
            )
            newBranches = newBranches + BranchState(
                id = nextBranchId,
                parentDominoId = dominoId,
                direction = extraDirection,
                openValue = domino.left,
                chain = emptyList()
            )
            nextBranchId += 1
        }

        val table = state.table.copy(
            placed = state.table.placed + placedDomino,
            branches = newBranches,
            nextDominoId = dominoId + 1,
            nextBranchId = nextBranchId
        )

        val scorer = ScorerFactory.forKind(state.config.mode.scoringKind)
        val movePoints = scorer.movePoints(table.openEndsSum)

        val playersAfterRemoval = updatePlayer(state.players, move.playerId) { current ->
            current.copy(
                hand = current.hand.removeAt(move.dominoIndex),
                score = current.score + movePoints
            )
        }

        val activePlayer = playersAfterRemoval.first { it.id == move.playerId }
        val postMoveState = state.copy(
            players = playersAfterRemoval,
            table = table,
            consecutivePasses = 0,
            message = buildString {
                append("${player.name} выкладывает ${domino.left}|${domino.right} на ветку #${branch.id}")
                if (movePoints > 0) append(" (+$movePoints)")
            },
            lastPlacedDominoId = dominoId
        )

        if (activePlayer.hand.isEmpty()) {
            return finishRound(
                state = postMoveState,
                winnerId = move.playerId,
                reason = RoundEndReason.PLAYER_EMPTY_HAND
            )
        }

        return postMoveState.copy(currentPlayerId = state.otherPlayerId(move.playerId))
    }

    private fun chooseExtraDirection(
        existingBranches: List<BranchState>,
        parentDominoId: Int,
        sourceDirection: BranchDirection
    ): BranchDirection {
        val used = existingBranches
            .filter { it.parentDominoId == parentDominoId }
            .map { it.direction }
            .toSet()

        val candidates = listOf(
            sourceDirection.turnLeft(),
            sourceDirection.turnRight(),
            sourceDirection.opposite(),
            sourceDirection
        )

        return candidates.firstOrNull { it !in used } ?: sourceDirection.turnLeft()
    }

    private fun finishBlockedRound(state: GameState): GameState {
        val human = state.player(0)
        val ai = state.player(1)

        val winnerId = when {
            human.handPips < ai.handPips -> human.id
            ai.handPips < human.handPips -> ai.id
            else -> null
        }

        if (winnerId == null) {
            return state.copy(
                status = GameStatus.ROUND_OVER,
                message = "Блокировка: ничья по очкам в руке (${human.handPips})",
                winnerId = null,
                roundEndReason = RoundEndReason.BLOCKED
            )
        }

        return finishRound(
            state = state,
            winnerId = winnerId,
            reason = RoundEndReason.BLOCKED
        )
    }

    private fun finishRound(state: GameState, winnerId: Int, reason: RoundEndReason): GameState {
        val loserId = state.otherPlayerId(winnerId)
        val winnerBefore = state.player(winnerId)
        val loser = state.player(loserId)
        val scorer = ScorerFactory.forKind(state.config.mode.scoringKind)

        val bonusPoints = when (reason) {
            RoundEndReason.PLAYER_EMPTY_HAND -> scorer.finishPointsWhenDominoOut(loser.handPips)
            RoundEndReason.BLOCKED -> scorer.finishPointsWhenBlocked(winnerBefore.handPips, loser.handPips)
        }

        val playersAfterBonus = updatePlayer(state.players, winnerId) { current ->
            current.copy(
                score = current.score + bonusPoints,
                roundsWon = current.roundsWon + 1
            )
        }

        val winnerAfter = playersAfterBonus.first { it.id == winnerId }
        val matchFinished = winnerAfter.score >= state.config.targetScore

        return state.copy(
            players = playersAfterBonus,
            status = if (matchFinished) GameStatus.MATCH_OVER else GameStatus.ROUND_OVER,
            winnerId = winnerId,
            roundEndReason = reason,
            message = buildString {
                append("${winnerBefore.name} выигрывает ")
                append(if (reason == RoundEndReason.BLOCKED) "блокировку" else "раунд")
                append(". Бонус: $bonusPoints")
                if (matchFinished) {
                    append(". Матч завершён")
                }
            }
        )
    }

    private fun replacePlayer(players: List<PlayerState>, updated: PlayerState): List<PlayerState> {
        return players.map { current ->
            if (current.id == updated.id) updated else current
        }
    }

    private fun updatePlayer(
        players: List<PlayerState>,
        playerId: Int,
        updater: (PlayerState) -> PlayerState
    ): List<PlayerState> {
        return players.map { player ->
            if (player.id == playerId) updater(player) else player
        }
    }

    private fun <T> List<T>.removeAt(index: Int): List<T> = filterIndexed { i, _ -> i != index }
}
