package us.whodag.acquire.sm

import mu.KLogging
import us.whodag.acquire.req.Response
import us.whodag.acquire.req.Responses
import us.whodag.acquire.obj.*
import us.whodag.acquire.obj.hotel.HotelId
import us.whodag.acquire.obj.hotel.HotelState
import us.whodag.acquire.obj.player.PlayerId
import us.whodag.acquire.obj.tile.TileId
import us.whodag.acquire.obj.tile.TileState
import us.whodag.acquire.req.isFailure

/**
 * State machine for an Acquire game.
 */
object AcqSm {

    /** Entry into the Acquire State Machine. */
    fun start(acqObj: AcqObj): AcqSmState = DrawTurnTile(acqObj, emptyList())
}

/** State of the state machine for an Acquire game. */
sealed class AcqSmState : AcqObjSmState() {
    companion object: KLogging()

    @Override
    override fun toString(): String {
        return this.javaClass.simpleName
    }
}

/** State machine state which uses an AcqObj. */
abstract class AcqObjSmState {
    abstract val acqObj: AcqObj
}

/** A transition to a state machine state based on the response to a request. */
data class Transition<out T>(val response: Response, val smState: T)

/*
 * PreGame
 * =======
 */

/**
 * Each player draws a tile which will determine the player turn order.
 *
 * @property playersDrawn players who have drawn their turn tile.
 */
class DrawTurnTile(override val acqObj: AcqObj,
                   val playersDrawn: Collection<PlayerId>) : AcqSmState() {

    /**
     * Player draws their turn tile.
     *
     * Transitions to:
     *  - DrawTurnTile
     *  - PlaceTurnTile
     */
    fun drawTurnTile(player: PlayerId): Transition<AcqSmState> {
        if (playersDrawn.contains(player)) {
            logger.warn { "[$player] has already drawn their turn tile. Players who have drawn: $playersDrawn" }
            return Transition(Responses.FAILURE("$player has already drawn their turn tile"), this)
        }

        // draw a tile from the bank and add it to the players hand
        val (nextAcqObj, drawnTile) = acqObj.drawTile(player)
        val nextPlayersDrawn = playersDrawn + player
        logger.info { "[$player] has has drawn their turn tile: $drawnTile" }

        // when all players have drawn their turn tile, transition to the next state
        val nextState = if (nextPlayersDrawn.size == acqObj.players.size) PlaceTurnTile(nextAcqObj, emptyMap())
                        else                                              DrawTurnTile(nextAcqObj, nextPlayersDrawn)
        return Transition(Responses.SUCCESS("$player drew a turn tile"), nextState)
    }
}

/**
 * Each player places the turn tile they drew and the player turn order is determined.
 *
 * @property playersPlaced players who have placed their turn tile and the tile they placed.
 */
class PlaceTurnTile(override val acqObj: AcqObj,
                    val playersPlaced: Map<PlayerId, TileId>) : AcqSmState() {

    /**
     * Player places their turn tile on the board.
     *
     * Transitions to:
     *  - PlaceTurnTile
     *  - DrawInitialTiles
     */
    fun placeTurnTile(player: PlayerId, tile: TileId): Transition<AcqSmState> {
        if (playersPlaced.contains(player)) {
            logger.warn { "[$player] has already placed their turn tile. Players who have placed: $playersPlaced" }
            return Transition(Responses.FAILURE("$player have already placed their turn tile"), this)
        }
        if (!acqObj.player(player).hasTile(tile)) {
            logger.warn { "[$player] does not have turn tile [$tile] in their hand" }
            return Transition(Responses.FAILURE("$player does not have turn tile $tile in their hand"), this)
        }

        // place the tile from the players hand onto the board
        val nextAcqObj = acqObj.placeTile(player, tile)
        val nextPlayersPlaced = playersPlaced + Pair(player, tile)
        logger.info { "[$player] placed their turn tile [$tile]" }

        // if all players have placed their tiles, populate the player turn order and go to the next state
        return if (nextPlayersPlaced.size == acqObj.players.size) {
                   val playersDrawnSorted = nextPlayersPlaced.keys.toList().sortedBy { p -> nextPlayersPlaced[p]!! }
                   logger.info { "[$player] turn order set to: $playersDrawnSorted" }
                   Transition(Responses.SUCCESS("$player placed turn tile $tile. Turn order set to $playersDrawnSorted"),
                              DrawInitialTiles(nextAcqObj, playersDrawnSorted, emptyList()))
               } else {
                   Transition(Responses.SUCCESS("$player placed turn tile $tile"),
                              PlaceTurnTile(nextAcqObj, nextPlayersPlaced))
               }
    }
}

