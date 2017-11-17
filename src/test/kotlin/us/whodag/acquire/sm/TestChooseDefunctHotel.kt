package us.whodag.acquire.sm

import org.junit.Assert
import org.junit.Test
import us.whodag.acquire.*
import us.whodag.acquire.obj.AcqObjs
import us.whodag.acquire.obj.bank.Banks
import us.whodag.acquire.obj.board.Boards
import us.whodag.acquire.obj.board.Boards.withTile
import us.whodag.acquire.obj.hotel
import us.whodag.acquire.obj.hotel.HotelState
import us.whodag.acquire.obj.hotel.Hotels
import us.whodag.acquire.obj.player.PlayerId
import us.whodag.acquire.obj.player.Players
import us.whodag.acquire.obj.tile.TileId
import us.whodag.acquire.obj.tile.Tiles
import us.whodag.acquire.req.isFailure
import us.whodag.acquire.req.isSuccess

class TestChooseDefunctHotel {

    @Test
    fun invalid() {
        val (odo, joree, sal) = TestAcqObjs.stdPlayerIds
        val mergeTile = TileId("1-B")
        val hotelItr = Hotels.STANDARD_ID_TIERS.keys.iterator()
        val hotel1 = hotelItr.next()
        val hotel2 = hotelItr.next()
        val hotel3 = hotelItr.next()
        val hotel4 = hotelItr.next()
        val hotel1Tiles = listOf(TileId("1-A"), TileId("2-A"), TileId("3-A"))
        val hotel2Tiles = listOf(TileId("5-A"), TileId("6-A"), TileId("7-A"))
        val hotel3Tiles = listOf(TileId("4-B"), TileId("4-C"), TileId("5-C"))
        val hotels = Hotels.standard().map { h ->
            when {
                h.id() == hotel1 -> h.withState(HotelState.OnBoard(hotel1Tiles))
                h.id() == hotel2 -> h.withState(HotelState.OnBoard(hotel2Tiles))
                h.id() == hotel3 -> h.withState(HotelState.OnBoard(hotel3Tiles))
                else -> h
            }
        }
        val players = listOf(Players.standardPlayer(PlayerId("odo")),
                             Players.standardPlayer(PlayerId("joree")),
                             Players.standardPlayer(PlayerId("sal")))
        val board = Boards.standard().withTile(mergeTile)
        val acqObj = AcqObjs.custom(Tiles.standard(), Banks.standard(), board, hotels, players)
        val mergeContext = MergeContext(TestAcqObjs.stdGameState,
                                        mergeTile,
                                        listOf(hotel1, hotel2))
        val cdh0 = ChooseDefunctHotel(acqObj, mergeContext, hotel1, listOf(hotel2, hotel3), listOf(hotel2, hotel3))

        /*
         * sal ChooseDefunctHotel request (expect failure: not current player)
         */

        val (r1, s1) = cdh0.chooseHotelToDefunct(sal, hotel2)
        Assert.assertTrue("Expect failure", r1.isFailure())
        Assert.assertTrue("StateMachine state is unchanged", cdh0 == s1)
        val cdh1 = s1 as ChooseDefunctHotel

        /*
         * odo ChooseDefunctHotel request (expect failure: hotel not one of the options)
         */

        val (r2, s2) = cdh1.chooseHotelToDefunct(odo, hotel4)
        Assert.assertTrue("Expect failure", r2.isFailure())
        Assert.assertTrue("StateMachine state is unchanged", cdh1 == s2)
    }

    @Test
    fun chooseHotel() {
        val (odo, joree, sal) = TestAcqObjs.stdPlayerIds
        val mergeTile = TileId("1-B")
        val hotelItr = Hotels.STANDARD_ID_TIERS.keys.iterator()
        val hotel1 = hotelItr.next()
        val hotel2 = hotelItr.next()
        val hotel3 = hotelItr.next()
        val hotel4 = hotelItr.next()
        val hotel1Tiles = listOf(TileId("1-A"), TileId("2-A"), TileId("3-A"))
        val hotel2Tiles = listOf(TileId("5-A"), TileId("6-A"), TileId("7-A"))
        val hotel3Tiles = listOf(TileId("4-B"), TileId("4-C"), TileId("5-C"))
        val hotels = Hotels.standard().map { h ->
            when {
                h.id() == hotel1 -> h.withState(HotelState.OnBoard(hotel1Tiles))
                h.id() == hotel2 -> h.withState(HotelState.OnBoard(hotel2Tiles))
                h.id() == hotel3 -> h.withState(HotelState.OnBoard(hotel3Tiles))
                else -> h
            }
        }
        val players = listOf(Players.standardPlayer(PlayerId("odo"), initialStocks = mapOf(hotel2 to 5)),
                             Players.standardPlayer(PlayerId("joree")),
                             Players.standardPlayer(PlayerId("sal")))
        val board = Boards.standard().withTile(mergeTile)
        val acqObj = AcqObjs.custom(Tiles.standard(), Banks.standard(), board, hotels, players)
        val mergeContext = MergeContext(TestAcqObjs.stdGameState,
                                        mergeTile,
                                        listOf(hotel1, hotel2))
        val cdh0 = ChooseDefunctHotel(acqObj, mergeContext, hotel1, listOf(hotel2, hotel3), listOf(hotel2, hotel3))

        /*
         * odo ChooseDefunctHotel request
         */

        val (r1, s1) = cdh0.chooseHotelToDefunct(odo, hotel2)
        Assert.assertTrue("Expect Success", r1.isSuccess())
        Assert.assertTrue("StateMachine state is BuyStock", s1 is PayBonuses)
        val pb1 = s1 as PayBonuses

        // ensure state correct
        Assert.assertEquals("MergeContexts are equal", cdh0.mergeContext, pb1.mergeState.mergeContext)
        Assert.assertEquals("Surviving hotels are equal", cdh0.survivingHotel, pb1.mergeState.survivingHotel)
        Assert.assertEquals("Remaining hotels include those being merged", cdh0.remainingHotels, pb1.mergeState.remainingHotels)
        Assert.assertEquals("Defunct hotel is the hotel chosen", hotel2, pb1.mergeState.defunctHotel)
        Assert.assertEquals("odo is the player going to be paid", mapOf(odo to (pb1.acqObj.hotel(hotel2).majorityBonus() + pb1.acqObj.hotel(hotel2).minorityBonus())), pb1.playersToPay)
    }
}