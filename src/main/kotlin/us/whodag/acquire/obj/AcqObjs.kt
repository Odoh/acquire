package us.whodag.acquire.obj

import mu.KLogging
import us.whodag.acquire.obj.bank.Bank
import us.whodag.acquire.obj.bank.Banks
import us.whodag.acquire.obj.board.Board
import us.whodag.acquire.obj.board.Boards
import us.whodag.acquire.obj.hotel.Hotel
import us.whodag.acquire.obj.hotel.HotelId
import us.whodag.acquire.obj.hotel.HotelState
import us.whodag.acquire.obj.hotel.Hotels
import us.whodag.acquire.obj.player.Player
import us.whodag.acquire.obj.player.PlayerId
import us.whodag.acquire.obj.player.Players
import us.whodag.acquire.obj.tile.Tile
import us.whodag.acquire.obj.tile.TileId
import us.whodag.acquire.obj.tile.TileState
import us.whodag.acquire.obj.tile.Tiles

/**
 * Factory for creating AcqObjs.
 */
object AcqObjs {

    object KLog : KLogging()

    /** Create an AcqObj for a standard game. */
    fun standard(players: Collection<PlayerId>) =
        custom(Tiles.standard(),
               Banks.standard(),
               Boards.standard(),
               Hotels.standard(),
               Players.standard(players))

    /** Create an AcqObj for a standard game using the specified random generator for shuffling the tiles. */
    fun standardShuffle(players: Collection<PlayerId>, shuffleSeed: Long): AcqObj =
        custom(Tiles.standard(),
               Banks.standard(shuffleSeed),
               Boards.standard(),
               Hotels.standard(),
               Players.standard(players))

    /**
     * Create a custom AcqObj.
     *
     * Several steps are taken to ensure all objects are created with the correct state without requiring extra work:
     * - non-existent hotels in each players stocks are filled with 0s
     * - the tiles which make up each hotel are added to the board
     * - remove player tiles, board tiles, and hotel tiles from the draw pile
     * - non-existent hotels in the bank are filled with 0s
     * - tile states are updated if they are in a players hand, on the board, or a part of a hotel
     */
    fun custom(tiles: Collection<Tile>,
               bank: Bank,
               board: Board,
               hotels: Collection<Hotel>,
               players: Collection<Player>): AcqObj {

        /* Hotel */
        val hotelIds = hotels.map { h -> h.id() }
        val hotelTiles = hotels.map { h -> h.state() }
                               .filter { s -> s is HotelState.OnBoard }
                               .map { s -> s as HotelState.OnBoard }
                               .flatMap { onBoard -> onBoard.tiles }

        // confirm hotels are all made up of unique tiles
        require(areUniqueTiles(hotelTiles), { "All tiles apart of each hotel must be unique" })

        /* Player */
        val playerHandTiles = players.flatMap { p -> p.tiles() }
        val playerHotelsWithStock = playerHotelsWithStock(players)

        // confirm players all have unique tiles in their hands
        require(areUniqueTiles(playerHandTiles), { "All tiles in each players hand must be unique" })
        // confirm players and hotels don't share any tiles
        require(!hotelTiles.any { t -> playerHandTiles.contains(t) }, { "Cannot share tiles which make up hotels and are in players hands" })

        // confirm players don't have stock in a hotel that doesn't exist
        require(hotelIds.containsAll(playerHotelsWithStock.keys), { "Players cannot have stock in a hotel that doesn't exist" })
        // confirm players have a legal amount of stocks
        require(areLegalStockAmounts(playerHotelsWithStock, bank.totalStocksPerHotel()), { "Players must have a legal amount of stocks" })

        // fill players absent hotel stocks with 0s
        val players = playersFillAbsentStocks(players, hotels)

        /* Board */
        // confirm board doesn't have any tiles that a player has
        require(!board.tiles().any { t -> playerHandTiles.contains(t) }, { "The board cannot have any tiles that a player has" })
        // confirm board doesn't have any tiles that a hotel has
        require(!board.tiles().any { t -> hotelTiles.contains(t) }, { "The board cannot have any tiles that a hotel has" })
        // add any hotel tiles to board
        var board = boardAddTiles(board, hotelTiles)


        /* Bank */
        // confirm bank doesn't have stock in a hotel that doesn't exist
        require(hotelIds.containsAll(bank.stocks().keys), { "The bank cannot have stock in a hotel that doesn't exist" })

        // remove player hand tiles from the draw pile
        var bank = bankRemoveTilesFromDrawPile(bank, playerHandTiles)
        // remove board tiles from the draw pile
        bank = bankRemoveTilesFromDrawPile(bank, board.tiles())
        // remove hotel tiles from the draw pile
        bank = bankRemoveTilesFromDrawPile(bank, hotelTiles)

        // fill bank absent hotel stocks with 0s
        bank = bankFillAbsentStocks(bank, hotels)

        /* Tiles */
        // update tile state to reflect player hands
        var tiles = tilesSetPlayerHandState(tiles, players)
        // update tile state to reflect on board
        tiles = tilesSetOnBoard(tiles, board)
        // update tile state to reflect hotel
        tiles = tilesSetHotelState(tiles, hotels)

        return AcqObj(bank, board, players, tiles, hotels)
    }
}