/**
 * Each player draws their initial tiles, forming their hand.
 *
 * @property playerTurnOrder the player turn order.
 * @property playersDrawn players who have drawn their initial tiles.
 */
class DrawInitialTiles(override val acqObj: AcqObj,
                       val playerTurnOrder: List<PlayerId>,
                       val playersDrawn: Collection<PlayerId>) : AcqSmState() {

    /**
     * Player draws their initial tiles.
     *
     * Transitions to:
     *  - DrawInitialTiles
     *  - PlaceTile
     */
    fun drawInitialTiles(player: PlayerId): Transition<AcqSmState> {
        if (playersDrawn.contains(player)) {
            logger.warn { "[$player] has already drawn their initial tiles" }
            return Transition(Responses.FAILURE("$player has already drawn their initial tiles"), this)
        }

        // draw tiles until the players hand is full
        val nextAcqObj = (1..acqObj.player(player).handLimit())
                         .fold(acqObj) { acqObj, _ -> acqObj.drawTile(player).first }
        val nextPlayersDrawn = playersDrawn + player
        logger.info { "[$player] drew their initial tiles" }

        // when all players have drawn their initial tiles, go to the next state
        val nextState = if (nextPlayersDrawn.size == acqObj.players.size) PlaceTile(nextAcqObj, GameState(playerTurnOrder, playerTurnOrder.first()))
                        else                                              DrawInitialTiles(nextAcqObj, playerTurnOrder, nextPlayersDrawn)
        return Transition(Responses.SUCCESS("$player drew initial tiles"), nextState)
    }
}

/*
 * Game
 * ====
 */

/**
 * The current player places a tile onto the board, starting their turn.
 */
class PlaceTile(override val acqObj: AcqObj,
                val gameState: GameState) : AcqSmState() {

    /**
     * Player places tile onto the board.
     *
     * Transitions to:
     *  - PlaceTile
     *  - BuyStock
     *  - ChooseDefunctHotel
     *  - ChooseSurvivingHotel
     *  - DrawTile
     *  - PayBonuses
     *  - StartHotel
     *
     * @param unplayableRecursiveSearch flag denoting we're recursively searching for other unplayable tiles. Necessary to prevent infinite recursion.
     */
    fun placeTile(player: PlayerId, tile: TileId, unplayableRecursiveSearch: Boolean = false): Transition<AcqSmState> {
        if (gameState.isNotCurrentPlayer(player)) {
            logger.warn { "[$player] is not the current player [${gameState.currentPlayer}]" }
            return Transition(Responses.FAILURE("It is not $player's turn; it is ${gameState.currentPlayer}'s turn"), this)
        }
        if (!acqObj.player(player).hasTile(tile)) {
            logger.warn { "[$player] does not have tile [$tile] in their hand" }
            return Transition(Responses.FAILURE("$player does not have tile $tile in their hand"), this)
        }

        /*
         *  If there are no nearby tiles, just place the tile
         *
         *  If the nearby tiles are not part any hotel AND:
         *    hotels are not available to start, place a different tile
         *    hotels are available to start, start a new hotel
         *
         *  If there is one nearby hotel, placed tile gets added to that hotel
         *
         *  If there are two or more nearby hotels AND:
         *    if more than one of them are safe, discard the tile
         *    with one of a max size, merge hotels into the max sized hotel
         *    with more than one with a max size hotel, enter a merge choice
         */
        val nearbyTiles: List<TileId> = acqObj.board.adjacentAndConnectedTiles(tile)
        val nearbyHotels: List<HotelId> = nearbyTiles.mapNotNull { t -> acqObj.tile(t).hotel() }.distinct()

        // if there are no nearby tiles, just place the tile
        if (nearbyTiles.isEmpty()) {
            val nextAcqObj = acqObj.placeTile(player, tile)
            logger.info { "[$player] placed tile [$tile]" }
            return Transition(Responses.SUCCESS("$player placed tile $tile"),
                              buyStockIfAbleSMState(nextAcqObj, gameState, player))
        }

        // if the nearby tiles are not part any hotel AND:
        if (nearbyHotels.isEmpty()) {
            // hotels are not available to start
            if (acqObj.availableHotels().isEmpty()) {
                // if the rest of the player's tiles are unplayable, skip the draw tile state
                if (!unplayableRecursiveSearch && (acqObj.player(player).tiles() - tile).all { t -> placeTile(player, t, unplayableRecursiveSearch = true).response.isFailure() }) {
                    logger.info { "[$player] skipping place tile state as no tiles can be placed" }
                    return Transition(Responses.SUCCESS("$player skipping place tile as no tiles can be placed"),
                                      buyStockIfAbleSMState(acqObj, gameState, player))
                }

                // place a different tile
                logger.warn { "[$player] there are not hotels available to start for placing tile [$tile]" }
                return Transition(Responses.FAILURE("$player cannot place tile $tile as it would start a hotel"), this)
            }

            // hotels are available to start, start a new hotel
            val nextAcqObj = acqObj.placeTile(player, tile)
            val hotelTiles = nearbyTiles + tile
            logger.info { "[$player] placed tile [$tile] which will start a hotel made up of tiles: $hotelTiles" }
            return Transition(Responses.SUCCESS("$player placed tile $tile which starts a hotel"),
                              StartHotel(nextAcqObj, gameState, hotelTiles))
        }

        // if there is one nearby hotel, placed tile gets added to that hotel
        if (nearbyHotels.size == 1) {
            val hotel = nearbyHotels[0]
            val nextAcqObj = acqObj.placeTile(player, tile, hotel)
            logger.info { "[$player] placed tile [$tile] adding to hotel [$hotel]" }
            return Transition(Responses.SUCCESS("$player placed tile $tile adding to $hotel"),
                              buyStockIfAbleSMState(nextAcqObj, gameState, player))
        }

        // if there are two or more nearby hotels AND:
        if (nearbyHotels.size > 1) {
            // if more than one of them are safe then discard tile
            if (nearbyHotels.filter { h -> acqObj.hotel(h).isSafe() }.count() > 1) {
                val nextAcqObj = acqObj.discardTile(player, tile)
                logger.info { "[$player] discarded tile [$tile]" }
                return Transition(Responses.SUCCESS("$player discarded tile $tile"),
                                  PlaceTile(nextAcqObj, gameState))
            }

            // there will be a hotel merger
            val nextAcqObj = acqObj.placeTile(player, tile)
            logger.info { "[$player] placed tile [$tile] will starts a merger involving hotels: $nearbyHotels" }
            return Transition(Responses.SUCCESS("$player placed tile $tile which starts a merger"),
                              startMerger(nextAcqObj, gameState, tile, nearbyHotels))
        }

        throw RuntimeException("Unhandled PlaceTile logic")
    }

    /**
     * Player ends the game.
     *
     * Transitions to:
     *  - PlaceTile
     *  - EndGamePayout
     */
    fun endGame(player: PlayerId): Transition<AcqSmState> {
        if (gameState.isNotCurrentPlayer(player)) {
            logger.warn { "[$player] is not the current player [${gameState.currentPlayer}]" }
            return Transition(Responses.FAILURE("It is not $player's turn; it is ${gameState.currentPlayer}'s turn"), this)
        }
        if (!acqObj.canEndGame()) {
            logger.warn { "[$player] cannot end the game" }
            return Transition(Responses.FAILURE("The game cannot be ended"), this)
        }

        logger.info { "[$player] ended the game" }
        return Transition(Responses.SUCCESS("$player ended the game"), EndGamePayout(acqObj, emptyList()))
    }
}

