package us.whodag.acquire.obj.player

import us.whodag.acquire.obj.hotel.HotelId
import us.whodag.acquire.obj.hotel.Hotels
import us.whodag.acquire.obj.tile.TileId

/**
 * Factory for creating Players.
 */
object Players {

    val STANDARD_HAND_LIMIT = 6
    val STANDARD_STOCK_TURN_LIMIT = 3
    val STANDARD_INITIAL_MONEY = 6000

    /** Create players with the specified ids for a standard game. */
    fun standard(ids: Collection<PlayerId>): List<Player> = ids.map { id -> standardPlayer(id) }

    /** Create a single player for a standard game. */
    fun standardPlayer(id: PlayerId,
                       handLimit: Int = STANDARD_HAND_LIMIT,
                       stockTurnLimit: Int = STANDARD_STOCK_TURN_LIMIT,
                       initialMoney: Int = STANDARD_INITIAL_MONEY,
                       initialTiles: Collection<TileId> = emptyList(),
                       initialStocks: Map<HotelId, Int> = Hotels.STANDARD_ID_TIERS.keys.associateBy({h -> h}, {_ -> 0})): Player {
        val fullInitialStocks = Hotels.STANDARD_ID_TIERS.keys.associateBy({ h -> h },
                                                                          { h -> if (initialStocks.containsKey(h)) initialStocks[h]!!
                                                                                 else 0 })
        return custom(id, handLimit, stockTurnLimit, initialMoney, initialTiles, fullInitialStocks)
    }

    /** Create a custom player. */
    fun custom(id: PlayerId,
               handLimit: Int,
               stockTurnLimit: Int,
               initialMoney: Int,
               initialTiles: Collection<TileId>,
               initialStocks: Map<HotelId, Int>): Player {
        return BasePlayer(id,
                          handLimit,
                          stockTurnLimit,
                          initialMoney,
                          initialTiles,
                          initialStocks)
    }
}