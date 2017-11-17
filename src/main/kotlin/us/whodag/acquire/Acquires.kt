package us.whodag.acquire

import mu.KLogging
import us.whodag.acquire.acq.*
import us.whodag.acquire.json.JsonException
import us.whodag.acquire.json.StandardAcquirePersist
import us.whodag.acquire.json.parseJson
import us.whodag.acquire.json.toStandardAcquirePersist
import us.whodag.acquire.obj.AcqObj
import us.whodag.acquire.obj.AcqObjs
import us.whodag.acquire.obj.player.PlayerId
import us.whodag.acquire.sm.AcqSm
import us.whodag.acquire.sm.AcqSmState
import us.whodag.acquire.acq.JsonCacheAcquire
import java.io.File
import java.io.FileNotFoundException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Objects.hash

/**
 * Factory for creating Acquire games.
 */
object Acquires : KLogging() {

    /** The current version of the Acquire implementation. */
    val CURRENT_VERSION = AcqVersion(1, 0)

    /** Create a standard Acquire game with the specified players. */
    fun standard(players: Collection<PlayerId>): Acquire =
            standardStart(players, AcqObjs.standard(players))

    /** Create a standard Acquire game using rand for shuffling the tiles. */
    fun standardShuffle(players: Collection<PlayerId>, shuffleSeed: Long): Acquire =
            standardStart(players, AcqObjs.standardShuffle(players, shuffleSeed))

    /** Load an Acquire game from the specified path. */
    fun standardLoadFromPath(path: String): Acquire? {
        try {
            return standardLoad(parseJson(File(path).readText()).toStandardAcquirePersist())
        } catch(e: FileNotFoundException) {
            logger.warn { "Unable to find game file at path [$path]: ${e.message}" }
        } catch (e: JsonException) {
            logger.warn { "Malformed game file at path [$path]: ${e.message}" }
        }
        return null
    }

    /* Create a standard Acquire game starting from the beginning. */
    private fun standardStart(players: Collection<PlayerId>, acqObj: AcqObj): Acquire =
            create(generateId(players),
                   CURRENT_VERSION,
                   AcqType.Standard,
                   players,
                   AcqSm.start(acqObj))

    /* Load a standard game from persistence. */
    private fun standardLoad(persist: StandardAcquirePersist): Acquire {
        if (!CURRENT_VERSION.isCompatible(persist.version)) throw UnsupportedOperationException("Cannot load game as current version [$CURRENT_VERSION] is incompatible with load game version ${persist.version}")
        if (CURRENT_VERSION != persist.version) logger.warn { "Loaded game will work but current version [$CURRENT_VERSION] does not match loaded game version ${persist.version}" }
        val acquire = create(persist.id,
                             persist.version,
                             persist.type,
                             persist.players,
                             AcqSm.start(AcqObjs.standardShuffle(persist.players, persist.shuffleSeed)))
        // ignore the start game request
        val requests = persist.requests.drop(1)
        requests.forEach({ acquire.submit(it) })
        return acquire
    }

    /* Create an Acquire game. */
    private fun create(id: AcquireId,
                       version: AcqVersion,
                       type: AcqType,
                       players: Collection<PlayerId>,
                       startState: AcqSmState): Acquire =
            BaseAcquire(id, version, type, players, startState)

    /** Persist this acquire game to a file. */
    fun Acquire.filePersist(): FilePersistentAcquire {
        val path = generatePath(this)
        logger.debug { "Persisting acquire game [${id()}] to file at path: $path" }
        return if (type() == AcqType.Standard) FilePersistentAcquire(this, path)
               else throw UnsupportedOperationException("Unable to persist non-standard acquire game")
    }

    /** Cache JSON serializations of acquire state. */
    fun Acquire.jsonCache(): JsonCacheAcquire {
        return JsonCacheAcquire(this)
    }
}

/** Return the default game path for the specified game. */
fun defaultGamePath(game: String) = "games/$game"

/* Generate a path for an Acquire game */
private fun generatePath(acquire: Acquire): String = defaultGamePath("${acquire.id().name}.acq")

/* Generate an AcquireId given a collection of players. */
private val GEN_ID_PATTERN = DateTimeFormatter.ofPattern("MM-dd-uuuu-HH-mm")
private fun generateId(players: Collection<PlayerId>): AcquireId {
    val now = LocalDateTime.now()
    val ps = players.sorted().joinToString(separator = "-")
    val ts = now.format(GEN_ID_PATTERN).toString()
    val ss = Math.abs(hash(now))
    return AcquireId("${ps}_${ts}_$ss")
}