/* Whether the specified tiles are all unique. */
private fun areUniqueTiles(tiles: List<TileId>): Boolean {
    val uniqueHandTiles = tiles.distinct()
    if (tiles.size != uniqueHandTiles.size) {
        val duplicateTiles = tiles - uniqueHandTiles
        AcqObjs.KLog.logger.warn { "Hand tiles between all players are not unique. Duplicate tiles: $duplicateTiles" }
        return false
    }
    return true
}

/* The hotels and their stock amounts currently with each player. */
private fun playerHotelsWithStock(players: Collection<Player>): Map<HotelId, Int> {
    val allStocksOfHotel = { hotelId: HotelId -> players.mapNotNull { p -> p.stocks()[hotelId] }.sum() }
    val allHotelsWithStock = players.flatMap { p -> p.stocks().keys }.distinct()
    return allHotelsWithStock.associateBy({ h -> h }, { h -> allStocksOfHotel(h) })
}

/* Whether the specified amounts of stock in each hotel is legal to the specified hotel stock size. */
private fun areLegalStockAmounts(hotelsWithStock: Map<HotelId, Int>, hotelStocksSize: Int): Boolean {
    val hotelsIllegalAmount = hotelsWithStock.entries.filter { (_, s) -> s > hotelStocksSize }
    if (!hotelsIllegalAmount.isEmpty()) {
        AcqObjs.KLog.logger.warn { "Illegal amounts of stock in hotels. Expected [$hotelStocksSize] found: $hotelsIllegalAmount" }
        return false
    }
    return true
}

/* Fill the missing hotel stocks for each player with 0s. */
private fun playersFillAbsentStocks(players: Collection<Player>, hotels: Collection<Hotel>): Collection<Player> =
        players.map { player ->
            hotels.fold(player) { p, h ->
                if (p.stocks().containsKey(h.id())) p
                else Players.custom(p.id(),
                                    p.handLimit(),
                                    p.stockTurnLimit(),
                                    p.money(),
                                    p.tiles(),
                                    p.stocks() + Pair(h.id(), 0))
            }
        }

/* Fill the missing hotel stocks for the bank with 0s. */
private fun bankFillAbsentStocks(bank: Bank, hotels: Collection<Hotel>): Bank =
        hotels.fold(bank) { b, h ->
            if (b.stocks().containsKey(h.id())) b
            else Banks.custom(b.totalStocksPerHotel(), bank.shuffleSeed(), b.drawPile(), b.stocks() + Pair(h.id(), 0))
        }

/* Remove the specified tiles from the Bank's draw pile. */
private fun bankRemoveTilesFromDrawPile(bank: Bank, tiles: Collection<TileId>): Bank =
        Banks.custom(bank.totalStocksPerHotel(), bank.shuffleSeed(), bank.drawPile() - tiles, bank.stocks())

/* Add all the tiles to the board. */
private fun boardAddTiles(board: Board, tiles: Collection<TileId>): Board =
        tiles.fold(board){ newBoard, tile -> newBoard.addTile(tile)}

/* Set the state of the tiles in each players hands. */
private fun tilesSetPlayerHandState(tiles: Collection<Tile>, players: Collection<Player>): Collection<Tile> {
    val playerToTiles: Map<Player, Collection<TileId>> = players.associateBy({ p -> p }, { p -> p.tiles() })
    if (playerToTiles.isEmpty()) return tiles

    val newTiles = tiles.toMutableList()
    playerToTiles.entries.forEach { (player, tileIds) ->
        tileIds.forEach { tileId ->
            val tile = tiles.find { t -> t.id() == tileId }!!
            newTiles.remove(tile)
            newTiles.add(tile.withState(TileState.PlayerHand(player.id())))
        }
    }
    return newTiles.toList()
}

/* Set the state of the tiles a part of each hotel. */
private fun tilesSetHotelState(tiles: Collection<Tile>, hotels: Collection<Hotel>): Collection<Tile> {
    val hotelToTiles = hotels.filter { h -> h.isOnBoard() }
                             .associateBy({ h -> h }, { h -> (h.state() as HotelState.OnBoard).tiles })
    if (hotelToTiles.isEmpty()) return tiles

    val newTiles = tiles.toMutableList()
    hotelToTiles.entries.forEach { (hotel, tileIds) ->
        tileIds.forEach { tileId ->
            val tile = tiles.find { t -> t.id() == tileId }!!
            newTiles.remove(tile)
            newTiles.add(tile.withState(TileState.OnBoardHotel(hotel.id())))
        }
    }
    return newTiles.toList()
}

/* Set the state of tiles on the board. */
private fun tilesSetOnBoard(tiles: Collection<Tile>, board: Board): List<Tile> {
    val newTiles = tiles.toMutableList()
    board.tiles().forEach { tileId ->
        val tile = tiles.find { t -> t.id() == tileId }!!
        newTiles.remove(tile)
        newTiles.add(tile.withState(TileState.OnBoard))
    }
    return newTiles.toList()
}