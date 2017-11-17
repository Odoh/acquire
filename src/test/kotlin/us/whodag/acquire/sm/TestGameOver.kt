package us.whodag.acquire.sm

import org.junit.Assert
import org.junit.Test
import us.whodag.acquire.obj.AcqObjs
import us.whodag.acquire.TestAcqObjs
import us.whodag.acquire.req.isSuccess
import us.whodag.acquire.obj.bank.Banks
import us.whodag.acquire.obj.board.Boards
import us.whodag.acquire.obj.hotel.Hotels
import us.whodag.acquire.obj.player.PlayerId
import us.whodag.acquire.obj.player.Players
import us.whodag.acquire.obj.tile.Tiles

class TestGameOver {

    @Test
    fun all() {
        val (odo, joree, sal) = TestAcqObjs.stdPlayerIds
        val players = listOf(Players.standardPlayer(PlayerId("odo")),
                                                    Players.standardPlayer(PlayerId("joree")),
                                                    Players.standardPlayer(PlayerId("sal")))
        val acqObj = AcqObjs.custom(Tiles.standard(), Banks.standard(), Boards.standard(), Hotels.standard(), players)
        val go0 = GameOver(acqObj, TestAcqObjs.stdPlayerIds)

        /*
         * odo GameOver request
         */

        val (r1, s1) = go0.results(odo)
        Assert.assertTrue("Expect success", r1.isSuccess())
        Assert.assertTrue("State is still EndGamePayout", s1 is GameOver)
        val go1 = s1 as GameOver

        // confirm state is correct
        Assert.assertEquals("State stays the same", go0, go1)

        /*
         * odo GameOver request
         */

        val (r2, s2) = go1.results(odo)
        Assert.assertTrue("Expect success", r2.isSuccess())
        Assert.assertTrue("State is still EndGamePayout", s2 is GameOver)
        val go2 = s2 as GameOver

        // confirm state is correct
        Assert.assertEquals("State stays the same", go1, go2)

        /*
         * joree GameOver request
         */

        val (r3, s3) = go2.results(joree)
        Assert.assertTrue("Expect success", r3.isSuccess())
        Assert.assertTrue("State is still EndGamePayout", s3 is GameOver)
        val go3 = s3 as GameOver

        // confirm state is correct
        Assert.assertEquals("State stays the same", go2, go3)

        /*
         * sal GameOver request
         */

        val (r4, s4) = go1.results(sal)
        Assert.assertTrue("Expect success", r4.isSuccess())
        Assert.assertTrue("State is still EndGamePayout", s4 is GameOver)
        val go4 = s4 as GameOver

        // confirm state is correct
        Assert.assertEquals("State stays the same", go3, go4)
    }
}
