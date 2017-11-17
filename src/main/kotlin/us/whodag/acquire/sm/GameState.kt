package us.whodag.acquire.sm

import us.whodag.acquire.obj.player.PlayerId

/**
 * Acquire state machine state which is tracked most of the game.
 *
 * @property playerTurnOrder the players in game in order of their turns.
 * @property currentPlayer the player whose current turn it is.
 */
data class GameState(val playerTurnOrder: List<PlayerId>,
                     val currentPlayer: PlayerId)

/** Whether the specified player is the current player. */
fun GameState.isCurrentPlayer(player: PlayerId) = player == currentPlayer

/** Whether the specified player is not the current player. */
fun GameState.isNotCurrentPlayer(player: PlayerId) = player != currentPlayer

/** The player whose turn is next. */
fun GameState.nextCurrentPlayer(): PlayerId {
    require(!playerTurnOrder.isEmpty(), { "Player turn order needs to be defined" })
    val i = playerTurnOrder.indexOf(currentPlayer)
    return if (i + 1 >= playerTurnOrder.size) playerTurnOrder.first()
    else playerTurnOrder[i + 1]
}
