package us.whodag.acquire.obj.bank

import us.whodag.acquire.obj.hotel.HotelId
import us.whodag.acquire.obj.hotel.Hotels
import us.whodag.acquire.obj.tile.TileId
import us.whodag.acquire.obj.tile.Tiles
import java.util.*

/**
 * Factory for creating Banks.
 */
object Banks {

    val STANDARD_TOTAL_STOCKS_PER_HOTEL = 25

    /** Create a bank for a standard game. */
    fun standard(shuffleSeed: Long = Math.abs(Random().nextLong())): Bank =
            custom(STANDARD_TOTAL_STOCKS_PER_HOTEL,
                   shuffleSeed,
                   shuffle(Tiles.STANDARD_IDS, Random(shuffleSeed)),
                   Hotels.STANDARD_ID_TIERS.keys.associateBy({ h -> h }, { _ -> STANDARD_TOTAL_STOCKS_PER_HOTEL }))

    /** Create a custom bank. */
    fun custom(totalStocksPerHotel: Int, shuffleSeed: Long, drawPile: List<TileId>, stocks: Map<HotelId, Int>): Bank =
            BaseBank(totalStocksPerHotel, shuffleSeed, drawPile, stocks)

    /** A copy of bank with stocks. */
    fun Bank.withStocks(stocks: Map<HotelId, Int>): Bank =
            custom(this.totalStocksPerHotel(), this.shuffleSeed(), this.drawPile(), stocks)

    /** A copy of bank with all hotels containing stocks. */
    fun Bank.withStocks(allHotelStockAmount: Int): Bank =
            custom(this.totalStocksPerHotel(),
                   this.shuffleSeed(),
                   this.drawPile(),
                   this.stocks().keys.associateBy({ h -> h }, { _ -> allHotelStockAmount }))

    /** A copy of bank with draw pile. */
    fun Bank.withDrawPile(drawPile: List<TileId>) =
            custom(this.totalStocksPerHotel(), this.shuffleSeed(), drawPile, this.stocks())

    /* Shuffle tiles using the random generator rand. */
    private fun shuffle(tiles: Collection<TileId>, rand: Random): List<TileId> {
        val array = tiles.toTypedArray()
        var x = array.size
        while (x > 0) {
            val r = rand.nextInt(x)
            val tempTile = array[r]
            array[r] = array[x - 1]
            array[x-- - 1] = tempTile
        }
        return array.toList()
    }
}