/**
 * The current player's placed tile starts a hotel.
 *
 * @property tiles the tiles that will make up the started hotel.
 */
class StartHotel(override val acqObj: AcqObj,
                 val gameState: GameState,
                 val tiles: Collection<TileId>) : AcqSmState() {

    /**
     * Player starting hotel with their placed tile.
     *
     * Transitions to:
     *  - StartHotel
     *  - BuyStock
     *  - DrawTile
     *  - FoundersStock
     */
    fun startHotel(player: PlayerId, hotel: HotelId): Transition<AcqSmState> {
        if (gameState.isNotCurrentPlayer(player)) {
            logger.warn { "[$player] is not the current player [${gameState.currentPlayer}]" }
            return Transition(Responses.FAILURE("It is not $player's turn; it is ${gameState.currentPlayer}'s turn"), this)
        }
        if (!acqObj.availableHotels().contains(hotel)) {
            logger.warn { "[$player] chose to start hotel [$hotel] but it is not available to start" }
            return Transition(Responses.FAILURE("$player choose hotel $hotel but it is not available to start"), this)
        }

        logger.info { "[$player] started hotel [$hotel]" }
        val nextAcqObj = acqObj.startHotel(hotel, tiles)
        val nextState = if (acqObj.bank.hasStock(hotel)) FoundersStock(nextAcqObj, gameState, hotel)
                        else                             buyStockIfAbleSMState(nextAcqObj, gameState, player)
        return Transition(Responses.SUCCESS("$player started hotel $hotel"), nextState)
    }
}

/**
 * The current player started a hotel and receives the founders bonus stock.
 *
 * @property startedHotel the hotel that was started.
 */
