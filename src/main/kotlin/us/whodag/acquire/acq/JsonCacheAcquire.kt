package us.whodag.acquire.acq

import mu.KLogging
import us.whodag.acquire.req.AcqReq
import us.whodag.acquire.Acquire
import us.whodag.acquire.req.Response
import us.whodag.acquire.json.stateJson

/**
 * In memory cache for JSON serialization of an Acquire game.
 *
 * @property acquire an Acquire game.
 * @property stateCache a cache of state JSON serializations at given turns.
 */
class JsonCacheAcquire(private val acquire: Acquire,
                       private val stateCache: MutableMap<Int, String>) : Acquire by acquire {
    constructor(acquire: Acquire) : this(acquire, mutableMapOf())

    companion object: KLogging()

    override fun submit(req: AcqReq): Response {
        // A completed Undo request will rewrite history, so the caches need to be cleared
        val preTurn = acquire.turn()
        val response = acquire.submit(req)
        val postTurn = acquire.turn()
        if (postTurn < preTurn) {
            logger.debug { "Detected rewritten history, clearing JSON caches" }
            stateCache.clear()
        }
        return response
    }

    /** The JSON string of the state of the Acquire gameat  the specified turn. */
    fun stateJson(turn: Int): String {
        var json = stateCache[turn]
        if (json == null) {
            json = acquire.state(turn).stateJson().toString()
            stateCache[turn] = json
        }
        return json
    }

    /** The JSON string of the history of states of the Acquire game between the specified turns. */
    fun statesJson(startTurn: Int, endTurn: Int): String {
        val stateJsons = (startTurn..endTurn).map { stateJson(it) }
        return "[" + stateJsons.joinToString() + "]"
    }
}

