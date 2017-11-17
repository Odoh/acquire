package us.whodag.acquire.sm

import org.junit.Assert
import org.junit.Test
import us.whodag.acquire.*
import us.whodag.acquire.obj.AcqObjs
import us.whodag.acquire.obj.bank.Banks
import us.whodag.acquire.obj.board.Boards
import us.whodag.acquire.obj.hotel
import us.whodag.acquire.obj.hotel.HotelState
import us.whodag.acquire.obj.hotel.Hotels
import us.whodag.acquire.obj.player.PlayerId
import us.whodag.acquire.obj.player.Players
import us.whodag.acquire.obj.tile.TileId
import us.whodag.acquire.obj.tile.Tiles
import us.whodag.acquire.req.isFailure
import us.whodag.acquire.req.isSuccess

class TestChooseSurvivingHotel {

    @Test
    fun invalid() {
        val (odo, joree, sal) = TestAcqObjs.stdPlayerIds
        val hotelItr = Hotels.STANDARD_ID_TIERS.keys.iterator()
        val hotel1 = hotelItr.next()
        val hotel2 = hotelItr.next()
        val hotel3 = hotelItr.next()
        val hotel1Tiles = listOf(TileId("1-A"), TileId("2-A"), TileId("3-A"), TileId("4-A"), TileId("5-A"))
        val hotel2Tiles = listOf(TileId("1-C"), TileId("2-C"), TileId("3-C"), TileId("4-C"), TileId("5-C"))
        val hotels = Hotels.standard().map { h ->
            when {
                h.id() == hotel1 -> h.withState(HotelState.OnBoard(hotel1Tiles))
                h.id() == hotel2 -> h.withState(HotelState.OnBoard(hotel2Tiles))
                else -> h
            }
        }
        val players = listOf(Players.standardPlayer(PlayerId("odo")),
                             Players.standardPlayer(PlayerId("joree")),
                             Players.standardPlayer(PlayerId("sal")))
        val acqObj = AcqObjs.custom(Tiles.standard(), Banks.standard(), Boards.standard(), hotels, players)
        val mergeContext = MergeContext(TestAcqObjs.stdGameState,
                                        TileId("1-B"),
                                        listOf(hotel1, hotel2))
        val csh0 = ChooseSurvivingHotel(acqObj, mergeContext, listOf(hotel1, hotel2))

        /*
         * sal ChooseSurvivingHotel request (expect failure: not current player)
         */

        val (r1, s1) = csh0.chooseSurvivingHotel(sal, hotel1)
        Assert.assertTrue("Expect failure", r1.isFailure())
        Assert.assertTrue("StateMachine state is unchanged", csh0 == s1)
        val csh1 = s1 as ChooseSurvivingHotel

        /*
         * odo ChooseSurvivingHotel request (expect failure: hotel not one of the options)
         */

        val (r2, s2) = csh1.chooseSurvivingHotel(odo, hotel3)
        Assert.assertTrue("Expect failure", r2.isFailure())
        Assert.assertTrue("StateMachine state is unchanged", csh1 == s2)
    }

    @Test
    fun majorityMinorityHolders() {
        val (odo, joree, sal) = TestAcqObjs.stdPlayerIds
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
        val players = listOf(Players.standardPlayer(PlayerId("odo"), initialStocks = mapOf(hotel2 to 3)),
                             Players.standardPlayer(PlayerId("joree"), initialStocks = mapOf(hotel2 to 2)),
                             Players.standardPlayer(PlayerId("sal"), initialStocks = mapOf(hotel2 to 1)))
        val acqObj = AcqObjs.custom(Tiles.standard(), Banks.standard(), Boards.standard(), hotels, players)
        val mergeContext = MergeContext(TestAcqObjs.stdGameState,
                                        TileId("1-B"),
                                        listOf(hotel1, hotel2))
        val csh0 = ChooseSurvivingHotel(acqObj, mergeContext, listOf(hotel1, hotel2))

        /*
         * odo ChooseSurvivingHotel request
         */

        val (r1, s1) = csh0.chooseSurvivingHotel(odo, hotel1)
        Assert.assertTrue("Expect success", r1.isSuccess())
        Assert.assertTrue("State is now PayBonuses", s1 is PayBonuses)
        val pb1 = s1 as PayBonuses

        // ensure state updated
        Assert.assertEquals("Merge contexts are equal", csh0.mergeContext, pb1.mergeState.mergeContext)
        Assert.assertEquals("Surviving hotel was as chosen", hotel1, pb1.mergeState.survivingHotel)
        Assert.assertEquals("Defunct hotel was as chosen", hotel2, pb1.mergeState.defunctHotel)
        Assert.assertTrue("Remaining hotels is empty", pb1.mergeState.remainingHotels.isEmpty())

        Assert.assertEquals("Players to pay contains odo and joree", 2, pb1.playersToPay.size)
        Assert.assertEquals("odo is paid the majority bonus", pb1.acqObj.hotel(hotel2).majorityBonus(), pb1.playersToPay[odo]!!)
        Assert.assertEquals("joree is paid the minority bonus", pb1.acqObj.hotel(hotel2).minorityBonus(), pb1.playersToPay[joree]!!)
    }

