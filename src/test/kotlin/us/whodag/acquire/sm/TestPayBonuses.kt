package us.whodag.acquire.sm

import org.junit.Assert
import org.junit.Test
import us.whodag.acquire.*
import us.whodag.acquire.TestAcqObjs.stdPlayerIds
import us.whodag.acquire.obj.AcqObjs
import us.whodag.acquire.obj.bank.Banks
import us.whodag.acquire.obj.bank.Banks.withStocks
import us.whodag.acquire.obj.board.Boards
import us.whodag.acquire.obj.board.Boards.withTile
import us.whodag.acquire.obj.hotel.HotelState
import us.whodag.acquire.obj.hotel.Hotels
import us.whodag.acquire.obj.player
import us.whodag.acquire.obj.player.PlayerId
import us.whodag.acquire.obj.player.Players
import us.whodag.acquire.obj.tile.TileId
import us.whodag.acquire.obj.tile.Tiles
import us.whodag.acquire.req.isFailure
import us.whodag.acquire.req.isSuccess

class TestPayBonuses {

    @Test
    fun all() {
        val (odo, joree, sal) = TestAcqObjs.stdPlayerIds
        val mergeTile = TileId("1-B")
        val hotelItr = Hotels.STANDARD_ID_TIERS.keys.iterator()
        val hotel1 = hotelItr.next()
        val hotel2 = hotelItr.next()
        val hotel1Tiles = listOf(TileId("1-A"), TileId("2-A"), TileId("3-A"), TileId("4-A"), TileId("5-A"))
        val hotel2Tiles = listOf(TileId("1-C"), TileId("2-C"), TileId("3-C"), TileId("4-C"), TileId("5-C"))
        val hotels = Hotels.standard().map { h ->
            when {
                h.id() == hotel1 -> h.withState(HotelState.OnBoard(hotel1Tiles))
                h.id() == hotel2 -> h.withState(HotelState.OnBoard(hotel2Tiles))
                else -> h
            }
        }
        val players = listOf(Players.standardPlayer(PlayerId("odo"), initialStocks = mapOf(hotel2 to 7)),
                             Players.standardPlayer(PlayerId("joree")),
                             Players.standardPlayer(PlayerId("sal")))
        val bank = Banks.standard().withStocks(2)
        val board = Boards.standard().withTile(mergeTile)
        val acqObj = AcqObjs.custom(Tiles.standard(), bank, board, hotels, players)
        val mergeContext = MergeContext(TestAcqObjs.stdGameState,
                                        mergeTile,
                                        listOf(hotel1, hotel2))
        val mergeState = MergeState(mergeContext,
                                    survivingHotel = hotel1,
                                    defunctHotel = hotel2,
                                    remainingHotels = emptyList())
        val pb0 = PayBonuses(acqObj, mergeState, mapOf(odo to 6000, joree to 3500))

        /*
         * odo PayBonus request
         */

        val (r1, s1) = pb0.payBonus(odo)
        Assert.assertTrue("Expect success", r1.isSuccess())
        Assert.assertTrue("StateMachine state is PayBonuses", s1 is PayBonuses)
        val pb1 = s1 as PayBonuses

        // ensure state correct
        Assert.assertEquals("MergeStates are equal", pb0.mergeState, pb1.mergeState)
        Assert.assertEquals("odo was removed from players to pay", pb0.playersToPay - odo, pb1.playersToPay)

        // ensure player correct
        Assert.assertEquals("odo's money increased by his bonus amount", pb0.acqObj.player(odo).money() + 6000, pb1.acqObj.player(odo).money())
        Assert.assertEquals("joree's money stayed the same", pb0.acqObj.player(joree).money(), pb1.acqObj.player(joree).money())

        /*
         * odo PayBonus request (expect failure: odo has already received his bonus)
         */

        val (r2, s2) = pb1.payBonus(odo)
        Assert.assertTrue("Expect failure", r2.isFailure())
        Assert.assertTrue("StateMachine state is the same as before", s2 == pb1)
        val pb2 = s2 as PayBonuses

        /*
         * joree PayBonus request
         */

        val (r3, s3) = pb2.payBonus(joree)
        Assert.assertTrue("Expect success", r3.isSuccess())
        Assert.assertTrue("StateMachine state is HandleDefunctHotelStocks", s3 is HandleDefunctHotelStocks)
        val hdhs3 = s3 as HandleDefunctHotelStocks

        // ensure state correct
        Assert.assertEquals("MergeStates are equal", pb2.mergeState, hdhs3.mergeState)
        Assert.assertEquals("odo has stocks that need to be handled", listOf(odo), hdhs3.playersWithStockInTurnOrder)

        // ensure player correct
        Assert.assertEquals("odo's money stayed the same", pb2.acqObj.player(odo).money(), hdhs3.acqObj.player(odo).money())
        Assert.assertEquals("joree's money increased by bonus amount", pb2.acqObj.player(joree).money() + 3500, hdhs3.acqObj.player(joree).money())
    }