class FoundersStock(override val acqObj: AcqObj,
                    val gameState: GameState,
                    val startedHotel: HotelId) : AcqSmState() {

    /**
     * Player receives their founders stock.
     *
     * Transitions to:
     *  - FoundersStock
     *  - BuyStock
     *  - DrawTile
     */
    fun receiveStock(player: PlayerId): Transition<AcqSmState> {
        if (gameState.isNotCurrentPlayer(player)) {
            logger.warn { "[$player] is not the current player [${gameState.currentPlayer}]" }
            return Transition(Responses.FAILURE("It is not $player's turn; it is ${gameState.currentPlayer}'s turn"), this)
        }

        logger.info { "[$player] received 1 stock of hotel [$startedHotel] for starting it" }
        val nextAcqObj = acqObj.withdrawStock(player, startedHotel, 1)
        return Transition(Responses.SUCCESS("$player received a founding stock of $startedHotel"),
                          buyStockIfAbleSMState(nextAcqObj, gameState, player))
    }
}

/**
 * The current player may buy stock from any available hotels.
 */
class BuyStock(override val acqObj: AcqObj,
               val gameState: GameState) : AcqSmState() {

    /**
     * Player requests to buy stocks the hotels specified in the buy stocks map.
     *
     * Transitions to:
     *  - BuyStock
     *  - DrawTile
     */
    fun buyStock(player: PlayerId, buyStocks: Map<HotelId, Int>): Transition<AcqSmState> {
        if (gameState.isNotCurrentPlayer(player)) {
            logger.warn { "[$player] is not the current player [${gameState.currentPlayer}]" }
            return Transition(Responses.FAILURE("It is not $player's turn; it is ${gameState.currentPlayer}'s turn"), this)
        }

        /* To buy stock:
         *  hotels to buy cannot contain hotels that are not on the board
         *  player must be buying stock amounts of each hotel between 0-3
         *  hotel must have sufficient number of stocks in the bank
         *  player must be buying 3 or less stocks total
         *  player must have sufficient money to buy stocks
         */
        val stockTurnLimit = acqObj.player(player).stockTurnLimit()
        val playerMoney = acqObj.player(player).money()

        val hotelsToBuy = buyStocks.filter { e -> e.value != 0 }.map { e -> e.key }

        // hotels to buy cannot contain hotels that are not on the board
        if (hotelsToBuy.any { h -> !acqObj.hotel(h).isOnBoard() }) {
            val hotels = hotelsToBuy.filter { h -> !acqObj.hotel(h).isOnBoard() }
            logger.warn { "[$player] requested to buy stock of hotels not on the board: $hotels" }
            return Transition(Responses.FAILURE("$player requested to buy stock of hotels not on the board $hotels"), this)
        }

        val stocksToBuy = buyStocks.values.fold(0, { total, stocks -> total + stocks })
        val moneyToBuy = hotelsToBuy.fold(0, { total, hotel -> total + buyStocks[hotel]!! * acqObj.hotel(hotel).stockPrice() })

        // player must be buying stock amounts of each hotel between 0-3
        if (hotelsToBuy.any { h -> buyStocks[h]!! < 0 || buyStocks[h]!! > stockTurnLimit }) {
            val hotelStocks = hotelsToBuy.filter { h -> buyStocks[h]!! < 0 || buyStocks[h]!! > stockTurnLimit }
                                         .map { h -> Pair(h, buyStocks[h]!!) }
            logger.warn { "[$player] requested to buy an invalid number of stocks: $hotelStocks" }
            return Transition(Responses.FAILURE("$player requested to buy an invalid number of stocks $hotelStocks"), this)
        }

        // hotel must have sufficient number of stocks in the bank
        if (hotelsToBuy.any { h -> acqObj.bank.stock(h) < buyStocks[h]!! }) {
            val hotelStocks = hotelsToBuy.filter { h -> acqObj.bank.stock(h) < buyStocks[h]!! }
                                         .map { h -> Triple(h, acqObj.bank.stock(h), buyStocks[h]!!) }
            logger.warn { "[$player] requested to buy more stocks than available in the bank: $hotelStocks" }
            return Transition(Responses.FAILURE("$player requested to buy more stocks than available in the bank $hotelStocks"), this)
        }

        // player must be buying 3 or less stocks total
        if (stocksToBuy > stockTurnLimit) {
            logger.warn { "[$player] requested to buy more stocks [$stocksToBuy] than allowed [$stockTurnLimit]" }
            return Transition(Responses.FAILURE("$player requested to buy more stocks ($stocksToBuy) than their limit of $stockTurnLimit"), this)
        }

        // player must have sufficient money to buy stocks
        if (moneyToBuy > playerMoney) {
            logger.warn { "[$player] does not have enough money [$$playerMoney] to buy the requested stocks [$$moneyToBuy]" }
            return Transition(Responses.FAILURE("$player does not have enough money $$playerMoney to buy the requested stocks $$moneyToBuy"), this)
        }

        val nextAcqObj = acqObj.buyStocks(player, buyStocks)
        logger.info { "[$player] paid [$$moneyToBuy] and bought stocks $buyStocks" }
        val responseStr = if (moneyToBuy == 0) "$player bought no stocks"
                          else { val boughtStr = buyStocks.entries.filter { (_, s) -> s > 0 }.joinToString { (h, s) -> "$s $h" }
                                 "$player paid $$moneyToBuy and bought $boughtStr" }
        return Transition(Responses.SUCCESS(responseStr),
                          DrawTile(nextAcqObj, gameState))
    }
}

