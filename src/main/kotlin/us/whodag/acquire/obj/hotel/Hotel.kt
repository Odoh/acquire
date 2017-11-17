package us.whodag.acquire.obj.hotel

/**
 * Describes a Hotel in Acquire.
 */
interface Hotel {

    /** Unique identifier. */
    fun id(): HotelId

    /** The size of the hotel when safe. */
    fun safeSize(): Int

    /** The size of the hotel when the game can be ended. */
    fun endGameSize(): Int

    /** State of the Hotel. */
    fun state(): HotelState

    /** Current stock price of the hotel. */
    fun stockPrice(): Int

    /** The majority holders bonus. */
    fun majorityBonus(): Int

    /** The minority holders bonus. */
    fun minorityBonus(): Int

    /** Construct a hotel with the specified state. */
    fun withState(state: HotelState): Hotel

    /** Whether the hotel is available to start. */
    fun isAvailable(): Boolean = state().isAvailable()

    /** Whether the hotel is on the board. */
    fun isOnBoard(): Boolean = state().isOnBoard()

    /** The size of the hotel. */
    fun size(): Int = state().size()

    /** Whether the hotel is safe. */
    fun isSafe(): Boolean = size() >= safeSize()

    /** Whether the hotel has a reached a size to end the game. */
    fun isEndGameSize(): Boolean = size() >= endGameSize()
}