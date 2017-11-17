package us.whodag.acquire

import us.whodag.acquire.acq.AcqType
import us.whodag.acquire.acq.AcqVersion
import us.whodag.acquire.acq.AcquireId
import us.whodag.acquire.obj.player.PlayerId
import us.whodag.acquire.req.AcqReq
import us.whodag.acquire.req.Response

/**
 * A game of Acquire which has been started.
 */
interface Acquire {

    /** Unique identifier. */
    fun id(): AcquireId

    /** The version of the Acquire game. */
    fun version(): AcqVersion

    /** The type of the Acquire game. */
    fun type(): AcqType

    /** The players in the Acquire game. */
    fun players(): Collection<PlayerId>

    /** The current turn of the game. Starts at 0, the start of the game, and will always be >= 0. */
    fun turn(): Int

    /** The state of the game including the request that led to that state at the specified turn. */
    fun state(turn: Int): State

    /** The current state of the game including the request that led to that state. */
    fun state(): State = state(turn())

    /** Submit a request to the game. */
    fun submit(req: AcqReq): Response

    /** A list of all requests and states from startTurn to endTurn, inclusive. */
    fun states(startTurn: Int, endTurn: Int): List<State> = (startTurn..endTurn).mapNotNull { state(it) }

    /** A list of all requests and states so far in the game. */
    fun states(): List<State> = (0..turn()).mapNotNull { state(it) }
}
