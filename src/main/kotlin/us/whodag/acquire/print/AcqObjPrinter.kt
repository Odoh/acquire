package us.whodag.acquire.print

import mu.KLogging
import us.whodag.acquire.obj.AcqObj
import us.whodag.acquire.obj.bank.Bank
import us.whodag.acquire.obj.board.Board
import us.whodag.acquire.obj.hotel.Hotel
import us.whodag.acquire.obj.hotel.HotelId
import us.whodag.acquire.obj.hotel.HotelState
import us.whodag.acquire.obj.player.Player
import us.whodag.acquire.obj.player.PlayerId
import us.whodag.acquire.obj.tile.Tile
import us.whodag.acquire.obj.tile.TileId
import us.whodag.acquire.obj.tile.TileState
import us.whodag.acquire.obj.tile.Tiles

/**
 * Printer for printing AcqObjs.
 *
 * @property origPlayers the original list of players which make up the to-be printed objects.
 * @property origHotels the original list of hotels which make up the to-be printed objects.
 * @property origBank the original bank which makes up the to-be printed objects.
 */
class AcqObjPrinter(private val origPlayers: List<PlayerId>,
                    private val origHotels: List<HotelId>,
                    private val origBank: Bank) {
    constructor(acqObj: AcqObj) : this(acqObj.players.keys.sorted(),
                                       acqObj.hotels.keys.sorted(),
                                       acqObj.bank)

    companion object: KLogging()

    /* Comparators */
    object StockComparator : Comparator<Map.Entry<HotelId, Int>> {
        override fun compare(p0: Map.Entry<HotelId, Int>?, p1: Map.Entry<HotelId, Int>?): Int = p0!!.key.compareTo(p1!!.key)
    }

    private val MAX_PLAYER_ID_LEN = origPlayers.map { p -> p.toString().length }.max()!!
    private val MAX_HOTEL_ID_LEN = origHotels.map { h -> h.toString().length }.max()!!
    private val ALL_HOTEL_NAME_LEN = origHotels.map { h -> h.toString().length }.sum()
    private val MAX_HOTEL_STATE_LEN = HotelState.OnBoard(emptyList()).toString().length + 3
    private val MAX_TILE_STATE_LEN = maxOf(TileState.PlayerHand(PlayerId(".".repeat(MAX_PLAYER_ID_LEN))).toString().length,
                                           TileState.OnBoardHotel(HotelId(".".repeat(MAX_HOTEL_ID_LEN))).toString().length)

    private val bankTable: List<Column<Bank>> = listOf(
        object : Column<Bank> {
            override fun title(): String = "DrawSize"
            override fun valueSize(): Int = 3
            override fun value(e: Bank): String = e.drawPile().size.toString()
        }, object : Column<Bank> {
            override fun title(): String = "Stocks"
            override fun valueSize(): Int =
                    ALL_HOTEL_NAME_LEN + // all hotel names
                    origHotels.size * 3 +    // [:##]
                    (origHotels.size - 1)    // [" "]
            override fun value(e: Bank): String =
                    e.stocks().entries.toSortedSet(StockComparator).joinToString(separator = " ") { (s, n) -> if (n == 0) ""
                                                                                                              else        "$s:$n" }
    }, object : Column<Bank> {
            override fun title(): String = "DrawPile"
            override fun valueSize(): Int =
                    origBank.drawPile().size * 4 + // all tile names
                    (origBank.drawPile().size - 1) // [" "]
            override fun value(e: Bank): String = e.drawPile().reversed().joinToString(separator = " ") { t -> t.toString() }
    })

    private val playerTable: List<Column<Player>> = listOf(
        object : Column<Player> {
            override fun title(): String = "Id"
            override fun valueSize(): Int = MAX_PLAYER_ID_LEN
            override fun value(e: Player): String = e.id().toString()
        }, object : Column<Player> {
            override fun title(): String = "Money"
            override fun valueSize(): Int = 7
            override fun value(e: Player): String = "$${e.money()}"
        }, object : Column<Player> {
            override fun title(): String = "Tiles"
            override fun valueSize(): Int = 29 // 6 * [tileId (4)] + 5 * [" " (1)]
            override fun value(e: Player): String = e.tiles().sorted().joinToString(separator = " ")
        }, object : Column<Player> {
            override fun title(): String = "Stocks"
            override fun valueSize(): Int =
                    ALL_HOTEL_NAME_LEN + // all hotel names
                    origHotels.size * 3 +    // [:##]
                    (origHotels.size - 1)    // [" "]
            override fun value(e: Player): String =
                    e.stocks().entries.toSortedSet(StockComparator).joinToString(separator = " ") { (s, n) -> if (n == 0) ""
                                                                                                              else        "$s:$n" }
        }, object : Column<Player> {
            override fun title(): String = "HandLimit"
            override fun valueSize(): Int = 3
            override fun value(e: Player): String = e.handLimit().toString()
        }, object : Column<Player> {
            override fun title(): String = "StockTurnLimit"
            override fun valueSize(): Int = 2
            override fun value(e: Player): String = e.stockTurnLimit().toString()
        })

    private val hotelTable: List<Column<Hotel>> = listOf(
        object : Column<Hotel> {
            override fun title(): String = "Id"
            override fun valueSize(): Int = MAX_HOTEL_ID_LEN
            override fun value(e: Hotel): String = e.id().toString()
        }, object : Column<Hotel> {
            override fun title(): String = "State"
            override fun valueSize(): Int = MAX_HOTEL_STATE_LEN
            override fun value(e: Hotel): String = e.state().toString()
        }, object : Column<Hotel> {
            override fun title(): String = "Stock"
            override fun valueSize(): Int = 4
            override fun value(e: Hotel): String = "$${e.stockPrice()}"
        }, object : Column<Hotel> {
            override fun title(): String = "Majority"
            override fun valueSize(): Int = 5
            override fun value(e: Hotel): String = "$${e.majorityBonus()}"
        }, object : Column<Hotel> {
            override fun title(): String = "Minority"
            override fun valueSize(): Int = 5
            override fun value(e: Hotel): String = "$${e.minorityBonus()}"
        }, object : Column<Hotel> {
            override fun title(): String = "Safe"
            override fun valueSize(): Int = 2
            override fun value(e: Hotel): String = e.safeSize().toString()
        }, object : Column<Hotel> {
            override fun title(): String = "EndGame"
            override fun valueSize(): Int = 2
            override fun value(e: Hotel): String = e.endGameSize().toString()
    })

    private interface Column<in T> {
        fun title(): String
        fun valueSize(): Int
        fun value(e: T): String
        fun size(): Int = maxOf(title().length, valueSize())
    }

    private fun <T> printTable(table: List<Column<T>>, entries: List<T>) {
        val header = table.map { c -> c.title().padEnd(c.size()) }.joinToString(separator = " | ")
        val underline = "-".repeat(header.length)
        val endline = "=".repeat(header.length)
        val lines = entries.map { entry ->
                      table.map { c -> c.value(entry).padEnd(c.size()) }.joinToString(separator = " | ") }
        logger.trace { header }
        logger.trace { underline }
        lines.forEach(logger::trace)
        logger.trace { endline }
    }

    private fun printBoard(board: Board, tiles: Map<TileId, Tile>) {
        val numberRow = StringBuilder("  ")
        for (y in 0 until board.maxY()) {
            var number = Tiles.NUMBERS[y]
            if (number.length > 1) {
                number = number.substring(1)
            }
            numberRow.append(" ").append(number).append(" ")
        }
        logger.trace { numberRow }

        for (x in 0 until board.maxX()) {
            val rowString = StringBuilder(Tiles.LETTERS[x]).append(" ")
            for (y in 0 until board.maxY()) {
                val tile = when (board.tile(x, y)) {
                    null -> "."
                    else -> {
                        val state = tiles[board.tile(x,y)!!]!!.state()
                        when (state) {
                            TileState.OnBoard -> "o"
                            is TileState.OnBoardHotel -> state.hotel.name.substring(0, 1).toUpperCase()
                            TileState.Discarded -> "x"
                            TileState.DrawPile, is TileState.PlayerHand -> "."
                        }
                    }
                }
                rowString.append(" ").append(tile).append(" ")
            }
            logger.trace { rowString }
        }
    }

    private fun printTiles(board: Board, tiles: Map<TileId, Tile>) {
        for (x in 0 until board.maxX()) {
            val rowString = StringBuilder()
            for (y in 0 until board.maxY()) {
                val tileId = TileId(Tiles.NUMBERS[y], Tiles.LETTERS[x])
                val tile = tiles[tileId]!!
                rowString.append(tileId).append(" ").append(tile.state().toString().padEnd(MAX_TILE_STATE_LEN)).append("  ")
            }
            logger.trace { rowString }
        }
    }

    /** Print the specified AcqObjs. */
    fun print(acqObj: AcqObj) {
        val bank = acqObj.bank
        val players = origPlayers.map { pid -> acqObj.players[pid]!! }
        val hotels = origHotels.map { hid -> acqObj.hotels[hid]!! }
        val board = acqObj.board
        val tiles = acqObj.tiles
        printTable(hotelTable, hotels)
        printTable(bankTable, listOf(bank))
        printTable(playerTable, players)
        printBoard(board, tiles)
        printTiles(board, tiles)
    }
}