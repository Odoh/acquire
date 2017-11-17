package us.whodag.acquire.sm

import org.junit.Assert
import org.junit.Test
import us.whodag.acquire.obj.AcqObjs
import us.whodag.acquire.TestAcqObjs
import us.whodag.acquire.req.isFailure
import us.whodag.acquire.req.isSuccess
import us.whodag.acquire.obj.bank.Banks
import us.whodag.acquire.obj.bank.Banks.withDrawPile
import us.whodag.acquire.obj.board.Boards
import us.whodag.acquire.obj.hotel.HotelState
import us.whodag.acquire.obj.hotel.Hotels
import us.whodag.acquire.obj.player
import us.whodag.acquire.obj.player.PlayerId
import us.whodag.acquire.obj.player.Players
import us.whodag.acquire.obj.tile.TileId
import us.whodag.acquire.obj.tile.Tiles

class TestDrawTile {

    @Test
    fun invalid() {
        val (odo, joree, sal) = TestAcqObjs.stdPlayerIds
        val odoTiles = listOf(TileId("1-A"), TileId("1-B"), TileId("1-C"), TileId("1-D"))
        val players = listOf(Players.standardPlayer(PlayerId("odo"), initialTiles = odoTiles),
                             Players.standardPlayer(PlayerId("joree")),
                             Players.standardPlayer(PlayerId("sal")))
        val acqObj = AcqObjs.custom(Tiles.standard(), Banks.standard(), Boards.standard(), Hotels.standard(), players)
        val dt0 = DrawTile(acqObj, TestAcqObjs.stdGameState)

        /*
         * sal DrawTile request (expect failure: not current player)
         */

        val (r1, s1) = dt0.drawTile(sal)
        Assert.assertTrue("Expect failure", r1.isFailure())
        Assert.assertTrue("StateMachine state is unchanged", dt0 == s1)
        val dt1 = s1 as DrawTile

        /*
         * sal EndGame request (expect failure: not current player)
         */

        val (r2, s2) = dt1.endGame(sal)
        Assert.assertTrue("Expect failure", r2.isFailure())
        Assert.assertTrue("StateMachine state is unchanged", dt1 == s2)
        val dt2 = s2 as DrawTile

        /*
         * odo EndGame request (expect failure: not in position to end game)
         */

        val (r3, s3) = dt2.endGame(odo)
        Assert.assertTrue("Expect failure", r3.isFailure())
        Assert.assertTrue("StateMachine state is unchanged", dt2 == s3)
    }

    @Test
    fun drawOneTile() {
        val (odo, joree, sal) = TestAcqObjs.stdPlayerIds
        val odoTiles = listOf(TileId("1-A"), TileId("1-B"), TileId("1-C"), TileId("1-D"), TileId("1-E"))
        val players = listOf(Players.standardPlayer(PlayerId("odo"), initialTiles = odoTiles),
                             Players.standardPlayer(PlayerId("joree")),
                             Players.standardPlayer(PlayerId("sal")))
        val acqObj = AcqObjs.custom(Tiles.standard(), Banks.standard(), Boards.standard(), Hotels.standard(), players)
        val dt0 = DrawTile(acqObj, TestAcqObjs.stdGameState)

        /*
         * odo DrawTile request
         */

        val (r1, s1) = dt0.drawTile(odo)
        Assert.assertTrue("Expect success", r1.isSuccess())
        Assert.assertTrue("StateMachine state is now PlaceTile", s1 is PlaceTile)
        val pt1 = s1 as PlaceTile

        // ensure state updated
        Assert.assertEquals("Player turn order stayed the same", dt0.gameState.playerTurnOrder.sorted(), pt1.gameState.playerTurnOrder.sorted())
        Assert.assertEquals("CurrentPlayer changed to joree", joree, pt1.gameState.currentPlayer)

        // ensure player updated
        Assert.assertEquals("odo now has six tiles in his hand", 6, pt1.acqObj.player(odo).tiles().size)
        Assert.assertTrue("odo has all the tiles he previously had", odoTiles.all { tile -> pt1.acqObj.player(odo).tiles().contains(tile) })

        // ensure bank updated
        Assert.assertEquals("Bank draw pile reduced by 1", dt0.acqObj.bank.drawPile().size - 1, pt1.acqObj.bank.drawPile().size)
    }

    @Test
    fun drawTwoTiles() {
        val (odo, joree, sal) = TestAcqObjs.stdPlayerIds
        val odoTiles = listOf(TileId("1-A"), TileId("1-B"), TileId("1-C"), TileId("1-D"))
        val players = listOf(Players.standardPlayer(PlayerId("odo"), initialTiles = odoTiles),
                             Players.standardPlayer(PlayerId("joree")),
                             Players.standardPlayer(PlayerId("sal")))
        val acqObj = AcqObjs.custom(Tiles.standard(), Banks.standard(), Boards.standard(), Hotels.standard(), players)
        val dt0 = DrawTile(acqObj, TestAcqObjs.stdGameState)

        /*
         * odo DrawTile request
         */

        val (r1, s1) = dt0.drawTile(odo)
        Assert.assertTrue("Expect success", r1.isSuccess())
        Assert.assertTrue("StateMachine state is now DrawTile", s1 is DrawTile)
        val dt1 = s1 as DrawTile

        // ensure state updated
        Assert.assertEquals("GameState stayed the same", dt0.gameState, dt1.gameState)

        // ensure player updated
        Assert.assertEquals("odo now has five tiles in his hand", 5, dt1.acqObj.player(odo).tiles().size)
        Assert.assertTrue("odo has all the tiles he previously had", odoTiles.all { tile -> dt1.acqObj.player(odo).tiles().contains(tile) })

        // ensure bank updated
        Assert.assertEquals("Bank draw pile reduced by 1", dt0.acqObj.bank.drawPile().size - 1, dt1.acqObj.bank.drawPile().size)
    }

