package us.whodag.acquire.obj.hotel

/**
 * Describes a hotel's stock prices, majority bonus, and minority bonus given its size.
 */
interface HotelTier {

    /** The stock price per size. */
    fun stockPricePerSize(size: Int): Int

    /** The majority bonus per size. */
    fun majorityBonusPerSize(size: Int): Int

    /** The minority bonus per size. */
    fun minorityBonusPerSize(size: Int): Int
}