package us.whodag.acquire.obj.bank

import us.whodag.acquire.obj.hotel.HotelId
import us.whodag.acquire.obj.tile.TileId

/**
 * Describes a bank in Acquire.
 */
interface Bank {

    /** The total number of stocks for each hotel. */
    fun totalStocksPerHotel(): Int

    /** The random seed used shuffling the draw pile. */
    fun shuffleSeed(): Long

    /** The tiles in the draw pile. */
    fun drawPile(): List<TileId>

    /** Draw a tile from the draw pile. */
    fun drawTile(): Pair<Bank, TileId>

    /** The stocks the bank has. */
    fun stocks(): Map<HotelId, Int>

    /** The amount of stock for the specified hotel. */
    fun stock(hotel: HotelId): Int

    /** Add an amount of stocks of the specified hotel to the bank. */
    fun addStock(hotel: HotelId, amount: Int): Bank

    /** Remove an amount of stocks of the specified hotel from the bank. */
    fun removeStock(hotel: HotelId, amount: Int): Bank

    /** Whether there is a tile to draw. */
    fun hasTileToDraw(): Boolean = !drawPile().isEmpty()

    /** Whether there is stock of hotel. */
    fun hasStock(hotel: HotelId): Boolean = stock(hotel) > 0
}