/**
 * The current player draws tiles until their hand is full (if able).
 */
class DrawTile(override val acqObj: AcqObj,
               val gameState: GameState) : AcqSmState() {

    /**
     * Player draws tiles until their hand is full.
     *
     * Transitions to:
     *  - DrawTile
     *  - PlaceTile
     */
    fun drawTile(player: PlayerId): Transition<AcqSmState> {
        if (gameState.isNotCurrentPlayer(player)) {
            logger.warn { "[$player] is not the current player [${gameState.currentPlayer}]" }
            return Transition(Responses.FAILURE("It is not $player's turn; it is ${gameState.currentPlayer}'s turn"), this)
        }

        if (!acqObj.bank.hasTileToDraw()) {
            logger.info { "[$player] did not draw a tile: there are no tiles left to draw" }
            val nextGameState = gameState.copy(currentPlayer = gameState.nextCurrentPlayer())
            return Transition(Responses.SUCCESS("$player did not draw a tile as there are no tiles left to draw"),
                              PlaceTile(acqObj, nextGameState))
        }

        val (nextAcqObj, tile) = acqObj.drawTile(player)
        logger.info { "[$player] drew tile [$tile]" }
        val nextState = if (nextAcqObj.player(player).tiles().size < nextAcqObj.player(player).handLimit()) DrawTile(nextAcqObj, gameState)
                        else                                                                                PlaceTile(nextAcqObj, gameState.copy(currentPlayer = gameState.nextCurrentPlayer()))
        return Transition(Responses.SUCCESS("$player drew a tile"), nextState)
    }

    /**
     * Player ends the game.
     *
     * Transitions to:
     *  - DrawTile
     *  - EndGamePayout
     */
    fun endGame(player: PlayerId): Transition<AcqSmState> {
        if (gameState.isNotCurrentPlayer(player)) {
            logger.warn { "[$player] is not the current player [${gameState.currentPlayer}]" }
            return Transition(Responses.FAILURE("It is not $player's turn; it is ${gameState.currentPlayer}'s turn"), this)
        }
        if (!acqObj.canEndGame()) {
            logger.warn { "[$player] cannot end the game" }
            return Transition(Responses.FAILURE("The game cannot be ended"), this)
        }

        logger.info { "[$player] ended the game" }
        return Transition(Responses.SUCCESS("$player ended the game"), EndGamePayout(acqObj, emptyList()))
    }
}

/**
 * The payout of assets once the game has been declared over.
 *
 * @property playersPaid players who have been paid their assets.
 */
class EndGamePayout(override val acqObj: AcqObj,
                    val playersPaid: Collection<PlayerId>) : AcqSmState() {

    /**
     * Player receives a payout of their assets.
     *
     * Transitions to:
     *  - EndGamePayout
     *  - GameOver
     */
    fun payoutAssets(player: PlayerId): Transition<AcqSmState> {
        if (playersPaid.contains(player)) {
            logger.warn { "[$player] has already received their assets payout. Players been paid: $playersPaid" }
            return Transition(Responses.FAILURE("$player has already received their assets payout"), this)
        }

        val assetWorth = acqObj.assetWorth(player)
        val nextAcqObj = acqObj.addMoney(player, assetWorth)
        val nextPlayersPaid = playersPaid + player
        logger.info { "[$player] paid $$assetWorth for their assets" }

        val nextState = if (nextPlayersPaid.size == acqObj.players.size) GameOver(nextAcqObj, nextAcqObj.players.values.sortedBy { p -> p.money() }.map { p -> p.id() }.reversed())
                        else                                             EndGamePayout(nextAcqObj, nextPlayersPaid)
        return Transition(Responses.SUCCESS("$player paid $$assetWorth for their assets"), nextState)
    }
}

/**
 * The final state of the game. The game is over.
 *
 * @property playerResults the player results determined by total money.
 */
