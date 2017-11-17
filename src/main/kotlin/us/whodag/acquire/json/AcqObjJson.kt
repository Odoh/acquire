package us.whodag.acquire.json

import com.eclipsesource.json.JsonArray
import com.eclipsesource.json.JsonObject
import us.whodag.acquire.obj.AcqObj
import us.whodag.acquire.obj.bank.Bank
import us.whodag.acquire.obj.board.Board
import us.whodag.acquire.obj.hotel.Hotel
import us.whodag.acquire.obj.hotel.HotelState
import us.whodag.acquire.obj.player.Player
import us.whodag.acquire.obj.tile.Tile
import us.whodag.acquire.obj.tile.TileState

/*
 * Functions to work with JSON and AcqObjs.
 */

/* JSON Object Fields. */
private val STATE_TYPE = "type"
private val HOTEL_STATE_TILES = "tiles"
private val TILE_STATE_PLAYER = "player"
private val TILE_STATE_HOTEL = "hotel"

private val OBJ_ID = "id"
private val BANk_DRAW_PILE_SIZE = "draw_pile_size"
private val BANK_STOCKS = "stocks"
private val BOARD_TILES = "tiles"
private val HOTEL_STATE = "state"
private val HOTEL_STOCK_PRICE = "stock_price"
private val HOTEL_MAJORITY_BONUS = "majority_bonus"
private val HOTEL_MINORITY_BONUS = "minority_bonus"
private val HOTEL_IS_SAFE = "is_safe"
private val HOTEL_IS_END_GAME_SIZE = "is_end_game_size"
private val PLAYER_MONEY = "money"
private val PLAYER_TILES = "tiles"
private val PLAYER_STOCKS = "stocks"
private val TILE_STATE = "state"

private val ACQ_BANK = "bank"
private val ACQ_BOARD = "board"
private val ACQ_PLAYERS = "players"
private val ACQ_TILES = "tiles"
private val ACQ_HOTELS = "hotels"

/* JSON Serialization. */
fun HotelState.json(): JsonObject =
        when (this) {
            HotelState.Available -> JsonObject().add(STATE_TYPE, "available")
            is HotelState.OnBoard -> JsonObject().add(STATE_TYPE, "on_board")
                                                 .add(HOTEL_STATE_TILES, tiles.tileIdsJson())
        }
fun TileState.json(): JsonObject =
        when (this) {
            TileState.DrawPile -> JsonObject().add(STATE_TYPE, "draw_pile")
            is TileState.PlayerHand -> JsonObject().add(STATE_TYPE, "player_hand")
                                                   .add(TILE_STATE_PLAYER, player.json())
            TileState.OnBoard -> JsonObject().add(STATE_TYPE, "on_board")
            is TileState.OnBoardHotel -> JsonObject().add(STATE_TYPE, "on_board_hotel")
                                                     .add(TILE_STATE_HOTEL, hotel.json())
            TileState.Discarded -> JsonObject().add(STATE_TYPE, "discarded")
        }

fun Bank.json(): JsonObject = JsonObject().add(BANk_DRAW_PILE_SIZE, drawPile().size)
                                          .add(BANK_STOCKS, stocks().stockJson())
fun Board.json(): JsonObject = JsonObject().add(BOARD_TILES, tiles().tileIdsJson())
fun Hotel.json(): JsonObject = JsonObject().add(OBJ_ID, id().json())
                                           .add(HOTEL_STATE, state().json())
                                           .add(HOTEL_STOCK_PRICE, stockPrice())
                                           .add(HOTEL_MAJORITY_BONUS, majorityBonus())
                                           .add(HOTEL_MINORITY_BONUS, minorityBonus())
                                           .add(HOTEL_IS_SAFE, isSafe())
                                           .add(HOTEL_IS_END_GAME_SIZE, isEndGameSize())
fun Player.json(): JsonObject = JsonObject().add(OBJ_ID, id().json())
                                            .add(PLAYER_MONEY, money())
                                            .add(PLAYER_TILES, tiles().tileIdsJson())
                                            .add(PLAYER_STOCKS, stocks().stockJson())
fun Tile.json(): JsonObject = JsonObject().add(OBJ_ID, id().json())
                                          .add(TILE_STATE, state().json())

fun Collection<Player>.playersJson(): JsonObject = fold(JsonObject()) { obj, player -> obj.add(player.id().json(), player.json()) }
fun Collection<Hotel>.hotelsJson(): JsonObject = fold(JsonObject()) { obj, hotel -> obj.add(hotel.id().json(), hotel.json()) }
fun Collection<Tile>.tilesJson(): JsonObject = fold(JsonObject()) { obj, tile -> obj.add(tile.id().json(), tile.json()) }

fun AcqObj.json(): JsonObject = JsonObject().add(ACQ_BANK, bank.json())
                                            .add(ACQ_BOARD, board.json())
                                            .add(ACQ_PLAYERS, players.values.playersJson())
                                            .add(ACQ_TILES, tiles.values.tilesJson())
                                            .add(ACQ_HOTELS, hotels.values.hotelsJson())