    @Test
    fun sameMajorityMinorityHolder() {
        val (odo, joree, sal) = TestAcqObjs.stdPlayerIds
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
        val players = listOf(Players.standardPlayer(PlayerId("odo"), initialStocks = mapOf(hotel2 to 2)),
                             Players.standardPlayer(PlayerId("joree")),
                             Players.standardPlayer(PlayerId("sal")))
        val acqObj = AcqObjs.custom(Tiles.standard(), Banks.standard(), Boards.standard(), hotels, players)
        val mergeContext = MergeContext(TestAcqObjs.stdGameState,
                                        TileId("1-B"),
                                        listOf(hotel1, hotel2))
        val csh0 = ChooseSurvivingHotel(acqObj, mergeContext, listOf(hotel1, hotel2))

        /*
         * odo ChooseSurvivingHotel request
         */

        val (r1, s1) = csh0.chooseSurvivingHotel(odo, hotel1)
        Assert.assertTrue("Expect success", r1.isSuccess())
        Assert.assertTrue("State is now PayBonuses", s1 is PayBonuses)
        val pb1 = s1 as PayBonuses

        // ensure state updated
        Assert.assertEquals("Merge contexts are equal", csh0.mergeContext, pb1.mergeState.mergeContext)
        Assert.assertEquals("Surviving hotel was as chosen", hotel1, pb1.mergeState.survivingHotel)
        Assert.assertEquals("Defunct hotel was as chosen", hotel2, pb1.mergeState.defunctHotel)
        Assert.assertTrue("Remaining hotels is empty", pb1.mergeState.remainingHotels.isEmpty())

        Assert.assertEquals("Players to pay contains odo", 1, pb1.playersToPay.size)
        Assert.assertEquals("odo is paid the majority bonus", pb1.acqObj.hotel(hotel2).majorityBonus() + pb1.acqObj.hotel(hotel2).minorityBonus(), pb1.playersToPay[odo]!!)
    }

    @Test
    fun majoritySplitMinorityHolders() {
        val (odo, joree, sal) = TestAcqObjs.stdPlayerIds
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
        val players = listOf(Players.standardPlayer(PlayerId("odo"), initialStocks = mapOf(hotel2 to 2)),
                             Players.standardPlayer(PlayerId("joree"), initialStocks = mapOf(hotel2 to 1)),
                             Players.standardPlayer(PlayerId("sal"), initialStocks = mapOf(hotel2 to 1)))
        val acqObj = AcqObjs.custom(Tiles.standard(), Banks.standard(), Boards.standard(), hotels, players)
        val mergeContext = MergeContext(TestAcqObjs.stdGameState,
                                        TileId("1-B"),
                                        listOf(hotel1, hotel2))
        val csh0 = ChooseSurvivingHotel(acqObj, mergeContext, listOf(hotel1, hotel2))

        /*
         * odo ChooseSurvivingHotel request
         */

        val (r1, s1) = csh0.chooseSurvivingHotel(odo, hotel1)
        Assert.assertTrue("Expect success", r1.isSuccess())
        Assert.assertTrue("State is now PayBonuses", s1 is PayBonuses)
        val pb1 = s1 as PayBonuses

        // ensure state updated
        Assert.assertEquals("Merge contexts are equal", csh0.mergeContext, pb1.mergeState.mergeContext)
        Assert.assertEquals("Surviving hotel was as chosen", hotel1, pb1.mergeState.survivingHotel)
        Assert.assertEquals("Defunct hotel was as chosen", hotel2, pb1.mergeState.defunctHotel)
        Assert.assertTrue("Remaining hotels is empty", pb1.mergeState.remainingHotels.isEmpty())

        Assert.assertEquals("Players to pay contains odo, joree, and sal", 3, pb1.playersToPay.size)
        Assert.assertEquals("odo is paid the majority bonus", pb1.acqObj.hotel(hotel2).majorityBonus(), pb1.playersToPay[odo]!!)
        Assert.assertEquals("joree is paid half the minority bonus", pb1.acqObj.hotel(hotel2).minorityBonus() / 2, pb1.playersToPay[joree]!!)
        Assert.assertEquals("sal is paid half the minority bonus", pb1.acqObj.hotel(hotel2).minorityBonus() / 2, pb1.playersToPay[sal]!!)
    }