class GameOver(override val acqObj: AcqObj,
               val playerResults: List<PlayerId>) : AcqSmState() {

    /**
     * Player queries for the results of the game.
     *
     * Transitions to:
     *  - GameOver
     */
    fun results(player: PlayerId): Transition<GameOver> {
        val result = playerResults.indexOf(player) + 1
        logger.info { "[$player] finished [$result]. The results were: $playerResults"}
        return when (result) {
                   1 -> Transition(Responses.SUCCESS("$player won!"), this)
                   else -> Transition(Responses.SUCCESS("$player lost, finishing #$result"), this)
               }
    }
}

/**
 * Transition to BuyStock or bypass it depending on whether stocks may be bought.
 *
 * Transitions to:
 *  - BuyStock
 *  - DrawTile
 *
 * @param player the player to transition on.
 */
fun buyStockIfAbleSMState(acqObj: AcqObj,
                          gameState: GameState,
                          player: PlayerId): AcqSmState =
        if (acqObj.canBuyStock(player)) BuyStock(acqObj, gameState)
        else                            DrawTile(acqObj, gameState)

/*
 * Merger
 * ======
 */

/**
 * Entry into a merger.
 *
 * Transitions to:
 *  - ChooseSurvivingHotel
 *  - ChooseDefunctHotel
 *  - PayBonuses
 *
 * @param placedTile the tile that was placed starting the merger.
 * @param nearbyHotels hotels that make up the nearby tiles.
 */
fun startMerger(acqObj: AcqObj,
                gameState: GameState,
                placedTile: TileId,
                nearbyHotels: List<HotelId>): AcqSmState {
    val mergeContext = MergeContext(gameState, placedTile, nearbyHotels)

    // need to choose the surviving hotel?
    val maxSizeSurvivingHotels = acqObj.largestSizeHotels(nearbyHotels)
    if (maxSizeSurvivingHotels.size > 1) {
        return ChooseSurvivingHotel(acqObj, mergeContext, maxSizeSurvivingHotels)
    }
    val survivingHotel = maxSizeSurvivingHotels[0]
    val defunctHotels = (nearbyHotels - survivingHotel)
    return nextDefunctHotelSmState(acqObj, mergeContext, survivingHotel, defunctHotels)
}

/**
 * Transition to the next state machine state depending on the next defunct hotel, if any exist.
 *
 * Transitions to:
 *  - ChooseDefunctHotel
 *  - PayBonuses
 *
 * @param survivingHotel the hotel surviving the merger.
 * @param defunctHotels list of hotels to be defunct.
 */
fun nextDefunctHotelSmState(acqObj: AcqObj,
                            mergeContext: MergeContext,
                            survivingHotel: HotelId,
                            defunctHotels: List<HotelId>): AcqSmState {
    if (defunctHotels.isEmpty()) {
        return endMerger(acqObj, mergeContext, survivingHotel)
    }

    // need to choose the defunct hotel?
    val maxSizeDefunctHotels = acqObj.largestSizeHotels(defunctHotels)
    if (maxSizeDefunctHotels.size > 1) {
        return ChooseDefunctHotel(acqObj, mergeContext, survivingHotel, defunctHotels, maxSizeDefunctHotels)
    }
    val defunctHotel = maxSizeDefunctHotels[0]
    val remainingHotels = (defunctHotels - defunctHotel)

    val mergeState = MergeState(mergeContext, survivingHotel, defunctHotel, remainingHotels)
    val playersToPay = acqObj.stockBonuses(defunctHotel)
    return PayBonuses(acqObj, mergeState, playersToPay)
}

/**
 * Merging player choose a hotel to survive the merger.
 *
 * @property potentialSurvivingHotels hotel candidates for surviving the merger.
 */
class ChooseSurvivingHotel(override val acqObj: AcqObj,
                           val mergeContext: MergeContext,
                           val potentialSurvivingHotels: Collection<HotelId>) : AcqSmState() {

    /**
     * Place chooses a hotel to survive the merger.
     *
     * Transitions to:
     *  - ChooseSurvivingHotel
     *  - ChooseDefunctHotel
     *  - PayBonuses
     */
    fun chooseSurvivingHotel(player: PlayerId, hotel: HotelId): Transition<AcqSmState> {
        if (mergeContext.gameState.isNotCurrentPlayer(player)) {
            logger.warn { "[$player] is not the current player [${mergeContext.gameState.currentPlayer}]" }
            return Transition(Responses.FAILURE("It is not $player's turn; it is ${mergeContext.gameState.currentPlayer}'s turn"), this)
        }
        if (!potentialSurvivingHotels.contains(hotel)) {
            logger.warn { "[$player] chosen surviving hotel [$hotel] is not a candidate. The candidates are: $potentialSurvivingHotels" }
            return Transition(Responses.FAILURE("$player requested a hotel $hotel that is not a surviving candidate. The candidates are $potentialSurvivingHotels"), this)
        }

        logger.info { "[$player] choose hotel [$hotel] to survive the merger" }
        return Transition(Responses.SUCCESS("$player choose $hotel to survive the merger"),
                          nextDefunctHotelSmState(acqObj,
                                                  mergeContext,
                                                  survivingHotel = hotel,
                                                  defunctHotels = (mergeContext.nearbyHotels - hotel)))
    }
}

