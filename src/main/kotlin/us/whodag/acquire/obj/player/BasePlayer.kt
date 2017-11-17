package us.whodag.acquire.obj.player

import us.whodag.acquire.obj.hotel.HotelId
import us.whodag.acquire.obj.tile.TileId

/**
 * Base implementation of a Player.
 *
 * @property id unique identifier
 * @property handLimit the max number of tiles the Player may have in their hand.
 * @property stockTurnLimit the maximum number of stocks the Player may buy per turn.
 * @property money amount of money the Player has.
 * @property tiles the tiles that the Player's hand has.
 * @property stocks the stocks the Player has.
 */
data class BasePlayer(private val id: PlayerId,
                      private val handLimit: Int,
                      private val stockTurnLimit: Int,
                      private val money: Int,
                      private val tiles: Collection<TileId>,
                      private val stocks: Map<HotelId, Int>): Player {

    override fun id(): PlayerId = id
    override fun handLimit(): Int = handLimit
    override fun stockTurnLimit(): Int = stockTurnLimit

    override fun money(): Int = money
    override fun addMoney(amount: Int): Player {
        require(amount >= 0, { "Can only add a positive amount of money" })
        return copy(money = money + amount)
    }
    override fun removeMoney(amount: Int): Player {
        require(amount >= 0, { "Can only remove a positive amount of money" })
        val newAmount = money - amount
        require(newAmount >= 0, { "Must have a positive amount of money remaining after removal" })
        return copy(money = newAmount)
    }

    override fun tiles(): Collection<TileId> = tiles
    override fun addTile(tile: TileId): Player {
        require(tiles.size + 1 <= handLimit, { "Cannot add a tile when it would exceed the players handlimit" })
        require(!tiles.contains(tile), { "Cannot add a tile the player already has" })
        return copy(tiles = tiles + tile)
    }
    override fun removeTile(tile: TileId): Player {
        require(tiles.contains(tile), { "Cannot remove a tile the player does not have" })
        return copy(tiles = tiles - tile)
    }

    override fun stocks(): Map<HotelId, Int> = stocks
    override fun stock(hotel: HotelId): Int = stocks[hotel]!!
    override fun addStock(hotel: HotelId, amount: Int): Player {
        require(amount >= 0, { "Can only add a positive amount of stock" })
        require(stocks.containsKey(hotel), { "Stock map must contain the hotel to add stock in" })
        val newAmount = stocks[hotel]!! + amount
        return copy(stocks = stocks + Pair(hotel, newAmount))
    }
    override fun removeStock(hotel: HotelId, amount: Int): Player {
        require(amount >= 0, { "Can only remove a positive amount of stock" })
        require(stocks.containsKey(hotel), { "Stock map must contain the hotel to remove stock from" })
        val newAmount = stocks[hotel]!! - amount
        require(newAmount >= 0, { "Must have a positive amount of stock remaining after removal" })
        return copy(stocks = stocks + Pair(hotel, newAmount))
    }
}