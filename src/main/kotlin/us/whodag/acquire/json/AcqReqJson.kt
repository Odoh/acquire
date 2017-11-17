package us.whodag.acquire.json

import com.eclipsesource.json.JsonArray
import com.eclipsesource.json.JsonObject
import com.eclipsesource.json.JsonValue
import us.whodag.acquire.req.*

/*
 * Functions to work with JSON and AcqReqs.
 */

/* JSON Object Fields. */
private val REQ_TYPE = "type"
private val REQ_PLAYER = "player"
private val REQ_TILE = "tile"
private val REQ_HOTEL = "hotel"
private val REQ_STOCKS = "stocks"
private val REQ_TRADE = "trade"
private val REQ_SELL = "sell"
private val REQ_KEEP = "keep"

/* JSON Serialization. */
fun AcceptMoneyReq.json(): JsonObject = JsonObject().add(REQ_TYPE, "accept_money")
                                                    .add(REQ_PLAYER, player.json())
fun AcceptStockReq.json(): JsonObject = JsonObject().add(REQ_TYPE, "accept_stock")
                                                    .add(REQ_PLAYER, player.json())
fun AcceptUndoReq.json(): JsonObject = JsonObject().add(REQ_TYPE, "accept_undo")
                                                    .add(REQ_PLAYER, player.json())
fun BuyStockReq.json(): JsonObject = JsonObject().add(REQ_TYPE, "buy_stock")
                                                 .add(REQ_PLAYER, player.json())
                                                 .add(REQ_STOCKS, buyStocks.stockJson())
fun ChooseHotelReq.json(): JsonObject = JsonObject().add(REQ_TYPE, "choose_hotel")
                                                    .add(REQ_PLAYER, player.json()).add(REQ_HOTEL, hotel.json())
fun DrawTileReq.json(): JsonObject = JsonObject().add(REQ_TYPE, "draw_tile")
                                                 .add(REQ_PLAYER, player.json())
fun EndGameReq.json(): JsonObject = JsonObject().add(REQ_TYPE, "end_game")
                                                .add(REQ_PLAYER, player.json())
fun HandleStocksReq.json(): JsonObject = JsonObject().add(REQ_TYPE, "handle_stocks")
                                                     .add(REQ_PLAYER, player.json())
                                                     .add(REQ_TRADE, trade)
                                                     .add(REQ_SELL, sell)
                                                     .add(REQ_KEEP, keep)
fun PlaceTileReq.json(): JsonObject = JsonObject().add(REQ_TYPE, "place_tile")
                                                  .add(REQ_PLAYER, player.json())
                                                  .add(REQ_TILE, tile.json())
fun StartGameReq.json(): JsonObject = JsonObject().add(REQ_TYPE, "start_game")
                                                  .add(REQ_PLAYER, player.json())
fun UndoReq.json(): JsonObject = JsonObject().add(REQ_TYPE, "undo")
                                             .add(REQ_PLAYER, player.json())

fun AcqReq.json(): JsonObject =
        when (this) {
            is AcceptMoneyReq -> json()
            is AcceptStockReq -> json()
            is AcceptUndoReq -> json()
            is BuyStockReq -> json()
            is ChooseHotelReq -> json()
            is DrawTileReq -> json()
            is EndGameReq -> json()
            is HandleStocksReq -> json()
            is PlaceTileReq -> json()
            is StartGameReq -> json()
            is UndoReq -> json()
        }

fun Collection<AcqReq>.acqReqsJson(): JsonArray = fold(JsonArray()) { array, acqReq -> array.add(acqReq.json()) }

/* JSON Deserialization. */
fun JsonValue.toAcceptMoneyReq(): AcceptMoneyReq = AcceptMoneyReq(toObject().field(REQ_PLAYER).toPlayerId())
fun JsonValue.toAcceptStockReq(): AcceptStockReq = AcceptStockReq(toObject().field(REQ_PLAYER).toPlayerId())
fun JsonValue.toAcceptUndoReq(): AcceptUndoReq = AcceptUndoReq(toObject().field(REQ_PLAYER).toPlayerId())

fun JsonValue.toBuyStockReq(): BuyStockReq {
    val o = toObject()
    val player = o.field(REQ_PLAYER).toPlayerId()
    val stocks = o.field(REQ_STOCKS).toStocks()
    return BuyStockReq(player, stocks)
}

fun JsonValue.toChooseHotelReq(): ChooseHotelReq {
    val o = toObject()
    val player = o.field(REQ_PLAYER).toPlayerId()
    val hotel = o.field(REQ_HOTEL).toHotelId()
    return ChooseHotelReq(player, hotel)
}

fun JsonValue.toDrawTileReq(): DrawTileReq = DrawTileReq(toObject().field(REQ_PLAYER).toPlayerId())
fun JsonValue.toEndGameReq(): EndGameReq = EndGameReq(toObject().field(REQ_PLAYER).toPlayerId())

fun JsonValue.toHandleStocksReq(): HandleStocksReq {
    val o = toObject()
    val player = o.field(REQ_PLAYER).toPlayerId()
    val trade = o.field(REQ_TRADE).toInt()
    val sell = o.field(REQ_SELL).toInt()
    val keep = o.field(REQ_KEEP).toInt()
    return HandleStocksReq(player, trade, sell, keep)
}

fun JsonValue.toPlaceTileReq(): PlaceTileReq {
    val o = toObject()
    val player = o.field(REQ_PLAYER).toPlayerId()
    val tile = o.field(REQ_TILE).toTileId()
    return PlaceTileReq(player, tile)
}

fun JsonValue.toStartGameReq(): StartGameReq = StartGameReq(toObject().field(REQ_PLAYER).toPlayerId())
fun JsonValue.toUndoReq(): UndoReq = UndoReq(toObject().field(REQ_PLAYER).toPlayerId())

fun JsonValue.toAcqReq(): AcqReq {
    val o = toObject()
    val type = o.field(REQ_TYPE).toStr()
    return when (type) {
        "accept_money" -> o.toAcceptMoneyReq()
        "accept_stock" -> o.toAcceptStockReq()
        "accept_undo" -> o.toAcceptUndoReq()
        "buy_stock" -> o.toBuyStockReq()
        "choose_hotel" -> o.toChooseHotelReq()
        "draw_tile" -> o.toDrawTileReq()
        "end_game" -> o.toEndGameReq()
        "handle_stocks" -> o.toHandleStocksReq()
        "place_tile" -> o.toPlaceTileReq()
        "start_game" -> o.toStartGameReq()
        "undo" -> o.toUndoReq()
        else -> throw JsonException("Unknown request type: $type")
    }
}

fun JsonValue.toAcqReqs(): List<AcqReq> = toArray().fold(emptyList<AcqReq>()) { list, req -> list + req.toAcqReq() }
