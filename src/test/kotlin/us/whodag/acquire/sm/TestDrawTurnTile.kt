package us.whodag.acquire.sm

import org.junit.Assert
import org.junit.Test
import us.whodag.acquire.obj.AcqObjs
import us.whodag.acquire.TestAcqObjs
import us.whodag.acquire.req.isFailure
import us.whodag.acquire.req.isSuccess
import us.whodag.acquire.obj.bank.Banks
import us.whodag.acquire.obj.board.Boards
import us.whodag.acquire.obj.hotel.Hotels
import us.whodag.acquire.obj.player
import us.whodag.acquire.obj.player.Players
import us.whodag.acquire.obj.tile
import us.whodag.acquire.obj.tile.TileState
import us.whodag.acquire.obj.tile.Tiles

class TestDrawTurnTile {

    @Test
    fun all() {
        val (odo, joree, sal) = TestAcqObjs.stdPlayerIds
        val acqObj = AcqObjs.custom(Tiles.standard(), Banks.standard(), Boards.standard(), Hotels.standard(), Players.standard(TestAcqObjs.stdPlayerIds))
        val dtt0 = DrawTurnTile(acqObj, emptyList())

        /*
         * DrawTurnTile request for odo
         */

        val (r1, s1) = dtt0.drawTurnTile(odo)
        Assert.assertTrue("Successful response", r1.isSuccess())
        Assert.assertTrue("State is DrawTurnTile", s1 is DrawTurnTile)
        val dtt1 = s1 as DrawTurnTile

        // confirm DrawTurnTile state updated correctly
        Assert.assertTrue("odo was added as a player who drew a turn tile", dtt1.playersDrawn.contains(odo))
        Assert.assertEquals("Only odo is a player who drew a turn tile", 1, dtt1.playersDrawn.size)

        // confirm the correct tile was drawn, the draw pile, and player updated correctly
        Assert.assertEquals("odo has a tile in hand", 1, dtt1.acqObj.player(odo).tiles().size)
        val odoTileToDraw = dtt0.acqObj.bank.drawPile().last()
        val odoTileDrawn = dtt1.acqObj.player(odo).tiles().iterator().next()
        Assert.assertEquals("The top tile in the draw pile equals the tile drawn", odoTileToDraw, odoTileDrawn)
        Assert.assertEquals("The draw pile decreased in size", dtt0.acqObj.bank.drawPile().size - 1, dtt1.acqObj.bank.drawPile().size)
        Assert.assertTrue("Draw pile does not contain drawn tile", !dtt1.acqObj.bank.drawPile().contains(odoTileDrawn))

        // confirm tile state changed
        Assert.assertTrue("Tile state changed to player hand", dtt1.acqObj.tile(odoTileDrawn).state() is TileState.PlayerHand)
        val odoTileStatePlayerHand = dtt1.acqObj.tile(odoTileDrawn).state() as TileState.PlayerHand
        Assert.assertEquals("Tile state is in odo's hand", odo, odoTileStatePlayerHand.player)

        /*
         * Attempt another DrawTurnTile request for odo (expect failure)
         */

        val (r2, s2) = dtt1.drawTurnTile(odo)
        Assert.assertTrue("Failure response", r2.isFailure())
        Assert.assertTrue("State is the same pre-request", s1 == s2)
        val dtt2 = s2 as DrawTurnTile

        /*
         * DrawTurnTile request for joree
         */

        val (r3, s3) = dtt2.drawTurnTile(joree)
        Assert.assertTrue("Successful response", r3.isSuccess())
        Assert.assertTrue("State is DrawTurnTile", s3 is DrawTurnTile)
        val dtt3 = s3 as DrawTurnTile

        // confirm DrawTurnTile state updated correctly
        Assert.assertTrue("joree was added as a player who drew a turn tile", dtt3.playersDrawn.contains(joree))
        Assert.assertEquals("odo and joree have drawn turn tiles", 2, dtt3.playersDrawn.size)

        // confirm the correct tile was drawn, the draw pile, and player updated correctly
        Assert.assertEquals("joree has a tile in hand", 1, dtt3.acqObj.player(joree).tiles().size)
        val joreeTileToDraw = dtt2.acqObj.bank.drawPile().last()
        val joreeTileDrawn = dtt3.acqObj.player(joree).tiles().iterator().next()
        Assert.assertEquals("The top tile in the draw pile equals the tile drawn", joreeTileToDraw, joreeTileDrawn)
        Assert.assertEquals("The draw pile decreased in size", dtt2.acqObj.bank.drawPile().size - 1, dtt3.acqObj.bank.drawPile().size)
        Assert.assertTrue("Draw pile does not contain drawn tile", !dtt3.acqObj.bank.drawPile().contains(joreeTileDrawn))

        // confirm tile state changed
        Assert.assertTrue("Tile state changed to player hand", dtt3.acqObj.tile(joreeTileDrawn).state() is TileState.PlayerHand)
        val joreeTileStatePlayerHand = dtt3.acqObj.tile(joreeTileDrawn).state() as TileState.PlayerHand
        Assert.assertEquals("Tile state is in joree's hand", joree, joreeTileStatePlayerHand.player)

        /*
         * DrawTurnTile request for sal
         */

        val (r4, s4) = dtt3.drawTurnTile(sal)
        Assert.assertTrue("Successful response", r4.isSuccess())
        Assert.assertTrue("State is PlaceTurnTile", s4 is PlaceTurnTile)
        val ptt4 = s4 as PlaceTurnTile

        // confirm PlaceTurnTile state updated correctly
        Assert.assertTrue("PlaceTurnTile state is empty", ptt4.playersPlaced.isEmpty())

        // confirm the correct tile was drawn, the draw pile, and player updated correctly
        Assert.assertEquals("sal has a tile in hand", 1, ptt4.acqObj.player(sal).tiles().size)
        val salTileToDraw = dtt3.acqObj.bank.drawPile().last()
        val salTileDrawn = ptt4.acqObj.player(sal).tiles().iterator().next()
        Assert.assertEquals("The top tile in the draw pile equals the tile drawn", salTileToDraw, salTileDrawn)
        Assert.assertEquals("The draw pile decreased in size", dtt3.acqObj.bank.drawPile().size - 1, ptt4.acqObj.bank.drawPile().size)
        Assert.assertTrue("Draw pile does not contain drawn tile", !ptt4.acqObj.bank.drawPile().contains(salTileDrawn))

        // confirm tile state changed
        Assert.assertTrue("Tile state changed to player hand", ptt4.acqObj.tile(salTileDrawn).state() is TileState.PlayerHand)
        val salTileStatePlayerHand = ptt4.acqObj.tile(salTileDrawn).state() as TileState.PlayerHand
        Assert.assertEquals("Tile state is in sal's hand", sal, salTileStatePlayerHand.player)
    }
}