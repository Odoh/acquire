package us.whodag.acquire.obj.player

import us.whodag.acquire.obj.hotel.HotelId
import us.whodag.acquire.obj.tile.TileId

/**
 * Describes a Player in Acquire.
 */
interface Player {

    /** Unique identifier. */
    fun id(): PlayerId

    /** The number of tiles that may be in the player's hand. */
    fun handLimit(): Int

    /** The number of stock that may be bought per turn. */
    fun stockTurnLimit(): Int

    /** The amount of money the Player has. */
    fun money(): Int

    /** Add the specified amount of money to the Player. */
    fun addMoney(amount: Int): Player

    /** Remove the specified amount of money from the Player */
    fun removeMoney(amount: Int): Player

    /** The tiles in the Player's hand. */
    fun tiles(): Collection<TileId>

    /** Add the specified tile to the Player's hand. */
    fun addTile(tile: TileId): Player

    /** Remove the specified tile from the Player's hand. */
    fun removeTile(tile: TileId): Player

    /** The amount of stocks the Player has. */
    fun stocks(): Map<HotelId, Int>

    /** The amount of stocks for the specified hotel. */
    fun stock(hotel: HotelId): Int

    /** Add the stock of the specified hotel and amount to a Player. */
    fun addStock(hotel: HotelId, amount: Int): Player

    /** Remove the stock of the specified hotel and amount to a Player. */
    fun removeStock(hotel: HotelId, amount: Int): Player

    /** Whether player has tile in hand. */
    fun hasTile(tile: TileId): Boolean = tiles().contains(tile)

    /** Whether player has stock of hotel. */
    fun hasStock(hotel: HotelId): Boolean = stock(hotel) > 0
}