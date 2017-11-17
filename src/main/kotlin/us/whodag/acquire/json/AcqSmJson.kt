package us.whodag.acquire.json

import com.eclipsesource.json.JsonArray
import com.eclipsesource.json.JsonObject
import us.whodag.acquire.obj.player.PlayerId
import us.whodag.acquire.obj.tile.TileId
import us.whodag.acquire.sm.*

/*
 * Functions to work with JSON and AcqSmStates.
 */

/* JSON Object Fields. */
private val PLAYER_TILE_PLAYER = "player"
private val PLAYER_TILE_TILE = "tile"
private val PLAYER_AMOUNT_PLAYER = "player"
private val PLAYER_AMOUNT_AMOUNT = "amount"

private val CURRENT_PLAYER = "current_player"
private val SM_STATE = "state"
private val SM_PLAYERS_DRAWN = "players_drawn"
private val PTT_PLAYERS_PLACED = "players_placed"
private val SH_TILES = "tiles"
private val FS_STARTED_HOTEL = "started_hotel"
private val EGP_PLAYERS_PAID = "players_paid"
private val GO_PLAYER_RESULTS = "player_results"
private val CSH_POTENTIAL_SURVIVING_HOTELS = "potential_surviving_hotels"
private val CDH_SURVIVING_HOTEL = "surviving_hotel"
private val CDH_POTENTIAL_NEXT_DEFUNCT_HOTELS = "potential_next_defunct_hotels"
private val CDH_REMAINING_HOTELS = "remaining_hotels"
private val PB_PLAYERS_TO_PAY = "players_to_pay"
private val PB_DEFUNCT_HOTEL = "defunct_hotel"
private val PB_SURVIVING_HOTEL = "surviving_hotel"
private val PB_REMAINING_HOTELS = "remaining_hotels"
private val HDHS_PLAYERS_WITH_STOCK = "players_with_stock"
private val HDHS_DEFUNCT_HOTEL = "defunct_hotel"
private val HDHS_SURVIVING_HOTEL = "surviving_hotel"
private val HDHS_REMAINING_HOTELS = "remaining_hotels"

/* JSON Serialization. */
fun Map<PlayerId, TileId>.playerTileJson(): JsonArray =
        entries.fold(JsonArray()) { array, (player, tile) ->
            array.add(JsonObject().add(PLAYER_TILE_PLAYER, player.json())
                                  .add(PLAYER_TILE_TILE, tile.json()))
        }
fun Map<PlayerId, Int>.playerAmountJson(): JsonArray =
        entries.fold(JsonArray()) { array, (player, amount) ->
            array.add(JsonObject().add(PLAYER_AMOUNT_PLAYER, player.json())
                                  .add(PLAYER_AMOUNT_AMOUNT, amount))
        }


fun DrawTurnTile.json(): JsonObject = JsonObject().add(SM_STATE, "draw_turn_tile")
                                                  .add(SM_PLAYERS_DRAWN, playersDrawn.playerIdsJson())
fun PlaceTurnTile.json(): JsonObject  = JsonObject().add(SM_STATE, "place_turn_tile")
                                                    .add(PTT_PLAYERS_PLACED, playersPlaced.playerTileJson())
fun DrawInitialTiles.json(): JsonObject = JsonObject().add(SM_STATE, "draw_initial_tiles")
                                                      .add(SM_PLAYERS_DRAWN, playersDrawn.playerIdsJson())
fun PlaceTile.json(): JsonObject = JsonObject().add(SM_STATE, "place_tile")
                                               .add(CURRENT_PLAYER, gameState.currentPlayer.json())
fun StartHotel.json(): JsonObject = JsonObject().add(SM_STATE, "start_hotel")
                                                .add(CURRENT_PLAYER, gameState.currentPlayer.json())
                                                .add(SH_TILES, tiles.tileIdsJson())
fun FoundersStock.json(): JsonObject = JsonObject().add(SM_STATE, "founders_stock")
                                                   .add(CURRENT_PLAYER, gameState.currentPlayer.json())
                                                   .add(FS_STARTED_HOTEL, startedHotel.json())
fun BuyStock.json(): JsonObject = JsonObject().add(SM_STATE, "buy_stock")
                                              .add(CURRENT_PLAYER, gameState.currentPlayer.json())
fun DrawTile.json(): JsonObject = JsonObject().add(SM_STATE, "draw_tile")
                                              .add(CURRENT_PLAYER, gameState.currentPlayer.json())
fun EndGamePayout.json(): JsonObject = JsonObject().add(SM_STATE, "end_game_payout")
                                                   .add(EGP_PLAYERS_PAID, playersPaid.playerIdsJson())
fun GameOver.json(): JsonObject = JsonObject().add(SM_STATE, "game_over")
                                              .add(GO_PLAYER_RESULTS, playerResults.playerIdsJson())
fun ChooseSurvivingHotel.json(): JsonObject = JsonObject().add(SM_STATE, "choose_surviving_hotel")
                                                          .add(CURRENT_PLAYER, mergeContext.gameState.currentPlayer.json())
                                                          .add(CSH_POTENTIAL_SURVIVING_HOTELS, potentialSurvivingHotels.hotelIdsJson())
fun ChooseDefunctHotel.json(): JsonObject = JsonObject().add(SM_STATE, "choose_defunct_hotel")
                                                        .add(CURRENT_PLAYER, mergeContext.gameState.currentPlayer.json())
                                                        .add(CDH_SURVIVING_HOTEL, survivingHotel.json())
                                                        .add(CDH_POTENTIAL_NEXT_DEFUNCT_HOTELS, potentialNextDefunctHotels.hotelIdsJson())
                                                        .add(CDH_REMAINING_HOTELS, remainingHotels.hotelIdsJson())
fun PayBonuses.json(): JsonObject = JsonObject().add(SM_STATE, "pay_bonuses")
                                                .add(CURRENT_PLAYER, mergeState.mergeContext.gameState.currentPlayer.json())
                                                .add(PB_PLAYERS_TO_PAY, playersToPay.playerAmountJson())
                                                .add(PB_DEFUNCT_HOTEL, mergeState.defunctHotel.json())
                                                .add(PB_SURVIVING_HOTEL, mergeState.survivingHotel.json())
                                                .add(PB_REMAINING_HOTELS, mergeState.remainingHotels.hotelIdsJson())
fun HandleDefunctHotelStocks.json(): JsonObject = JsonObject().add(SM_STATE, "handle_defunct_hotel_stocks")
                                                              .add(CURRENT_PLAYER, mergeState.mergeContext.gameState.currentPlayer.json())
                                                              .add(HDHS_PLAYERS_WITH_STOCK, playersWithStockInTurnOrder.playerIdsJson())
                                                              .add(HDHS_DEFUNCT_HOTEL, mergeState.defunctHotel.json())
                                                              .add(HDHS_SURVIVING_HOTEL, mergeState.survivingHotel.json())
                                                              .add(HDHS_REMAINING_HOTELS, mergeState.remainingHotels.hotelIdsJson())

fun AcqSmState.json(): JsonObject =
        when(this) {
            is DrawTurnTile -> json()
            is PlaceTurnTile -> json()
            is DrawInitialTiles -> json()
            is PlaceTile -> json()
            is StartHotel -> json()
            is FoundersStock -> json()
            is BuyStock -> json()
            is DrawTile -> json()
            is EndGamePayout -> json()
            is GameOver -> json()
            is ChooseSurvivingHotel -> json()
            is ChooseDefunctHotel -> json()
            is PayBonuses -> json()
            is HandleDefunctHotelStocks -> json()
        }
