package us.whodag.acquire.sm

import org.junit.Assert
import org.junit.Test
import us.whodag.acquire.obj.AcqObjs
import us.whodag.acquire.TestAcqObjs
import us.whodag.acquire.req.isFailure
import us.whodag.acquire.req.isSuccess
import us.whodag.acquire.obj.bank.Banks
import us.whodag.acquire.obj.board.Boards
import us.whodag.acquire.obj.hotel.HotelState
import us.whodag.acquire.obj.hotel.Hotels
import us.whodag.acquire.obj.player
import us.whodag.acquire.obj.player.Players
import us.whodag.acquire.obj.tile.TileId
import us.whodag.acquire.obj.tile.Tiles

class TestFoundersStock {

    @Test
    fun all() {
        val (odo, joree, sal) = TestAcqObjs.stdPlayerIds
        val hotelTiles = listOf(TileId("3-A"), TileId("3-B"))
        val hotel = Hotels.STANDARD_ID_TIERS.keys.iterator().next()
        val hotels = Hotels.standard().map { h -> if (h.id() == hotel) h.withState(HotelState.OnBoard(hotelTiles))
                                                  else                 h }
        val acqObj = AcqObjs.custom(Tiles.standard(), Banks.standard(), Boards.standard(), hotels, Players.standard(TestAcqObjs.stdPlayerIds))
        val fs0 = FoundersStock(acqObj, TestAcqObjs.stdGameState, hotel)

        /*
         * FoundersStock request for sal (failure)
         */

        val (r1, s1) = fs0.receiveStock(sal)
        Assert.assertTrue("Expect failure", r1.isFailure())
        Assert.assertTrue("State equals state before failed command", fs0 == s1)
        val fs1 = s1 as FoundersStock

        /*
         * FoundersStock request for odo
         */

        val (r2, s2) = fs1.receiveStock(odo)
        Assert.assertTrue("Expect success", r2.isSuccess())
        Assert.assertTrue("State is BuyStock", s2 is BuyStock)
        val bs2 = s2 as BuyStock

        // verify BuyStock state
        Assert.assertEquals("GameStates are equal", fs1.gameState, bs2.gameState)
        // confirm odo updated correctly
        Assert.assertEquals("odo gained one stock of hotel", fs1.acqObj.player(odo).stock(hotel) + 1, bs2.acqObj.player(odo).stock(hotel))
        Assert.assertEquals("Bank lost one stock of hotel", fs1.acqObj.bank.stock(hotel) - 1, bs2.acqObj.bank.stock(hotel))
    }
}