    @Test
    fun sameMajorityMinorityHolders() {
        val (odo, joree, sal) = TestAcqObjs.stdPlayerIds
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
        val players = listOf(Players.standardPlayer(PlayerId("odo"), initialStocks = mapOf(hotel2 to 2)),
                                                    Players.standardPlayer(PlayerId("joree"), initialStocks = mapOf(hotel2 to 2)),
                                                    Players.standardPlayer(PlayerId("sal"), initialStocks = mapOf(hotel2 to 2)))
        val acqObj = AcqObjs.custom(Tiles.standard(), Banks.standard(), Boards.standard(), hotels, players)
        val mergeContext = MergeContext(TestAcqObjs.stdGameState,
                                        TileId("1-B"),
                                        listOf(hotel1, hotel2))
        val csh0 = ChooseSurvivingHotel(acqObj, mergeContext, listOf(hotel1, hotel2))

        /*
         * odo ChooseSurvivingHotel request
         */

        val (r1, s1) = csh0.chooseSurvivingHotel(odo, hotel1)
        Assert.assertTrue("Expect success", r1.isSuccess())
        Assert.assertTrue("State is now PayBonuses", s1 is PayBonuses)
        val pb1 = s1 as PayBonuses

        // ensure state updated
        Assert.assertEquals("Merge contexts are equal", csh0.mergeContext, pb1.mergeState.mergeContext)
        Assert.assertEquals("Surviving hotel was as chosen", hotel1, pb1.mergeState.survivingHotel)
        Assert.assertEquals("Defunct hotel was as chosen", hotel2, pb1.mergeState.defunctHotel)
        Assert.assertTrue("Remaining hotels is empty", pb1.mergeState.remainingHotels.isEmpty())

        Assert.assertEquals("Players to pay contains odo, joree, and sal", 3, pb1.playersToPay.size)
        Assert.assertEquals("odo is paid half the majority and minority bonus", (pb1.acqObj.hotel(hotel2).majorityBonus() + pb1.acqObj.hotel(hotel2).minorityBonus()) / 3, pb1.playersToPay[odo]!!)
        Assert.assertEquals("joree is paid half the majority and minority bonus", (pb1.acqObj.hotel(hotel2).majorityBonus() + pb1.acqObj.hotel(hotel2).minorityBonus()) / 3, pb1.playersToPay[joree]!!)
        Assert.assertEquals("sal is paid half the majority and minority bonus", (pb1.acqObj.hotel(hotel2).majorityBonus() + pb1.acqObj.hotel(hotel2).minorityBonus()) / 3, pb1.playersToPay[sal]!!)
    }

