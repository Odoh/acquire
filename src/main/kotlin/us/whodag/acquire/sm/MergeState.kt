package us.whodag.acquire.sm

import us.whodag.acquire.obj.hotel.HotelId
import us.whodag.acquire.obj.tile.TileId

/**
 * The context of the game state upon entering a merger.
 *
 * @property gameState the game state.
 * @property placedTile the tile that was placed causing the merger.
 * @property nearbyHotels the hotels that make up the nearby tiles.
 */
data class MergeContext(
    val gameState: GameState,
    val placedTile: TileId,
    val nearbyHotels: List<HotelId>
)

/**
 * Acquire state machine state which is tracked throughout a merger.
 *
 * @property mergeContext the merge context.
 * @property survivingHotel the hotel to survive the merger.
 * @property defunctHotel the hotel currently being defunct.
 * @property remainingHotels the remaining hotels to merge.
 */
data class MergeState(
    val mergeContext: MergeContext,

    // state for the merge
    val survivingHotel: HotelId,
    val defunctHotel: HotelId,
    val remainingHotels: List<HotelId>
)