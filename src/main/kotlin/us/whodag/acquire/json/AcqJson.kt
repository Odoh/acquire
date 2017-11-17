package us.whodag.acquire.json

import com.eclipsesource.json.Json
import com.eclipsesource.json.JsonArray
import com.eclipsesource.json.JsonObject
import com.eclipsesource.json.JsonValue
import us.whodag.acquire.*
import us.whodag.acquire.acq.AcqType
import us.whodag.acquire.acq.AcqVersion
import us.whodag.acquire.acq.AcquireId
import us.whodag.acquire.obj.hotel.HotelId
import us.whodag.acquire.obj.player.PlayerId
import us.whodag.acquire.obj.tile.TileId
import us.whodag.acquire.req.Response
import us.whodag.acquire.req.isSuccess

/*
 * Functions to work with JSON and common Acquire classes.
 */

/* JSON Object Fields. */
private val ACQ_VER_MAJOR = "major"
private val ACQ_VER_MINOR = "minor"
private val RESPONSE_SUCCESS = "success"
private val RESPONSE_MESSAGE = "message"
private val STATE_TURN = "turn"
private val STATE_REQUEST = "request"
private val STATE_RESPONSE = "response"
private val STATE_SM = "sm"
private val STATE_OBJS = "objs"
private val ACQ_ID = "id"
private val ACQ_VERSION = "version"
private val ACQ_TYPE = "type"
private val ACQ_TURN = "turn"
private val ACQ_STATE = "state"

/* JSON Serialization. */
fun AcquireId.json(): String = name
fun PlayerId.json(): String = name
fun HotelId.json(): String = name
fun TileId.json(): String = name

fun Collection<PlayerId>.playerIdsJson(): JsonArray = fold(JsonArray()) { array, playerId -> array.add(playerId.json()) }
fun Collection<HotelId>.hotelIdsJson(): JsonArray = fold(JsonArray()) { array, hotelId -> array.add(hotelId.json()) }
fun Collection<TileId>.tileIdsJson(): JsonArray = fold(JsonArray()) { array, tileId -> array.add(tileId.json()) }

fun AcqVersion.json(): JsonObject = JsonObject().add(ACQ_VER_MAJOR, major)
                                                .add(ACQ_VER_MINOR, minor)
fun AcqType.json(): JsonValue =
        when (this) {
            AcqType.Standard -> Json.value("standard")
            AcqType.Custom -> Json.value("custom")
        }

fun Response.json(): JsonValue =
        JsonObject().add(RESPONSE_SUCCESS, isSuccess())
                    .add(RESPONSE_MESSAGE, message)

fun Map<HotelId, Int>.stockJson(): JsonObject =
        entries.fold(JsonObject()) { obj, (hotel, amount) -> obj.add(hotel.json(), amount) }

fun State.stateJson(): JsonObject =
        JsonObject().add(STATE_TURN, turn)
                    .add(STATE_REQUEST, acqReq.json())
                    .add(STATE_RESPONSE, response.json())
                    .add(STATE_SM, acqSmState.json())
                    .add(STATE_OBJS, acqSmState.acqObj.json())

fun List<State>.statesJson(): JsonArray =
        fold(JsonArray()) { array, state -> array.add(state.stateJson()) }

fun Acquire.json(): JsonValue =
        JsonObject().add(ACQ_ID, id().json())
                    .add(ACQ_VERSION, version().json())
                    .add(ACQ_TYPE, type().json())
                    .add(ACQ_TURN, turn())
                    .add(ACQ_STATE, state().stateJson())

/* JSON Deserialization. */
fun JsonValue.toAcquireId(): AcquireId = AcquireId(toStr())
fun JsonValue.toPlayerId(): PlayerId = PlayerId(toStr())
fun JsonValue.toHotelId(): HotelId = HotelId(toStr())
fun JsonValue.toTileId(): TileId = TileId(toStr())

fun JsonValue.toPlayerIds(): List<PlayerId> = toArray().fold(emptyList<PlayerId>()) { list, req -> list + req.toPlayerId() }

fun JsonValue.toAcqVersion(): AcqVersion {
    val o = toObject()
    val major = o.field(ACQ_VER_MAJOR).toInt()
    val minor = o.field(ACQ_VER_MINOR).toInt()
    return AcqVersion(major, minor)
}

fun JsonValue.toAcqType(): AcqType =
        when (toStr()) {
            "standard" -> AcqType.Standard
            "custom" -> AcqType.Custom
            else -> throw JsonException("Unknown Acquire type: ${toStr()}")
        }

fun JsonValue.toStocks(): Map<HotelId, Int> =
        toObject().fold(emptyMap()) { map, entry ->
            val hotel = Json.value(entry.name).toHotelId()
            val amount = entry.value.toInt()
            map + Pair(hotel, amount)
        }
