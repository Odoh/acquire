package us.whodag.acquire.obj

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

/**
 * Holds the state of all physical objects in Acquire.
 *
 * @property bank the bank.
 * @property board the game board.
 * @property players the players in the game and their state.
 * @property tiles the tiles in the game and their state.
 * @property hotels the hotels in the game and their state.
 */
data class AcqObj(val bank: Bank,
                  val board: Board,
                  val players: Map<PlayerId, Player>,
                  val tiles: Map<TileId, Tile>,
                  val hotels: Map<HotelId, Hotel>) {
    constructor(bank: Bank,
                board: Board,
                players: Collection<Player>,
                tiles: Collection<Tile>,
                hotels: Collection<Hotel>) : this(bank,
                                                  board,
                                                  players.associateBy { p -> p.id() },
                                                  tiles.associateBy { t -> t.id() },
                                                  hotels.associateBy { h -> h.id() })
}

/** The player state for the given player id. */
fun AcqObj.player(player: PlayerId): Player {
    require(players.containsKey(player), { "Player map must contain player" })
    return players[player]!!
}

/** The tile state for the given tile id. */
fun AcqObj.tile(tile: TileId): Tile {
    require(tiles.containsKey(tile), { "Tile map must contain tile" })
    return tiles[tile]!!
}

/** The hotel state for the given hotel id. */
fun AcqObj.hotel(hotel: HotelId): Hotel {
    require(hotels.containsKey(hotel), { "Hotel map must contain hotel" })
    return hotels[hotel]!!
}

/** The hotels which are available to start. */
fun AcqObj.availableHotels(): List<HotelId> = hotels.entries.filter { (_, hotel) -> hotel.isAvailable() }
                                                            .map { (id, _) -> id }

/** The hotels which are on the board. */
fun AcqObj.hotelsOnBoard(): List<HotelId> = hotels.entries.filter { (_, hotel) -> !hotel.isAvailable() }
                                                          .map { (id, _) -> id }

/** Filter hotels returning only those with the largest size. */
fun AcqObj.largestSizeHotels(hotels: List<HotelId>): List<HotelId> {
    require(hotels.isNotEmpty(), { "Need hotels to choose the largest size from" })
    val maxHotelSize = hotels.map { h -> hotel(h).size() }.max()!!
    return hotels.filter { h -> hotel(h).size() == maxHotelSize }
}

/** The worth of all player's assets. */
fun AcqObj.assetWorth(player: PlayerId): Int {
    val acqPlayer = player(player)
    return hotelsOnBoard().fold(0) { total, hotel ->
        val bonusMoney = stockBonuses(hotel)[player] ?: 0
        val stockMoney = acqPlayer.stock(hotel) * hotel(hotel).stockPrice()
        total + bonusMoney + stockMoney
    }
}

/** The players with stock of the specified hotel. */
fun AcqObj.playersWithStock(hotel: HotelId): List<PlayerId> =
        players.entries.filter { (_, player) -> player.hasStock(hotel) }
                       .map { (id, _) -> id }

/** The players with the most stock of hotel, including only the specified players. */
fun AcqObj.playersWithMostStock(players: Collection<PlayerId>, hotel: HotelId): List<PlayerId> {
    val playersWithStock = playersWithStock(hotel).filter { player -> players.contains(player) }
    if (playersWithStock.isEmpty()) return emptyList()

    val maxStock = playersWithStock.map { p -> player(p).stocks()[hotel]!! }.max()!!
    return playersWithStock.filter { p -> player(p).stock(hotel) == maxStock }
}

/** Whether the game can be ended. */
fun AcqObj.canEndGame(): Boolean {
    val hotelsOnBoard = hotelsOnBoard()

    // there are hotels on the board
    if (hotelsOnBoard.isEmpty()) return false

    // there exists a hotel which can end the game or all the hotels on the board are safe
    return hotelsOnBoard.any { h -> hotel(h).isEndGameSize() } ||
            hotelsOnBoard.all { h -> hotel(h).isSafe() }
}

/** Whether player can buy stocks. */
fun AcqObj.canBuyStock(player: PlayerId): Boolean {
    val hotelsOnBoard: List<HotelId> = hotels.entries.filter { (_, hotel) -> hotel.isOnBoard() }.map { (id, _) -> id }
    val hotelsOnBoardWithStocks: List<HotelId> = hotelsOnBoard.filter { h -> bank.hasStock(h) }
    val minMoneyRequired = hotelsOnBoardWithStocks.map { h -> hotel(h).stockPrice() }.min()

    // a hotel needs to be on the board
    if (hotelsOnBoard.isEmpty()) return false

    // the hotels on the board need to have available stocks
    if (hotelsOnBoardWithStocks.isEmpty()) return false

    // the player needs at least enough money to buy a stock of those available
    if (player(player).money() < minMoneyRequired!!) return false

    return true
}

/** The players and what bonuses they're paid for their stocks in the specified hotel. */
fun AcqObj.stockBonuses(hotel: HotelId): Map<PlayerId, Int> {
    val majorityPlayers = playersWithMostStock(players.keys, hotel)
    val minorityPlayers = playersWithMostStock(players.keys - majorityPlayers, hotel)
    require(majorityPlayers.isNotEmpty(), { "There must be at least one player to receive the stock bonus" })

    if (majorityPlayers.size > 1) {
        // pay combined bonuses to all majority players
        val combinedBonus = hotel(hotel).majorityBonus() + hotel(hotel).minorityBonus()
        val bonusPerPlayer = combinedBonus / majorityPlayers.size
        return majorityPlayers.associateBy({ p -> p }, { _ -> bonusPerPlayer })
    }
    val majorityPlayer = majorityPlayers[0]

    if (minorityPlayers.isEmpty()) {
        // pay combined bonuses to the majority player
        val combinedBonus = hotel(hotel).majorityBonus() + hotel(hotel).minorityBonus()
        return hashMapOf(majorityPlayer to combinedBonus)
    }

    if (minorityPlayers.size > 1) {
        // pay minority bonus to all minority players
        val minorityBonusPerPlayer = hotel(hotel).minorityBonus() / minorityPlayers.size
        return minorityPlayers.associateBy({ p -> p }, { _ -> minorityBonusPerPlayer }) +
               Pair(majorityPlayer, hotel(hotel).majorityBonus())
    }
    val minorityPlayer = minorityPlayers[0]

    // pay majority and minority bonuses to the respective players
    return hashMapOf(majorityPlayer to hotel(hotel).majorityBonus(),
                     minorityPlayer to hotel(hotel).minorityBonus())
}