    @Test
    fun emptyDrawPile() {
        val (odo, joree, sal) = TestAcqObjs.stdPlayerIds
        val odoTiles = listOf(TileId("1-A"), TileId("1-B"), TileId("1-C"), TileId("1-D"))
        val players = listOf(Players.standardPlayer(PlayerId("odo"), initialTiles = odoTiles),
                             Players.standardPlayer(PlayerId("joree")),
                             Players.standardPlayer(PlayerId("sal")))
        val acqObj = AcqObjs.custom(Tiles.standard(), Banks.standard().withDrawPile(emptyList()), Boards.standard(), Hotels.standard(), players)
        val dt0 = DrawTile(acqObj, TestAcqObjs.stdGameState)

        /*
         * odo DrawTile request
         */

        val (r1, s1) = dt0.drawTile(odo)
        Assert.assertTrue("Expect success", r1.isSuccess())
        Assert.assertTrue("StateMachine state is now PlaceTile", s1 is PlaceTile)
        val pt1 = s1 as PlaceTile

        // ensure state updated
        Assert.assertEquals("Player turn order stayed the same", dt0.gameState.playerTurnOrder.sorted(), pt1.gameState.playerTurnOrder.sorted())
        Assert.assertEquals("CurrentPlayer changed to joree", joree, pt1.gameState.currentPlayer)

        // ensure player updated
        Assert.assertEquals("odo still has 4 tiles in his hand", 4, pt1.acqObj.player(odo).tiles().size)
        Assert.assertTrue("odo has all the tiles he previously had", odoTiles.all { tile -> pt1.acqObj.player(odo).tiles().contains(tile) })

        // ensure bank updated
        Assert.assertTrue("Bank draw pile is still empty", pt1.acqObj.bank.drawPile().isEmpty())
    }


    @Test
    fun endGameAllSafe() {
        val (odo, joree, sal) = TestAcqObjs.stdPlayerIds
        val hotelItr = Hotels.STANDARD_ID_TIERS.keys.iterator()
        val hotel1 = hotelItr.next()
        val hotel2 = hotelItr.next()
        val hotel1Tiles = listOf(TileId("1-A"), TileId("2-A"), TileId("3-A"), TileId("4-A"), TileId("5-A"),
                                 TileId("6-A"), TileId("7-A"), TileId("8-A"), TileId("9-A"), TileId("10-A"), TileId("11-A"))
        val hotel2Tiles = listOf(TileId("1-C"), TileId("2-C"), TileId("3-C"), TileId("4-C"), TileId("5-C"),
                                 TileId("6-C"), TileId("7-C"), TileId("8-C"), TileId("9-C"), TileId("10-C"), TileId("11-C"))
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
        val dt0 = DrawTile(acqObj, TestAcqObjs.stdGameState)

        /*
         * odo EndGame request
         */

        val (r1, s1) = dt0.endGame(odo)
        Assert.assertTrue("Expect success", r1.isSuccess())
        Assert.assertTrue("State is now EndGamePayout", s1 is EndGamePayout)
        val egp1 = s1 as EndGamePayout

        // confirm state is correct
        Assert.assertTrue("Players paid is empty", egp1.playersPaid.isEmpty())
    }

    @Test
    fun endGameHotelSize() {
        val (odo, joree, sal) = TestAcqObjs.stdPlayerIds
        val hotelItr = Hotels.STANDARD_ID_TIERS.keys.iterator()
        val hotel1 = hotelItr.next()
        val hotel1Tiles = listOf(TileId("1-A"), TileId("2-A"), TileId("3-A"), TileId("4-A"), TileId("5-A"),
                                 TileId("6-A"), TileId("7-A"), TileId("8-A"), TileId("9-A"), TileId("10-A"),
                                 TileId("1-B"), TileId("2-B"), TileId("3-B"), TileId("4-B"), TileId("5-B"),
                                 TileId("6-B"), TileId("7-B"), TileId("8-B"), TileId("9-B"), TileId("10-B"),
                                 TileId("1-C"), TileId("2-C"), TileId("3-C"), TileId("4-C"), TileId("5-C"),
                                 TileId("6-C"), TileId("7-C"), TileId("8-C"), TileId("9-C"), TileId("10-C"),
                                 TileId("1-D"), TileId("2-D"), TileId("3-D"), TileId("4-D"), TileId("5-D"),
                                 TileId("6-D"), TileId("7-D"), TileId("8-D"), TileId("9-D"), TileId("10-D"),
                                 TileId("1-E"))
        val hotels = Hotels.standard().map { h ->
            when {
                h.id() == hotel1 -> h.withState(HotelState.OnBoard(hotel1Tiles))
                else -> h
            }
        }
        val players = listOf(Players.standardPlayer(PlayerId("odo")),
                             Players.standardPlayer(PlayerId("joree")),
                             Players.standardPlayer(PlayerId("sal")))
        val acqObj = AcqObjs.custom(Tiles.standard(), Banks.standard(), Boards.standard(), hotels, players)
        val dt0 = DrawTile(acqObj, TestAcqObjs.stdGameState)

        /*
         * odo EndGame request
         */

        val (r1, s1) = dt0.endGame(odo)
        Assert.assertTrue("Expect success", r1.isSuccess())
        Assert.assertTrue("State is now EndGamePayout", s1 is EndGamePayout)
        val egp1 = s1 as EndGamePayout

        // confirm state is correct
        Assert.assertTrue("Players paid is empty", egp1.playersPaid.isEmpty())
    }
}