package us.whodag.acquire.req

import us.whodag.acquire.Acquire
import us.whodag.acquire.obj.availableHotels
import us.whodag.acquire.obj.canEndGame
import us.whodag.acquire.obj.hotel.HotelId
import us.whodag.acquire.obj.hotelsOnBoard
import us.whodag.acquire.obj.player
import us.whodag.acquire.obj.player.PlayerId
import us.whodag.acquire.sm.*

/**
 * Functions for working with AcqReqs.
 */
object AcqReqs {

    /** Return the possible Acquire requests for the specified player for the games current state. */
    fun Acquire.possibleReqs(player: PlayerId): List<AcqReq> {
        val sm = state().acqSmState
        if (!sm.doesPlayerHaveRequest(player)) return emptyList()
        return when (sm) {
            is DrawTurnTile -> listOf(DrawTileReq(player))
            is PlaceTurnTile -> listOf(sm.possibleReq(player))
            is DrawInitialTiles -> listOf(DrawTileReq(player))
            is PlaceTile -> sm.possibleReqs(player)
            is StartHotel -> sm.possibleReqs(player)
            is FoundersStock -> listOf(AcceptStockReq(player))
            is BuyStock -> sm.possibleReqs(player)
            is DrawTile -> sm.possibleReqs(player)
            is EndGamePayout -> listOf(AcceptMoneyReq(player))
            is GameOver -> emptyList()
            is ChooseSurvivingHotel -> sm.possibleReqs(player)
            is ChooseDefunctHotel -> sm.possibleReqs(player)
            is PayBonuses -> listOf(AcceptMoneyReq(player))
            is HandleDefunctHotelStocks -> sm.possibleReqs(player)
        }
    }

    /** Return the players which have a possible request to make. */
    fun Acquire.playersWithRequest(): List<PlayerId> {
        val smState = state().acqSmState
        return players().filter { smState.doesPlayerHaveRequest(it) }
    }

    /* Whether player has an action they may take in the current state. */
    private fun AcqSmState.doesPlayerHaveRequest(player: PlayerId): Boolean =
            when (this) {
                is DrawTurnTile -> !playersDrawn.contains(player)
                is PlaceTurnTile -> !playersPlaced.containsKey(player)
                is DrawInitialTiles -> !playersDrawn.contains(player)
                is PlaceTile -> gameState.isCurrentPlayer(player)
                is StartHotel -> gameState.isCurrentPlayer(player)
                is FoundersStock -> gameState.isCurrentPlayer(player)
                is BuyStock -> gameState.isCurrentPlayer(player)
                is DrawTile -> gameState.isCurrentPlayer(player)
                is EndGamePayout -> !playersPaid.contains(player)
                is GameOver -> false
                is ChooseSurvivingHotel -> mergeContext.gameState.isCurrentPlayer(player)
                is ChooseDefunctHotel -> mergeContext.gameState.isCurrentPlayer(player)
                is PayBonuses -> playersToPay.containsKey(player)
                is HandleDefunctHotelStocks -> playersWithStockInTurnOrder.first() == player
            }

    private fun PlaceTurnTile.possibleReq(player: PlayerId): AcqReq {
        val tile = acqObj.player(player).tiles().iterator().next()
        return PlaceTileReq(player, tile)
    }

    private fun PlaceTile.possibleReqs(player: PlayerId): List<AcqReq> {
        val tiles = acqObj.player(player).tiles()
        val successfulTiles = tiles.filter { tile -> placeTile(player, tile).response.isSuccess() }
        val placeTileReqs = successfulTiles.map { tile -> PlaceTileReq(player, tile) }
        val endGameReqs = if (acqObj.canEndGame()) listOf(EndGameReq(player))
        else emptyList()
        return placeTileReqs + endGameReqs
    }

    private fun DrawTile.possibleReqs(player: PlayerId): List<AcqReq> {
        val drawTileReqs = listOf(DrawTileReq(player))
        val endGameReqs = if (acqObj.canEndGame()) listOf(EndGameReq(player))
        else emptyList()
        return drawTileReqs + endGameReqs
    }

    private fun StartHotel.possibleReqs(player: PlayerId): List<AcqReq> {
        val hotels = acqObj.availableHotels()
        return hotels.map { hotel -> ChooseHotelReq(player, hotel) }
    }

    private fun ChooseSurvivingHotel.possibleReqs(player: PlayerId): List<AcqReq> {
        val hotels = potentialSurvivingHotels
        return hotels.map { hotel -> ChooseHotelReq(player, hotel) }
    }

    private fun ChooseDefunctHotel.possibleReqs(player: PlayerId): List<AcqReq> {
        val hotels = potentialNextDefunctHotels
        return hotels.map { hotel -> ChooseHotelReq(player, hotel) }
    }

    private fun BuyStock.possibleReqs(player: PlayerId): List<AcqReq> {
        val stockLimit = acqObj.player(player).stockTurnLimit()
        val hotelsOnBoard = acqObj.hotelsOnBoard()
        val hotelBuyPairs = hotelsOnBoard.associateBy({ it }, { (0..stockLimit).map { amount -> Pair(it, amount) } })
        val buyPairCombinations = combinations(hotelBuyPairs.values.toList())
        val buyMaps: List<Map<HotelId, Int>> = buyPairCombinations.fold(emptyList()) { buyMaps, buyPairs ->
            val buyMap: Map<HotelId, Int> = buyPairs.fold(emptyMap()) { map, buyPair -> map + buyPair }
            buyMaps + buyMap
        }
        val successfulBuyMaps = buyMaps.filter { buyMap -> buyStock(player, buyMap).response.isSuccess() }
        return successfulBuyMaps.map { buyMap -> BuyStockReq(player, buyMap) }
    }

    private fun HandleDefunctHotelStocks.possibleReqs(player: PlayerId): List<AcqReq> {
        val defunctHotel = mergeState.defunctHotel
        val playerStocks = acqObj.player(player).stock(defunctHotel)
        val tradeSellKeep = listOf((0..playerStocks).toList(), (0..playerStocks).toList(), (0..playerStocks).toList())
        val tradeSellKeepCombinations = combinations(tradeSellKeep)
        val triples: List<Triple<Int, Int, Int>> = tradeSellKeepCombinations.fold(emptyList()) { triples, (trade, sell, keep) ->
            triples + Triple(trade, sell, keep)
        }
        val successfulTriples = triples.filter { (trade, sell, keep) -> handleDefunctHotelStocks(player, trade, sell, keep).response.isSuccess() }
        return successfulTriples.map { (trade, sell, keep) -> HandleStocksReq(player, trade, sell, keep) }
    }
}

/* Return the combinations of the elements for each inner list. */
private fun <T> combinations(list: List<List<T>>): List<List<T>> {
    if (list.size == 1) {
        return list.first().map { listOf(it) }
    }
    val first = list.first()
    val rest = list.drop(1)
    val restCombinations = combinations(rest)
    val result = mutableListOf<List<T>>()
    for (i in first) {
        for (c in restCombinations) {
            result.add(listOf(i) + c)
        }
    }
    return result
}
