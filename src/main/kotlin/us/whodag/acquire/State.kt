package us.whodag.acquire

import us.whodag.acquire.req.AcqReq
import us.whodag.acquire.req.Response
import us.whodag.acquire.sm.AcqSmState

/**
 * The state at a point in time of an Acquire game.
 *
 * @property turn the turn of this state in the Acquire game.
 * @property acqReq the last Acquire request which has been completed.
 * @property response the response to the last Acquire request.
 * @property acqSmState the current Acquire state machine state.
 */
data class State(val turn: Int, val acqReq: AcqReq, val response: Response, val acqSmState: AcqSmState)