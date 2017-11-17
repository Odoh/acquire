package us.whodag.acquire.obj.bank

import us.whodag.acquire.obj.hotel.HotelId
import us.whodag.acquire.obj.tile.TileId

/**
 * Base implementation of a bank in Acquire.
 *
 * @property totalStocksPerHotel the total number of stocks in the game for each hotel.
 * @property shuffleSeed the seed used to shuffle the draw pile.
 * @property drawPile the draw pile of tiles.
 * @property stocks the hotel stocks in the bank.
 */
data class BaseBank(private val totalStocksPerHotel: Int,
                    private val shuffleSeed: Long,
                    private val drawPile: List<TileId>,
                    private val stocks: Map<HotelId, Int>) : Bank {

    override fun totalStocksPerHotel(): Int = totalStocksPerHotel
    override fun shuffleSeed(): Long = shuffleSeed
    override fun drawPile(): List<TileId> = drawPile
    override fun drawTile(): Pair<Bank, TileId> {
        check(hasTileToDraw(), { "Must have a tile available to draw" })
        return Pair(copy(drawPile = drawPile.dropLast(1)), drawPile.last())
    }

    override fun stocks(): Map<HotelId, Int> = stocks
    override fun stock(hotel: HotelId): Int = stocks[hotel]!!
    override fun addStock(hotel: HotelId, amount: Int): Bank {
        require(amount >= 0, { "Can only add a positive amount of stock" })
        require(stocks.containsKey(hotel), { "Stock map must contain the hotel to add stock in" })
        val newAmount = stocks[hotel]!! + amount
        require(newAmount <= totalStocksPerHotel, { "Cannot add stock that would exceed the banks stock limit" })
        return copy(stocks = stocks + Pair(hotel, newAmount))
    }
    override fun removeStock(hotel: HotelId, amount: Int): Bank {
        require(amount >= 0, { "Can only remove a positive amount of stock" })
        val newAmount = stocks[hotel]!! - amount
        require(newAmount >= 0, { "Must have a positive amount of stock remaining after removal" })
        return copy(stocks = stocks + Pair(hotel, newAmount))
    }
}