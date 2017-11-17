package us.whodag.acquire.sm

import org.junit.Assert
import org.junit.Test
import us.whodag.acquire.obj.AcqObjs
import us.whodag.acquire.TestAcqObjs
import us.whodag.acquire.req.isFailure
import us.whodag.acquire.req.isSuccess
import us.whodag.acquire.obj.bank.Banks
import us.whodag.acquire.obj.board.Boards
import us.whodag.acquire.obj.hotel
import us.whodag.acquire.obj.hotel.HotelState
import us.whodag.acquire.obj.hotel.Hotels
import us.whodag.acquire.obj.player
import us.whodag.acquire.obj.player.PlayerId
import us.whodag.acquire.obj.player.Players
import us.whodag.acquire.obj.tile.TileId
import us.whodag.acquire.obj.tile.Tiles

class TestEndGamePayout {

    @Test
    fun all() {
        val (odo, joree, sal) = TestAcqObjs.stdPlayerIds
        val hotelItr = Hotels.STANDARD_ID_TIERS.keys.iterator()
        val hotel1 = hotelItr.next()
        val hotel2 = hotelItr.next()
        val hotel3 = hotelItr.next()
        val hotel1Tiles = listOf(TileId("1-A"), TileId("2-A"), TileId("3-A"), TileId("4-A"))
        val hotel2Tiles = listOf(TileId("5-A"), TileId("6-A"), TileId("7-A"))
        val hotel3Tiles = listOf(TileId("4-B"), TileId("4-C"))
        val hotels = Hotels.standard().map { h ->
            when {
                h.id() == hotel1 -> h.withState(HotelState.OnBoard(hotel1Tiles))
                h.id() == hotel2 -> h.withState(HotelState.OnBoard(hotel2Tiles))
                h.id() == hotel3 -> h.withState(HotelState.OnBoard(hotel3Tiles))
                else -> h
            }
        }
        val players = listOf(Players.standardPlayer(PlayerId("odo"), initialStocks = mapOf(hotel1 to 5,
                                                                                                  hotel2 to 3,
                                                                                                  hotel3 to 1)),
                             Players.standardPlayer(PlayerId("joree"), initialStocks = mapOf(hotel2 to 4,
                                                                                                    hotel3 to 2)),
                             Players.standardPlayer(PlayerId("sal"), initialStocks = mapOf(hotel3 to 2)))
        val acqObj = AcqObjs.custom(Tiles.standard(), Banks.standard(), Boards.standard(), hotels, players)
        val egp0 = EndGamePayout(acqObj, emptyList())

        /*
         * odo EndGamePayout request
         */

        val (r1, s1) = egp0.payoutAssets(odo)
        Assert.assertTrue("Expect success", r1.isSuccess())
        Assert.assertTrue("State is still EndGamePayout", s1 is EndGamePayout)
        val egp1 = s1 as EndGamePayout

        // confirm state is correct
        Assert.assertEquals("odo is in players paid", listOf(odo), egp1.playersPaid)

        // confirm player updated
        val odoAssets = egp1.acqObj.hotel(hotel1).majorityBonus() + egp1.acqObj.hotel(hotel1).minorityBonus() + egp1.acqObj.hotel(hotel1).stockPrice() * egp1.acqObj.player(odo).stock(hotel1) +
                        egp1.acqObj.hotel(hotel2).minorityBonus() + egp1.acqObj.hotel(hotel2).stockPrice() * egp1.acqObj.player(odo).stock(hotel2) +
                        egp1.acqObj.hotel(hotel3).stockPrice() * egp1.acqObj.player(odo).stock(hotel3)
        Assert.assertEquals("odo's money increase by his asset worth", egp0.acqObj.player(odo).money() + odoAssets, egp1.acqObj.player(odo).money())
        Assert.assertEquals("joree's money stays the same", egp0.acqObj.player(joree).money(), egp1.acqObj.player(joree).money())
        Assert.assertEquals("sal's money stays the same", egp0.acqObj.player(sal).money(), egp1.acqObj.player(sal).money())

        /*
         * odo EndGamePayout request (expect failure - odo already paid out)
         */

        val (r2, s2) = egp1.payoutAssets(odo)
        Assert.assertTrue("Expect failure", r2.isFailure())
        Assert.assertTrue("State is the same as before", s2 == egp1)
        val egp2 = s2 as EndGamePayout

        /*
         * joree EndGamePayout request
         */

        val (r3, s3) = egp2.payoutAssets(joree)
        Assert.assertTrue("Expect success", r3.isSuccess())
        Assert.assertTrue("State is still EndGamePayout", s3 is EndGamePayout)
        val egp3 = s3 as EndGamePayout

        // confirm state is correct
        Assert.assertEquals("odo and joree are in players paid", listOf(odo, joree).sorted(), egp3.playersPaid.sorted())

        // confirm player is correct
        val joreeAssets = egp3.acqObj.hotel(hotel2).majorityBonus() + egp3.acqObj.hotel(hotel2).stockPrice() * egp3.acqObj.player(joree).stock(hotel2) +
                          ((egp3.acqObj.hotel(hotel3).majorityBonus() + egp3.acqObj.hotel(hotel3).minorityBonus()) / 2) + egp3.acqObj.hotel(hotel3).stockPrice() * egp3.acqObj.player(joree).stock(hotel3)
        Assert.assertEquals("odo's money stays the same", egp2.acqObj.player(odo).money(), egp3.acqObj.player(odo).money())
        Assert.assertEquals("joree's money increased by his asset worth", egp2.acqObj.player(joree).money() + joreeAssets, egp3.acqObj.player(joree).money())
        Assert.assertEquals("sal's money stays the same", egp2.acqObj.player(sal).money(), egp3.acqObj.player(sal).money())

        /*
         * sal EndGamePayout request
         */

        val (r4, s4) = egp3.payoutAssets(sal)
        Assert.assertTrue("Expect success", r4.isSuccess())
        Assert.assertTrue("State is GameOver", s4 is GameOver)
        val go4 = s4 as GameOver

        // confirm state is correct
        Assert.assertEquals("Players ordered by the most money received", listOf(odo, joree, sal), go4.playerResults)

        // confirm player is correct
        val salAssets = ((go4.acqObj.hotel(hotel3).majorityBonus() + go4.acqObj.hotel(hotel3).minorityBonus()) / 2) + go4.acqObj.hotel(hotel3).stockPrice() * go4.acqObj.player(sal).stock(hotel3)
        Assert.assertEquals("odo's money stays the same", egp3.acqObj.player(odo).money(), go4.acqObj.player(odo).money())
        Assert.assertEquals("joree's money stays the same", egp3.acqObj.player(joree).money(), go4.acqObj.player(joree).money())
        Assert.assertEquals("sal's money increases by his assets", egp3.acqObj.player(sal).money() + salAssets, go4.acqObj.player(sal).money())
    }
}