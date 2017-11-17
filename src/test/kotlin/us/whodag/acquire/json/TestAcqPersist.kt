package us.whodag.acquire.json

import org.junit.Assert
import org.junit.Test
import us.whodag.acquire.req.DrawTileReq
import us.whodag.acquire.req.StartGameReq
import us.whodag.acquire.acq.*
import us.whodag.acquire.obj.player.PlayerId

class TestAcqPersist {

    @Test
    fun all() {
        val stdAcqPersist = StandardAcquirePersist(AcquireId("acquire-name"),
                                                   AcqVersion(1, 2),
                                                   AcqType.Standard,
                                                   1,
                                                   listOf(PlayerId("odo"), PlayerId("joree")),
                                                   listOf(StartGameReq(PlayerId("odo")), DrawTileReq(PlayerId("joree"))))
        Assert.assertEquals("StandardAcquirePersists are equal", stdAcqPersist, parseJson(stdAcqPersist.json().toString()).toStandardAcquirePersist())
    }
}