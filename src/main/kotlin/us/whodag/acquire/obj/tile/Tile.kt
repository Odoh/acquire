package us.whodag.acquire.obj.tile

import us.whodag.acquire.obj.hotel.HotelId

/**
 * Describes a Tile in Acquire.
 */
interface Tile {

    /** Unique identifier. */
    fun id(): TileId

    /** State of the tile. */
    fun state(): TileState

    /** Construct a tile with the specified state. */
    fun withState(state: TileState): Tile

    /** Return the hotel this tile is a part of, if any. */
    fun hotel(): HotelId? = state().hotel()
}