/** Player draws a tile returning an updated AcqObj and the tile drawn. */
fun AcqObj.drawTile(player: PlayerId): Pair<AcqObj, TileId> {
    val (nextBank, drawnTile) = bank.drawTile()
    val nextPlayer = player(player).addTile(drawnTile)
    val nextTile = tile(drawnTile).withState(TileState.PlayerHand(player))
    val nextState = copy(bank = nextBank,
                         players = players + Pair(player, nextPlayer),
                         tiles = tiles + Pair(drawnTile, nextTile))
    return Pair(nextState, drawnTile)
}

/** Player places tile from their hand onto the board. */
fun AcqObj.placeTile(player: PlayerId, tile: TileId): AcqObj {
    val nextPlayer = player(player).removeTile(tile)
    val nextTile = tile(tile).withState(TileState.OnBoard)
    val nextBoard = board.addTile(tile)
    return copy(players = players + Pair(player, nextPlayer),
                tiles = tiles + Pair(tile, nextTile),
                board = nextBoard)
}

/** Player places tile from their hand onto the board which adds to hotel. */
fun AcqObj.placeTile(player: PlayerId, tile: TileId, hotel: HotelId): AcqObj {
    val acqObj = placeTile(player, tile)
    val nextTiles = acqObj.board.connectedTiles(tile).map { t -> acqObj.tile(t).withState(TileState.OnBoardHotel(hotel)) }
    val nextHotel = hotel(hotel).withState(HotelState.OnBoard(nextTiles.map { t -> t.id() }))
    return acqObj.copy(tiles = acqObj.tiles + nextTiles.map { t -> Pair(t.id(), t) },
                       hotels = acqObj.hotels + Pair(hotel, nextHotel))
}

/** Player discards tile from their hand. */
fun AcqObj.discardTile(player: PlayerId, tile: TileId): AcqObj {
    val nextPlayer = player(player).removeTile(tile)
    val nextTile = tile(tile).withState(TileState.Discarded)
    return copy(players = players + Pair(player, nextPlayer),
                tiles = tiles + Pair(tile, nextTile))
}

/** Start hotel to be made up of hotelTiles. */
fun AcqObj.startHotel(hotel: HotelId, hotelTiles: Collection<TileId>): AcqObj {
    val nextHotel = hotel(hotel).withState(HotelState.OnBoard(hotelTiles))
    val nextTiles = hotelTiles.map { t -> tile(t).withState(TileState.OnBoardHotel(hotel)) }
    return copy(hotels = hotels + Pair(hotel, nextHotel),
                tiles = tiles + nextTiles.map { t -> Pair(t.id(), t) })
}

/** Player withdraws the specified amount of stock of hotel from the bank. */
fun AcqObj.withdrawStock(player: PlayerId, hotel: HotelId, amount: Int): AcqObj {
    val nextBank = bank.removeStock(hotel, amount)
    val nextPlayer = player(player).addStock(hotel, amount)
    return copy(bank = nextBank,
                players = players + Pair(player, nextPlayer))
}

/** Player deposits the specified amount of stock of hotel into the bank. */
fun AcqObj.depositStock(player: PlayerId, hotel: HotelId, amount: Int): AcqObj {
    val nextBank = bank.addStock(hotel, amount)
    val nextPlayer = player(player).removeStock(hotel, amount)
    return copy(bank = nextBank,
                players = players + Pair(player, nextPlayer))
}

/** Player gains the specified amount of money. */
fun AcqObj.addMoney(player: PlayerId, amount: Int): AcqObj {
    val nextPlayer = player(player).addMoney(amount)
    return copy(players = players + Pair(player, nextPlayer))
}

/** Player sells the specified amount of stocks of hotel. */
fun AcqObj.sellStocks(player: PlayerId, hotel: HotelId, amount: Int): AcqObj {
    val moneyProfit = amount * hotel(hotel).stockPrice()
    val nextPlayer = player(player).removeStock(hotel, amount).addMoney(moneyProfit)
    val nextBank = bank.addStock(hotel, amount)
    return copy(bank = nextBank,
                players = players + Pair(player, nextPlayer))
}

/** Player buys the specified amount of stocks of hotel. */
fun AcqObj.buyStocks(player: PlayerId, hotel: HotelId, amount: Int): AcqObj {
    val moneyCost = amount * hotel(hotel).stockPrice()
    val nextPlayer = player(player).addStock(hotel, amount).removeMoney(moneyCost)
    val nextBank = bank.removeStock(hotel, amount)
    return copy(bank = nextBank,
                players = players + Pair(player, nextPlayer))
}

/** Player buys the stocks specified in the buyStocks map. */
fun AcqObj.buyStocks(player: PlayerId, buyStocks: Map<HotelId, Int>): AcqObj {
    val entries = buyStocks.filter { e -> e.value > 0 }.entries
    return entries.fold(this, { accState, (hotel, amount) -> accState.buyStocks(player, hotel, amount) })
}