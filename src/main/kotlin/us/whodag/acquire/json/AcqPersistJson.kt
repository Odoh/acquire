package us.whodag.acquire.json

import com.eclipsesource.json.JsonObject
import com.eclipsesource.json.JsonValue
import us.whodag.acquire.req.AcqReq
import us.whodag.acquire.Acquire
import us.whodag.acquire.acq.AcqType
import us.whodag.acquire.acq.AcqVersion
import us.whodag.acquire.acq.AcquireId
import us.whodag.acquire.obj.player.PlayerId


/**
 * Information required to persist a standard Acquire game.
 *
 * @property id the identity of the Acquire game.
 * @property version the version of the Acquire game.
 * @property type the type of the Acquire game (standard).
 * @property shuffleSeed the seed used for shuffling the draw pile.
 * @property players the players in the game.
 * @property requests the list of requests that have been performed.
 */
data class StandardAcquirePersist(val id: AcquireId,
                                  val version: AcqVersion,
                                  val type: AcqType,
                                  val shuffleSeed: Long,
                                  val players: List<PlayerId>,
                                  val requests: List<AcqReq>) {
    constructor(acquire: Acquire) : this(acquire.id(),
                                         acquire.version(),
                                         if (acquire.type() == AcqType.Standard) AcqType.Standard else throw IllegalArgumentException("Acquire type [${acquire.type()}] must be: ${AcqType.Standard}"),
                                         acquire.state().acqSmState.acqObj.bank.shuffleSeed(),
                                         acquire.state().acqSmState.acqObj.players.keys.sorted(),
                                         acquire.states().map { it.acqReq })
}

/* JSON Object Fields. */
private val PERSIST_ID = "name"
private val PERSIST_VERSION = "version"
private val PERSIST_TYPE = "type"
private val PERSIST_SHUFFLE_SEED = "shuffle_seed"
private val PERSIST_PLAYERS = "players"
private val PERSIST_REQUESTS = "requests"

/* JSON Serialization. */
fun StandardAcquirePersist.json(): JsonObject =
        JsonObject().add(PERSIST_ID, id.json())
                    .add(PERSIST_VERSION, version.json())
                    .add(PERSIST_TYPE, type.json())
                    .add(PERSIST_SHUFFLE_SEED, shuffleSeed)
                    .add(PERSIST_PLAYERS, players.playerIdsJson())
                    .add(PERSIST_REQUESTS, requests.acqReqsJson())

/* JSON Deserialization. */
fun JsonValue.toStandardAcquirePersist(): StandardAcquirePersist {
    val o = toObject()
    val id = o.field(PERSIST_ID).toAcquireId()
    val version = o.field(PERSIST_VERSION).toAcqVersion()
    val type = o.field(PERSIST_TYPE).toAcqType()
    val shuffleSeed = o.field(PERSIST_SHUFFLE_SEED).toLong()
    val players = o.field(PERSIST_PLAYERS).toPlayerIds()
    val requests = o.field(PERSIST_REQUESTS).toAcqReqs()
    return StandardAcquirePersist(id, version, type, shuffleSeed, players, requests)
}
