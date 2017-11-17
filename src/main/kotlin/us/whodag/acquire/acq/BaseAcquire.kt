package us.whodag.acquire.acq

import mu.KLogging
import us.whodag.acquire.*
import us.whodag.acquire.obj.player.PlayerId
import us.whodag.acquire.print.AcqPrinter
import us.whodag.acquire.req.*
import us.whodag.acquire.sm.*

/**
 * Base implementation of an Acquire game.
 */
class BaseAcquire private constructor(private val id: AcquireId,
                                      private val version: AcqVersion,
                                      private val type: AcqType,
                                      private val players: Collection<PlayerId>,
                                      private val history: MutableList<State>,
                                      private var undoState: UndoState,
                                      private val acqPrinter: AcqPrinter) : Acquire {
    constructor(id: AcquireId,
                version: AcqVersion,
                type: AcqType,
                players: Collection<PlayerId>,
                acqSmState: AcqSmState) : this(id,
                                               version,
                                               type,
                                               players,
                                               mutableListOf(State(0, StartGameReq(players.iterator().next()), Responses.SUCCESS("Game started!"), acqSmState)),
            UndoStates.reset(),
                                               AcqPrinter(acqSmState))

    companion object: KLogging()

    override fun id(): AcquireId = id
    override fun version(): AcqVersion = version
    override fun type(): AcqType = type
    override fun players(): Collection<PlayerId> = players

    override fun turn(): Int = history.size - 1
    override fun state(turn: Int): State {
        require(turn >= 0, { "Turn cannot be earlier than the start of the game" })
        require(turn < history.size, { "Turn cannot be later than the current turn of the game" })
        return history[turn]
    }
    override fun submit(req: AcqReq): Response {
        logger.info { "[${req.player}] submitted request: $req" }
        val sm = state().acqSmState
        val response = when(req) {
            is AcceptMoneyReq -> when (sm) {
                is EndGamePayout -> sm.payoutAssets(req.player).save(req)
                is PayBonuses -> sm.payBonus(req.player).save(req)
                else -> Responses.FAILURE("AcceptMoney request not accepted in state $sm")
            }
            is AcceptStockReq -> when (sm) {
                is FoundersStock -> sm.receiveStock(req.player).save(req)
                else -> Responses.FAILURE("AcceptStock request not accepted in state $sm")
            }
            is BuyStockReq -> when (sm) {
                is BuyStock -> sm.buyStock(req.player, req.buyStocks).save(req)
                else -> Responses.FAILURE("BuyStock request not accepted in state $sm")
            }
            is ChooseHotelReq -> when (sm) {
                is StartHotel -> sm.startHotel(req.player, req.hotel).save(req)
                is ChooseSurvivingHotel -> sm.chooseSurvivingHotel(req.player, req.hotel).save(req)
                is ChooseDefunctHotel -> sm.chooseHotelToDefunct(req.player, req.hotel).save(req)
                else -> Responses.FAILURE("ChooseHotel request not accepted in state $sm")
            }
            is DrawTileReq -> when (sm) {
                is DrawTurnTile -> sm.drawTurnTile(req.player).save(req)
                is DrawInitialTiles -> sm.drawInitialTiles(req.player).save(req)
                is DrawTile -> sm.drawTile(req.player).save(req)
                else -> Responses.FAILURE("DrawTile request not accepted in state $sm")
            }
            is EndGameReq -> when (sm) {
                is PlaceTile -> sm.endGame(req.player).save(req)
                is DrawTile -> sm.endGame(req.player).save(req)
                // is GameOver -> sm.results(req.player).save(req) // 11/11/17: Request seems unnecessary
                else -> Responses.FAILURE("EndGame request not accepted in state $sm")
            }
            is HandleStocksReq -> when (sm) {
                is HandleDefunctHotelStocks -> sm.handleDefunctHotelStocks(req.player, req.trade, req.sell, req.keep).save(req)
                else -> Responses.FAILURE("HandleStocks request not accepted in state $sm")
            }
            is PlaceTileReq -> when (sm) {
                is PlaceTurnTile -> sm.placeTurnTile(req.player, req.tile).save(req)
                is PlaceTile -> sm.placeTile(req.player, req.tile).save(req)
                else -> Responses.FAILURE("PlaceTile request not accepted in state $sm")
            }
            is StartGameReq -> {
                Responses.FAILURE("StartGame request not accepted once game is started")
            }
            is AcceptUndoReq -> {
                val (nextUndoState, response) = undoState.acceptUndoRequest(req.player)
                if (response.isSuccess() && nextUndoState.acceptedUndo.size == players.size) {
                    history.removeAt(history.size - 1)
                    undoState = UndoStates.reset()
                    Responses.SUCCESS("Undo request accepted for ${nextUndoState.requestedUndo!!}. Their last request was removed")
                } else {
                    undoState = nextUndoState
                    response
                }}
            is UndoReq -> {
                val (nextUndoState, response) = undoState.undoRequest(history, req.player)
                undoState = nextUndoState
                response
            }
        }
        if (response.isSuccess()) logger.info { "[${req.player}] request successful: ${response.message}" }
        else logger.warn { "[${req.player}] request failed: ${response.message}" }
        return response
    }

    /* Save the transition into the history returning the response. */
    private fun Transition<AcqSmState>.save(req: AcqReq): Response {
        // only save successes
        if (response.isSuccess()) {
            undoState = UndoStates.reset()
            history.add(State(turn() + 1, req, response, smState))
            acqPrinter.print(smState)
        }
        return response
    }
}

/*
 * State for undo requests.
 *
 * @property requestedUndo when an undo was requested and who requested it.
 * @property acceptedUndo list of players who have accepted the undo request.
 */
private data class UndoState(val requestedUndo: PlayerId?,
                             val acceptedUndo: List<PlayerId>)

/* Factory for creating undo states. */
private object UndoStates {

    /* Create a new undo state instance. */
    fun reset(): UndoState = UndoState(null, emptyList())
}

/* Process an accept undo request submitted by player. */
private fun UndoState.acceptUndoRequest(player: PlayerId): Pair<UndoState, Response> {
    if (requestedUndo == null) {
        return Pair(this, Responses.FAILURE("AcceptUndo request not accepted when no undo requested"))
    }
    return Pair(copy(acceptedUndo = acceptedUndo + player), Responses.SUCCESS("$player accepted the undo request"))
}

/* Process an undo request for history submitted by player. */
private fun UndoState.undoRequest(history: List<State>,
                                                        player: PlayerId): Pair<UndoState, Response> {
    if (requestedUndo != null) {
        return Pair(this, Responses.FAILURE("Undo request not accepted when an undo is already requested"))
    }
    val lastRequest = history[history.size - 1].acqReq
    if (lastRequest is StartGameReq) {
        return Pair(this, Responses.FAILURE("Undo request not accepted for a start game request"))
    }
    if (lastRequest.player != player) {
        return Pair(this, Responses.FAILURE("Undo request denied. Cannot undo a different players request"))
    }
    return Pair(UndoState(player, acceptedUndo + player),
           Responses.SUCCESS("$player requested to undo their last request: $lastRequest"))
}
