package us.whodag.acquire.ai

import us.whodag.acquire.*
import us.whodag.acquire.obj.player.PlayerId
import us.whodag.acquire.req.AcqReq

/**
 * Describes an AI for an Acquire game.
 */
interface AcqAi {

    /**
     * Choose a request to be submitted given an Acquire game and a player.
     * Null returned if no request is available.
     */
    fun chooseRequest(acquire: Acquire, player: PlayerId): AcqReq?
}
