package us.whodag.acquire.req

import us.whodag.acquire.obj.hotel.HotelId
import us.whodag.acquire.obj.player.PlayerId
import us.whodag.acquire.obj.tile.TileId

/**
 * A request which may be submitted to an Acquire game.
 */
sealed class AcqReq : PlayerReq()

/** A request which originates from a player. */
abstract class PlayerReq {
    abstract val player: PlayerId
}

/** Request for a player to accept a payment of money. */
data class AcceptMoneyReq(override val player: PlayerId) : AcqReq()

/** Request for a player to accept stock from the bank. */
data class AcceptStockReq(override val player: PlayerId) : AcqReq()

/** Request for a player to buy the stocks specified in the buyStocks map. */
data class BuyStockReq(override val player: PlayerId, val buyStocks: Map<HotelId, Int>) : AcqReq()

/** Request for a player to choose a hotel. */
data class ChooseHotelReq(override val player: PlayerId, val hotel: HotelId) : AcqReq()

/** Request for a player to draw a tile from the draw pile. */
data class DrawTileReq(override val player: PlayerId) : AcqReq()

/** Request for a player to end the game. */
data class EndGameReq(override val player: PlayerId) : AcqReq()

/** Request for a player to trade, sell, and/or keep stocks. */
data class HandleStocksReq(override val player: PlayerId, val trade: Int = 0, val sell: Int = 0, val keep: Int = 0) : AcqReq()

/** Request for a player to place a tile. */
data class PlaceTileReq(override val player: PlayerId, val tile: TileId) : AcqReq()

/** Request for a player to start the game. */
data class StartGameReq(override val player: PlayerId) : AcqReq()

/** Request for a player to undo his last request. */
data class UndoReq(override val player: PlayerId) : AcqReq()

/** Request for a player to accept another players undo request. */
data class AcceptUndoReq(override val player: PlayerId) : AcqReq()