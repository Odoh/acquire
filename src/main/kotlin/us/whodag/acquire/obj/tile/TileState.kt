package us.whodag.acquire.obj.tile

import us.whodag.acquire.obj.hotel.HotelId
import us.whodag.acquire.obj.player.PlayerId

/**
 * Potential states for a tile.
 */
sealed class TileState {

    /** The tile is currently in the draw pile. */
    object DrawPile : TileState() {
        override fun toString(): String = "DrawPile"
    }

    /** The tile is currently in the hand of player "player". */
    class PlayerHand(var player: PlayerId) : TileState() {
        override fun toString(): String = "PlayerHand [$player]"
    }

    /** The tile is currently on the board but not a part of a hotel. */
    object OnBoard : TileState() {
        override fun toString(): String = "OnBoard"
    }

    /** The tile is currently on the board and a part of the hotel "hotel". */
    class OnBoardHotel(val hotel: HotelId) : TileState() {
        override fun toString(): String = "OnBoardHotel [$hotel]"
    }

    /** The tile has been discarded from the game. */
    object Discarded: TileState() {
        override fun toString(): String = "Discarded"
    }
}

/** Return the hotel this tile is a part of, if any. */
fun TileState.hotel(): HotelId? =
        when (this) {
            TileState.DrawPile, is TileState.PlayerHand, TileState.Discarded, TileState.OnBoard -> null
            is TileState.OnBoardHotel -> this.hotel
        }