/**
 * Merging player choose the next hotel to defunct.
 *
 * @property survivingHotel the hotel to survive the merger.
 * @property remainingHotels the remaining hotels to be merged.
 * @property potentialNextDefunctHotels candidates to be the next defunct hotel.
 *
 */
class ChooseDefunctHotel(override val acqObj: AcqObj,
                         val mergeContext: MergeContext,
                         val survivingHotel: HotelId,
                         val remainingHotels: List<HotelId>,
                         val potentialNextDefunctHotels: List<HotelId>) : AcqSmState() {

    /**
     * Player choose the next hotel to defunct.
     *
     * Transitions to:
     *  - ChooseDefunctHotel
     *  - PayBonuses
     */
    fun chooseHotelToDefunct(player: PlayerId, hotel: HotelId): Transition<AcqSmState> {
        if (mergeContext.gameState.isNotCurrentPlayer(player)) {
            logger.warn { "[$player] is not the current player [${mergeContext.gameState.currentPlayer}]" }
            return Transition(Responses.FAILURE("It is not $player's turn; it is ${mergeContext.gameState.currentPlayer}'s turn"), this)
        }
        if (!potentialNextDefunctHotels.contains(hotel)) {
            logger.warn { "[$player] chosen hotel $hotel to defunct is not a candidate. The candidates are: $potentialNextDefunctHotels" }
            return Transition(Responses.FAILURE("$player requested a hotel $hotel that is is not a defunct candidate. The candidates are $potentialNextDefunctHotels"), this)
        }

        logger.info { "[$player] choose to defunct hotel [$hotel] from candidates [$potentialNextDefunctHotels] with remaining hotels: $remainingHotels" }
        val mergeState = MergeState(mergeContext,
                                    survivingHotel,
                                    defunctHotel = hotel,
                                    remainingHotels = remainingHotels)
        val playersToPay = acqObj.stockBonuses(hotel)
        return Transition(Responses.SUCCESS("$player choose to defunct $hotel"),
                          PayBonuses(acqObj, mergeState, playersToPay))
    }
}

/**
 * Pay bonuses to the stock holders of the hotel being merged.
 *
 * @property playersToPay map of players receiving bonuses and how much to pay them.
 */
class PayBonuses(override val acqObj: AcqObj,
                 val mergeState: MergeState,
                 val playersToPay: Map<PlayerId, Int>): AcqSmState() {

    /**
     * Player receives their bonus.
     *
     * Transitions to:
     *  - PayBonuses
     *  - HandleDefunctHotelStocks
     */
    fun payBonus(player: PlayerId): Transition<AcqSmState> {
        if (!playersToPay.contains(player)) {
            logger.warn { "[$player] does not receive any bonuses. The players who do: $playersToPay" }
            return Transition(Responses.FAILURE("$player does not receive any bonuses"), this)
        }

        val amount = playersToPay[player]!!
        val nextAcqObj = acqObj.addMoney(player, amount)
        val nextPlayersToPay = playersToPay - player
        logger.info { "[$player] received a bonus of [$$amount] for the merger of hotel [${mergeState.defunctHotel}] into hotel [${mergeState.survivingHotel}]" }

        val nextState = if (nextPlayersToPay.isNotEmpty()) PayBonuses(nextAcqObj, mergeState, nextPlayersToPay)
                        else {
                            // order players with stocks to handle them. The merging player is first
                            var playersTurnOrder = mergeState.mergeContext.gameState.playerTurnOrder
                            while (playersTurnOrder.first() != mergeState.mergeContext.gameState.currentPlayer) {
                                val lastPlayer = playersTurnOrder.last()
                                playersTurnOrder = listOf(lastPlayer) + playersTurnOrder.dropLast(1)
                            }
                            val playersWithStocks = playersTurnOrder.filter { id -> acqObj.player(id).stock(mergeState.defunctHotel) > 0 }
                            HandleDefunctHotelStocks(nextAcqObj,
                                                     mergeState,
                                                     playersWithStocks)
        }
        return Transition(Responses.SUCCESS("$player paid $$amount for the merger of ${mergeState.defunctHotel}"), nextState)
    }
}

/**
 * Players trade, sell or keep their stock of the defunct hotel.
 *
 * @property playersWithStockInTurnOrder the players with stock to handle, in turn order.
 */
