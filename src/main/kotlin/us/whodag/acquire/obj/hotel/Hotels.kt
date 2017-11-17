package us.whodag.acquire.obj.hotel

/**
 * Factory for creating Hotels.
 */
object Hotels {

    val STANDARD_ID_TIERS = mapOf<HotelId, HotelTier>(HotelId("luxor") to StandardHotelTiers.Low,
                                                      HotelId("tower") to StandardHotelTiers.Low,
                                                      HotelId("american") to StandardHotelTiers.Mid,
                                                      HotelId("festival") to StandardHotelTiers.Mid,
                                                      HotelId("worldwide") to StandardHotelTiers.Mid,
                                                      HotelId("continental") to StandardHotelTiers.High,
                                                      HotelId("imperial") to StandardHotelTiers.High)

    /** HotelTiers for a standard game. */
    enum class StandardHotelTiers : HotelTier {
        Low {
            override fun stockPricePerSize(size: Int): Int =
                when (size) {
                    0 -> 0
                    2 -> 200
                    3 -> 300
                    4 -> 400
                    5 -> 500
                    in 6..10 -> 600
                    in 11..20 -> 700
                    in 21..30 -> 800
                    in 31..40 -> 900
                    else -> 1000
                }
            override fun majorityBonusPerSize(size: Int): Int = 10 * stockPricePerSize(size)
            override fun minorityBonusPerSize(size: Int): Int = 5 * stockPricePerSize(size)
        },
        Mid {
            override fun stockPricePerSize(size: Int): Int =
                when (size) {
                    0 -> 0
                    2 -> 300
                    3 -> 400
                    4 -> 500
                    5 -> 600
                    in 6..10 -> 700
                    in 11..20 -> 800
                    in 21..30 -> 900
                    in 31..40 -> 1000
                    else -> 1100
                }
            override fun majorityBonusPerSize(size: Int): Int = 10 * stockPricePerSize(size)
            override fun minorityBonusPerSize(size: Int): Int = 5 * stockPricePerSize(size)
        },
        High {
            override fun stockPricePerSize(size: Int): Int =
                when (size) {
                    0 -> 0
                    2 -> 400
                    3 -> 500
                    4 -> 600
                    5 -> 700
                    in 6..10 -> 800
                    in 11..20 -> 900
                    in 21..30 -> 1000
                    in 31..40 -> 1100
                    else -> 1200
                }
            override fun majorityBonusPerSize(size: Int): Int = 10 * stockPricePerSize(size)
            override fun minorityBonusPerSize(size: Int): Int = 5 * stockPricePerSize(size)
        }
    }

    /** Create the hotels a standard game. */
    fun standard(): List<Hotel> =
            Hotels.STANDARD_ID_TIERS.entries.map { (id, tier) -> standardHotel(id, tier) }

    /** Create a single hotel of the specified tier for a standard game. */
    fun standardHotel(id: HotelId, tier: HotelTier): Hotel =
            customHotel(id,
                        HotelState.Available,
                        safeSize = 11,
                        endGameSize = 41,
                        stockPricePerSize = tier::stockPricePerSize,
                        majorityBonusPerSize = tier::majorityBonusPerSize,
                        minorityBonusPerSize = tier::minorityBonusPerSize)

    /** Create a single, custom hotel. */
    fun customHotel(id: HotelId,
                    state: HotelState,
                    safeSize: Int,
                    endGameSize: Int,
                    stockPricePerSize: (Int) -> Int,
                    majorityBonusPerSize: (Int) -> Int,
                    minorityBonusPerSize: (Int) -> Int): Hotel =
            BaseHotel(id,
                      state,
                      safeSize,
                      endGameSize,
                      stockPricePerSize,
                      majorityBonusPerSize,
                      minorityBonusPerSize)
}