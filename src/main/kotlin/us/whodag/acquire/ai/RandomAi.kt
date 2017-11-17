package us.whodag.acquire.ai

import us.whodag.acquire.req.AcqReq
import us.whodag.acquire.req.AcqReqs.possibleReqs
import us.whodag.acquire.Acquire
import us.whodag.acquire.obj.player.PlayerId
import java.util.*

/**
 * An Acquire AI which returns a random action.
 */
data class RandomAi(private val rand: Random) : AcqAi {
    constructor(seed: Long) : this(Random(seed))
    constructor() : this(Random())

    override fun chooseRequest(acquire: Acquire, player: PlayerId): AcqReq? {
        val reqs = acquire.possibleReqs(player)
        return if (reqs.isEmpty()) null
               else reqs[rand.nextInt(reqs.size)]
    }
}
