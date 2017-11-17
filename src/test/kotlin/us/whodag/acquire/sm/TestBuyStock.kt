package us.whodag.acquire.sm

import org.junit.Assert
import org.junit.Test
import us.whodag.acquire.TestAcqObjs
import us.whodag.acquire.obj.AcqObjs
import us.whodag.acquire.obj.bank.Banks
import us.whodag.acquire.obj.bank.Banks.withStocks
import us.whodag.acquire.obj.board.Boards
import us.whodag.acquire.obj.hotel
import us.whodag.acquire.obj.hotel.HotelState
import us.whodag.acquire.obj.hotel.Hotels
import us.whodag.acquire.obj.player
import us.whodag.acquire.obj.player.PlayerId
import us.whodag.acquire.obj.player.Players
import us.whodag.acquire.obj.tile.TileId
import us.whodag.acquire.obj.tile.Tiles
import us.whodag.acquire.req.isFailure
import us.whodag.acquire.req.isSuccess

class TestBuyStock {

    @Test
    fun invalid() {
        val (odo, joree, sal) = TestAcqObjs.stdPlayerIds
        val hotelItr = Hotels.STANDARD_ID_TIERS.keys.iterator()
        val hotel1 = hotelItr.next()
        val hotel2 = hotelItr.next()
        val hotel3 = hotelItr.next()
        val hotel1Tiles = listOf(TileId("1-A"), TileId("2-A"))
        val hotel2Tiles = listOf(TileId("1-C"), TileId("2-C"), TileId("3-C"), TileId("4-C"), TileId("5-C"),
                                 TileId("6-C"), TileId("7-C"), TileId("8-C"), TileId("9-C"), TileId("10-C"))
        val hotels = Hotels.standard().map { h ->
            when {
                h.id() == hotel1 -> h.withState(HotelState.OnBoard(hotel1Tiles))
                h.id() == hotel2 -> h.withState(HotelState.OnBoard(hotel2Tiles))
                else -> h
            }
        }
        val players = listOf(Players.standardPlayer(PlayerId("odo"), initialMoney = 600),
                             Players.standardPlayer(PlayerId("joree")),
                             Players.standardPlayer(PlayerId("sal")))
        val bank = Banks.standard().withStocks(2)
        val acqObj = AcqObjs.custom(Tiles.standard(), bank, Boards.standard(), hotels, players)
        val bs0 = BuyStock(acqObj, TestAcqObjs.stdGameState)

        /*
         * sal BuyStock request (expect failure: not current player)
         */

        val (r1, s1) = bs0.buyStock(sal, emptyMap())
        Assert.assertTrue("Expect failure", r1.isFailure())
        Assert.assertTrue("StateMachine state is unchanged", bs0 == s1)
        val bs1 = s1 as BuyStock

        /*
         * odo BuyStock request (expect failure: hotel not on the board)
         */

        val (r2, s2) = bs1.buyStock(odo, mapOf(hotel1 to 1, hotel3 to 1))
        Assert.assertTrue("Expect failure", r2.isFailure())
        Assert.assertTrue("StateMachine state is unchanged", bs1 == s2)
        val bs2 = s2 as BuyStock

        /*
         * odo BuyStock request (expect failure: not enough hotel stocks available)
         */

        val (r3, s3) = bs2.buyStock(odo, mapOf(hotel1 to 3))
        Assert.assertTrue("Expect failure", r3.isFailure())
        Assert.assertTrue("StateMachine state is unchanged", bs2 == s3)
        val bs3 = s3 as BuyStock

        /*
         * odo BuyStock request (expect failure: not enough money)
         */

        val (r4, s4) = bs3.buyStock(odo, mapOf(hotel2 to 2))
        Assert.assertTrue("Expect failure", r4.isFailure())
        Assert.assertTrue("StateMachine state is unchanged", bs3 == s4)
        val bs4 = s4 as BuyStock

        /*
         * odo BuyStock request (expect failure: buying more than allowed stocks)
         */

        val (r5, s5) = bs4.buyStock(odo, mapOf(hotel1 to 2, hotel2 to 2))
        Assert.assertTrue("Expect failure", r5.isFailure())
        Assert.assertTrue("StateMachine state is unchanged", bs4 == s5)
        val bs5 = s5 as BuyStock

        /*
         * odo BuyStock request (expect failure: buying negative stocks)
         */

        val (r6, s6) = bs4.buyStock(odo, mapOf(hotel1 to -1))
        Assert.assertTrue("Expect failure", r6.isFailure())
        Assert.assertTrue("StateMachine state is unchanged", bs5 == s6)
        val bs6 = s6 as BuyStock

        /*
         * odo BuyStock request (expect failure: buying more than 3 stocks)
         */

        val (r7, s7) = bs4.buyStock(odo, mapOf(hotel1 to 4))
        Assert.assertTrue("Expect failure", r7.isFailure())
        Assert.assertTrue("StateMachine state is unchanged", bs6 == s7)
        val bs7 = s7 as BuyStock
    }

