package us.whodag.acquire.obj.hotel

import us.whodag.acquire.obj.tile.TileId

/** Potential states for a hotel. */
sealed class HotelState {

    /** The hotel is available to be started. */
    object Available : HotelState() {
        override fun toString(): String = "Available"
    }

    /** The hotel is currently on the board but not safe. */
    class OnBoard(val tiles: Collection<TileId>) : HotelState() {
        override fun toString(): String = "OnBoard [${tiles.size}]"
    }
}

/** Whether the hotel is available to start. */
fun HotelState.isAvailable(): Boolean =
        when (this) {
            HotelState.Available -> true
            is HotelState.OnBoard -> false
        }

/** Whether the hotel is on the board. */
fun HotelState.isOnBoard(): Boolean =
        when (this) {
            HotelState.Available -> false
            is HotelState.OnBoard -> true
        }

/** The size of the hotel. */
fun HotelState.size(): Int =
        when (this) {
            HotelState.Available -> 0
            is HotelState.OnBoard -> tiles.size
        }