class HandleDefunctHotelStocks(override val acqObj: AcqObj,
                               val mergeState: MergeState,
                               val playersWithStockInTurnOrder: List<PlayerId>): AcqSmState() {

    /**
     * Player handles their defunct stocks.
     *
     * Transitions to:
     *  - HandleDefunctHotelStocks
     *  - BuyStock
     *  - ChooseDefunctHotel
     *  - PayBonuses
     *
     * @param trade the number of defunct stocks to trade (must be multiple of two).
     * @param sell the number of defunct stocks to sell.
     * @param keep the number of defunct stocks to keep.
     */
    fun handleDefunctHotelStocks(player: PlayerId, trade: Int, sell: Int, keep: Int): Transition<AcqSmState> {
        if (player != playersWithStockInTurnOrder.first()) {
            logger.warn { "[$player] cannot yet handle defunct stocks. Waiting for player [${playersWithStockInTurnOrder.first()}]" }
            return Transition(Responses.FAILURE("$player cannot yet handle defunct stocks"), this)
        }
        if (trade < 0 || sell < 0 || keep < 0) {
            logger.warn {"[$player] requested trade [$trade] sell [$sell] keep [$keep] less than 0"}
            return Transition(Responses.FAILURE("$player must request a positive number of stocks"), this)
        }
        if ((trade % 2) != 0) {
            logger.warn { "[$player] requested to trade an odd number of stocks" }
            return Transition(Responses.FAILURE("$player requested to trade an odd number of stocks"), this)
        }
        val numPlayerStocks = acqObj.player(player).stock(mergeState.defunctHotel)
        if (numPlayerStocks != (trade + sell + keep)) {
            logger.warn { "[$player] requested a different number of stocks ${(trade + sell + keep)} then they have $numPlayerStocks" }
            return Transition(Responses.FAILURE("$player requested a different number of stocks ${(trade + sell + keep)} then they have $numPlayerStocks"), this)
        }
        val bankAvailableStocks = acqObj.bank.stock(mergeState.survivingHotel)
        if (bankAvailableStocks < (trade / 2)) {
            logger.warn { "[$player] requested to trade [$trade] for more stocks than available in the bank [$bankAvailableStocks]" }
            return Transition(Responses.FAILURE("$player requesting to trade ($trade) for more stocks than available in the bank $bankAvailableStocks"), this)
        }

        val nextAcqObj = acqObj.depositStock(player, mergeState.defunctHotel, trade)
                               .withdrawStock(player, mergeState.survivingHotel, trade / 2)
                               .sellStocks(player, mergeState.defunctHotel, sell)
        val nextPlayersWithStocksInTurnOrder = playersWithStockInTurnOrder - player
        logger.info { "[$player] traded [$trade], sold [$sell], and kept [$keep] stocks of [${mergeState.defunctHotel}]" }
        val nextState = if (nextPlayersWithStocksInTurnOrder.isNotEmpty()) HandleDefunctHotelStocks(nextAcqObj, mergeState, nextPlayersWithStocksInTurnOrder)
                        else                                               nextDefunctHotelSmState(nextAcqObj,
                                                                                                   mergeState.mergeContext,
                                                                                                   mergeState.survivingHotel,
                                                                                                   defunctHotels = (mergeState.remainingHotels - mergeState.defunctHotel))
        return Transition(Responses.SUCCESS("$player traded $trade, sold $sell, and kept $keep stocks of ${mergeState.defunctHotel}"), nextState)
    }
}

/**
 * Exit out of a merger.
 *
 * Transitions to:
 *  - BuyStock
 *
 * @param survivingHotel the hotel which survived the merger.
 */
fun endMerger(acqObj: AcqObj,
              mergeContext: MergeContext,
              survivingHotel: HotelId): AcqSmState {
    val mergedTiles = mergeContext.nearbyHotels.flatMap { id -> (acqObj.hotel(id).state() as HotelState.OnBoard).tiles } + mergeContext.placedTile
    val mergedHotels = mergeContext.nearbyHotels - survivingHotel

    // set the placed tile and all nearby tiles as a part of the surviving hotel
    val nextTiles = acqObj.tiles + mergedTiles.associateBy({ id -> id },
                                                           { id -> acqObj.tile(id).withState(TileState.OnBoardHotel(survivingHotel)) })
    // set all merged hotels to available and set the surviving hotel with correct tiles
    val nextHotels = acqObj.hotels + mergedHotels.associateBy({ id -> id },
                                                              { id -> acqObj.hotel(id).withState(HotelState.Available) }) +
                                     Pair(survivingHotel, acqObj.hotel(survivingHotel).withState(HotelState.OnBoard(mergedTiles)))
    val nextAcqObj = acqObj.copy(tiles = nextTiles.toSortedMap(),
                                 hotels = nextHotels.toSortedMap())
    return BuyStock(nextAcqObj, mergeContext.gameState)
}