    @Test
    fun buyNone() {
        val (odo, joree, sal) = TestAcqObjs.stdPlayerIds
        val hotelItr = Hotels.STANDARD_ID_TIERS.keys.iterator()
        val hotel1 = hotelItr.next()
        val hotel2 = hotelItr.next()
        val hotel1Tiles = listOf(TileId("1-A"), TileId("2-A"))
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
        val bs0 = BuyStock(acqObj, TestAcqObjs.stdGameState)

        /*
         * odo BuyStock request
         */

        val (r1, s1) = bs0.buyStock(odo, emptyMap())
        Assert.assertTrue("Expect success", r1.isSuccess())
        Assert.assertTrue("State is now DrawTile", s1 is DrawTile)
        val dt1 = s1 as DrawTile

        // ensure state correct
        Assert.assertEquals("GameStates are equal", bs0.gameState, dt1.gameState)

        // ensure player correct
        Assert.assertEquals("odo's money did not change", bs0.acqObj.player(odo).money(), dt1.acqObj.player(odo).money())
        Assert.assertEquals("odo's stocks did not change", bs0.acqObj.player(odo).stocks(), dt1.acqObj.player(odo).stocks())

        // ensure bank correct
        Assert.assertEquals("Bank's stocks did not change", bs0.acqObj.bank.stocks(), dt1.acqObj.bank.stocks())
    }

    @Test
    fun buyOneHotel() {
        val (odo, joree, sal) = TestAcqObjs.stdPlayerIds
        val hotelItr = Hotels.STANDARD_ID_TIERS.keys.iterator()
        val hotel1 = hotelItr.next()
        val hotel2 = hotelItr.next()
        val hotel1Tiles = listOf(TileId("1-A"), TileId("2-A"))
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
        val bs0 = BuyStock(acqObj, TestAcqObjs.stdGameState)

        /*
         * odo BuyStock request
         */

        val (r1, s1) = bs0.buyStock(odo, mapOf(hotel1 to 3))
        Assert.assertTrue("Expect success", r1.isSuccess())
        Assert.assertTrue("State is now DrawTile", s1 is DrawTile)
        val dt1 = s1 as DrawTile

        // ensure state correct
        Assert.assertEquals("GameStates are equal", bs0.gameState, dt1.gameState)

        // ensure player correct
        Assert.assertEquals("odo's money decreased by stock price", bs0.acqObj.player(odo).money() - bs0.acqObj.hotel(hotel1).stockPrice() * 3, dt1.acqObj.player(odo).money())
        Assert.assertEquals("odo's stocks of hotel1 increased by 3", bs0.acqObj.player(odo).stock(hotel1) + 3, dt1.acqObj.player(odo).stock(hotel1))

        // ensure bank correct
        Assert.assertEquals("Bank's stocks of hotel1 decreased by 3", bs0.acqObj.bank.stock(hotel1) - 3, dt1.acqObj.bank.stock(hotel1))
    }

    @Test
    fun buyTwoHotels() {
        val (odo, joree, sal) = TestAcqObjs.stdPlayerIds
        val hotelItr = Hotels.STANDARD_ID_TIERS.keys.iterator()
        val hotel1 = hotelItr.next()
        val hotel2 = hotelItr.next()
        val hotel1Tiles = listOf(TileId("1-A"), TileId("2-A"))
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
        val bs0 = BuyStock(acqObj, TestAcqObjs.stdGameState)

        /*
         * odo BuyStock request
         */

        val (r1, s1) = bs0.buyStock(odo, mapOf(hotel1 to 2, hotel2 to 1))
        Assert.assertTrue("Expect success", r1.isSuccess())
        Assert.assertTrue("State is now DrawTile", s1 is DrawTile)
        val dt1 = s1 as DrawTile

        // ensure state correct
        Assert.assertEquals("GameStates are equal", bs0.gameState, dt1.gameState)

        // ensure player correct
        Assert.assertEquals("odo's money decreased by stock price", bs0.acqObj.player(odo).money() - bs0.acqObj.hotel(hotel1).stockPrice() * 2
                                                                                                   - bs0.acqObj.hotel(hotel2).stockPrice() * 1 , dt1.acqObj.player(odo).money())
        Assert.assertEquals("odo's stocks of hotel1 increased by 2", bs0.acqObj.player(odo).stock(hotel1) + 2, dt1.acqObj.player(odo).stock(hotel1))
        Assert.assertEquals("odo's stocks of hotel2 increased by 1", bs0.acqObj.player(odo).stock(hotel2) + 1, dt1.acqObj.player(odo).stock(hotel2))

        // ensure bank correct
        Assert.assertEquals("Bank's stocks of hotel1 decreased by 2", bs0.acqObj.bank.stock(hotel1) - 2, dt1.acqObj.bank.stock(hotel1))
        Assert.assertEquals("Bank's stocks of hotel2 decreased by 1", bs0.acqObj.bank.stock(hotel2) - 1, dt1.acqObj.bank.stock(hotel2))
    }
}