package us.whodag.acquire

import us.whodag.acquire.obj.player.PlayerId
import us.whodag.acquire.sm.GameState

object TestAcqObjs {
    val stdPlayerIds = listOf("odo", "joree", "sal").map { n -> PlayerId(n) }
    val stdGameState = GameState(stdPlayerIds, stdPlayerIds[0])
}
