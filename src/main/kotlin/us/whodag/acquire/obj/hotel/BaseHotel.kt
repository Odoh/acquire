package us.whodag.acquire.obj.hotel

/**
 * Base implementation of a Hotel.
 *
 * @property id unique identifier.
 * @property state the state of the hotel.
 * @property safeSize the size of the hotel needed to be safe.
 * @property endGameSize the size of the hotel needed to end the game.
 * @property stockPricePerSize the stock price given the size of the hotel.
 * @property majorityBonusPerSize the majority bonus given the size of the hotel.
 * @property minorityBonusPerSize the minority bonus given the size of the hotel.
 */
data class BaseHotel(private val id: HotelId,
                     private val state: HotelState,
                     private val safeSize: Int,
                     private val endGameSize: Int,
                     private val stockPricePerSize: (Int) -> Int,
                     private val majorityBonusPerSize: (Int) -> Int,
                     private val minorityBonusPerSize: (Int) -> Int): Hotel {

    override fun id(): HotelId = id
    override fun safeSize(): Int = safeSize
    override fun endGameSize(): Int = endGameSize
    override fun state(): HotelState = state

    override fun stockPrice(): Int = stockPricePerSize.invoke(size())
    override fun majorityBonus(): Int = majorityBonusPerSize.invoke(size())
    override fun minorityBonus(): Int = minorityBonusPerSize.invoke(size())

    override fun withState(state: HotelState): Hotel = copy(state = state)
}