    @Test
    fun mergingPlayerFirst() {
        val (odo, joree, sal) = TestAcqObjs.stdPlayerIds
        val mergeTile = TileId("1-B")
        val hotelItr = Hotels.STANDARD_ID_TIERS.keys.iterator()
        val hotel1 = hotelItr.next()
        val hotel2 = hotelItr.next()
        val hotel1Tiles = listOf(TileId("1-A"), TileId("2-A"), TileId("3-A"), TileId("4-A"), TileId("5-A"))
        val hotel2Tiles = listOf(TileId("1-C"), TileId("2-C"), TileId("3-C"), TileId("4-C"), TileId("5-C"))
        val hotels = Hotels.standard().map { h ->
            when {
                h.id() == hotel1 -> h.withState(HotelState.OnBoard(hotel1Tiles))
                h.id() == hotel2 -> h.withState(HotelState.OnBoard(hotel2Tiles))
                else -> h
            }
        }
        val players = listOf(Players.standardPlayer(PlayerId("odo"), initialStocks = mapOf(hotel2 to 7)),
                             Players.standardPlayer(PlayerId("joree"), initialStocks = mapOf(hotel2 to 4)),
                             Players.standardPlayer(PlayerId("sal"), initialStocks = mapOf(hotel2 to 4)))
        val bank = Banks.standard().withStocks(2)
        val board = Boards.standard().withTile(mergeTile)
        val acqObj = AcqObjs.custom(Tiles.standard(), bank, board, hotels, players)
        val mergeContext = MergeContext(GameState(stdPlayerIds, stdPlayerIds[1]),
                                        mergeTile,
                                        listOf(hotel1, hotel2))
        val mergeState = MergeState(mergeContext,
                                    survivingHotel = hotel1,
                                    defunctHotel = hotel2,
                                    remainingHotels = emptyList())
        val pb0 = PayBonuses(acqObj, mergeState, mapOf(odo to 6000, joree to 3500, sal to 3500))

        val (r1, s1) = pb0.payBonus(odo)
        Assert.assertTrue("Expect success", r1.isSuccess())
        Assert.assertTrue("StateMachine state is PayBonuses", s1 is PayBonuses)
        val pb1 = s1 as PayBonuses

        val (r2, s2) = pb1.payBonus(joree)
        Assert.assertTrue("Expect success", r2.isSuccess())
        Assert.assertTrue("StateMachine state is PayBonuses", s2 is PayBonuses)
        val pb2 = s2 as PayBonuses

        val (r3, s3) = pb2.payBonus(sal)
        Assert.assertTrue("Expect success", r3.isSuccess())
        Assert.assertTrue("StateMachine state is HandleDefunctHotelStocks", s3 is HandleDefunctHotelStocks)
        val hdhs3 = s3 as HandleDefunctHotelStocks

        // ensure state correct
        Assert.assertEquals("MergeStates are equal", pb0.mergeState, hdhs3.mergeState)
        Assert.assertEquals("Merging player is handled first then in turn order", listOf(joree, sal, odo), hdhs3.playersWithStockInTurnOrder)
    }
}