    @Test
    fun chooseWithDefunctHotel() {
        val (odo, joree, sal) = TestAcqObjs.stdPlayerIds
        val hotelItr = Hotels.STANDARD_ID_TIERS.keys.iterator()
        val hotel1 = hotelItr.next()
        val hotel2 = hotelItr.next()
        val hotel3 = hotelItr.next()
        val hotel1Tiles = listOf(TileId("1-A"), TileId("2-A"), TileId("3-A"))
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
        val players = listOf(Players.standardPlayer(PlayerId("odo"), initialStocks = mapOf(hotel2 to 2)),
                             Players.standardPlayer(PlayerId("joree"), initialStocks = mapOf(hotel2 to 1)),
                             Players.standardPlayer(PlayerId("sal")))
        val acqObj = AcqObjs.custom(Tiles.standard(), Banks.standard(), Boards.standard(), hotels, players)
        val mergeContext = MergeContext(TestAcqObjs.stdGameState,
                                        TileId("4-A"),
                                        listOf(hotel1, hotel2, hotel3))
        val csh0 = ChooseSurvivingHotel(acqObj, mergeContext, listOf(hotel1, hotel2))

        /*
         * odo ChooseSurvivingHotel request
         */

        val (r1, s1) = csh0.chooseSurvivingHotel(odo, hotel1)
        Assert.assertTrue("Expect success", r1.isSuccess())
        Assert.assertTrue("State is now PayBonuses", s1 is PayBonuses)
        val pb1 = s1 as PayBonuses

        // ensure state updated
        Assert.assertEquals("Merge contexts are equal", csh0.mergeContext, pb1.mergeState.mergeContext)
        Assert.assertEquals("Surviving hotel was as chosen", hotel1, pb1.mergeState.survivingHotel)
        Assert.assertEquals("Defunct hotel was as chosen", hotel2, pb1.mergeState.defunctHotel)
        Assert.assertEquals("Remaining hotels contains hotel2", listOf(hotel3), pb1.mergeState.remainingHotels)

        Assert.assertEquals("Players to pay contains odo and joree", 2, pb1.playersToPay.size)
        Assert.assertEquals("odo is paid the majority bonus", pb1.acqObj.hotel(hotel2).majorityBonus(), pb1.playersToPay[odo]!!)
        Assert.assertEquals("joree is the minority bonus", pb1.acqObj.hotel(hotel2).minorityBonus(), pb1.playersToPay[joree]!!)
    }

    @Test
    fun chooseThenDefunctHotel() {
        val (odo, joree, sal) = TestAcqObjs.stdPlayerIds
        val hotelItr = Hotels.STANDARD_ID_TIERS.keys.iterator()
        val hotel1 = hotelItr.next()
        val hotel2 = hotelItr.next()
        val hotel3 = hotelItr.next()
        val hotel1Tiles = listOf(TileId("1-A"), TileId("2-A"), TileId("3-A"))
        val hotel2Tiles = listOf(TileId("5-A"), TileId("6-A"), TileId("7-A"))
        val hotel3Tiles = listOf(TileId("4-B"), TileId("4-C"), TileId("4-D"))
        val hotels = Hotels.standard().map { h ->
            when {
                h.id() == hotel1 -> h.withState(HotelState.OnBoard(hotel1Tiles))
                h.id() == hotel2 -> h.withState(HotelState.OnBoard(hotel2Tiles))
                h.id() == hotel3 -> h.withState(HotelState.OnBoard(hotel3Tiles))
                else -> h
            }
        }
        val players = listOf(Players.standardPlayer(PlayerId("odo"), initialStocks = mapOf(hotel3 to 2)),
                             Players.standardPlayer(PlayerId("joree"), initialStocks = mapOf(hotel3 to 1)),
                             Players.standardPlayer(PlayerId("sal")))
        val acqObj = AcqObjs.custom(Tiles.standard(), Banks.standard(), Boards.standard(), hotels, players)
        val mergeContext = MergeContext(TestAcqObjs.stdGameState,
                                        TileId("4-A"),
                                        listOf(hotel1, hotel2, hotel3))
        val csh0 = ChooseSurvivingHotel(acqObj, mergeContext, listOf(hotel1, hotel2, hotel3))

        /*
         * odo ChooseSurvivingHotel request
         */

        val (r1, s1) = csh0.chooseSurvivingHotel(odo, hotel1)
        Assert.assertTrue("Expect success", r1.isSuccess())
        Assert.assertTrue("State is now PayBonuses", s1 is ChooseDefunctHotel)
        val cdh1 = s1 as ChooseDefunctHotel

        // ensure state updated
        Assert.assertEquals("Merge contexts are equal", csh0.mergeContext, cdh1.mergeContext)
        Assert.assertEquals("Surviving hotel was as chosen", hotel1, cdh1.survivingHotel)
        Assert.assertEquals("Remaining hotels contains hotel2 and hotel3", listOf(hotel2, hotel3).sorted(), cdh1.remainingHotels.sorted())
        Assert.assertEquals("Potential next defunct hotels contains hotel2 and hotel3", listOf(hotel2, hotel3).sorted(), cdh1.remainingHotels.sorted())
    }
}
