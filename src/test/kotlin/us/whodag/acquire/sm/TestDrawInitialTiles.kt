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

class TestDrawInitialTiles {

    @Test
    fun all() {
        val (odo, joree, sal) = TestAcqObjs.stdPlayerIds
        val acqObj = AcqObjs.custom(Tiles.standard(), Banks.standard(), Boards.standard(), Hotels.standard(), Players.standard(TestAcqObjs.stdPlayerIds))
        val dit0 = DrawInitialTiles(acqObj, TestAcqObjs.stdPlayerIds, emptyList())

        /*
         * DrawInitialTiles request for odo
         */

        val (r1, s1) = dit0.drawInitialTiles(odo)
        Assert.assertTrue("Successful response", r1.isSuccess())
        Assert.assertTrue("State is DrawInitialTiles", s1 is DrawInitialTiles)
        val dit1 = s1 as DrawInitialTiles

        // confirm DrawInitialTiles state updated correctly
        Assert.assertTrue("odo was added as a player who drew initial tiles", dit1.playersDrawn.contains(odo))
        Assert.assertEquals("Only odo is a player who drew initial tiles", 1, dit1.playersDrawn.size)

        // confirm the correct tile was drawn, the draw pile, and player updated correctly
        Assert.assertEquals("odo has 6 tiles in hand", 6, dit1.acqObj.player(odo).tiles().size)
        Assert.assertEquals("The draw pile decreased in size by 6", dit0.acqObj.bank.drawPile().size - 6, dit1.acqObj.bank.drawPile().size)
        Assert.assertTrue("Draw pile does not contain any of the drawn tile", !dit1.acqObj.bank.drawPile().any { t -> dit1.acqObj.player(odo).tiles().contains(t) })

        // confirm tile state changed
        Assert.assertTrue("Tile states changed to player hand", dit1.acqObj.player(odo).tiles().all { t -> dit1.acqObj.tile(t).state() is TileState.PlayerHand })
        Assert.assertEquals("Tile state is in odo's hand", odo, (dit1.acqObj.tile(dit1.acqObj.player(odo).tiles().iterator().next()).state() as TileState.PlayerHand).player)

        /*
         * Attempt another DrawInitialTiles request for odo (expect failure)
         */

        val (r2, s2) = dit1.drawInitialTiles(odo)
        Assert.assertTrue("Failure response", r2.isFailure())
        Assert.assertTrue("State is the same pre-request", s1 == s2)
        val dit2 = s2 as DrawInitialTiles

        /*
         * DrawInitialTiles request for joree
         */

        val (r3, s3) = dit2.drawInitialTiles(joree)
        Assert.assertTrue("Successful response", r3.isSuccess())
        Assert.assertTrue("State is DrawInitialTiles", s3 is DrawInitialTiles)
        val dit3 = s3 as DrawInitialTiles

        // confirm DrawInitialTiles state updated correctly
        Assert.assertTrue("joree was added as a player who drew initial tiles", dit3.playersDrawn.contains(joree))
        Assert.assertEquals("odo and joree have drawn initial tiles", 2, dit3.playersDrawn.size)

        // confirm the correct tile was drawn, the draw pile, and player updated correctly
        Assert.assertEquals("joree has 6 tiles in hand", 6, dit3.acqObj.player(joree).tiles().size)
        Assert.assertEquals("The draw pile decreased in size by 6", dit2.acqObj.bank.drawPile().size - 6, dit3.acqObj.bank.drawPile().size)
        Assert.assertTrue("Draw pile does not contain any of the drawn tile", !dit3.acqObj.bank.drawPile().any { t -> dit3.acqObj.player(joree).tiles().contains(t) })

        // confirm tile state changed
        Assert.assertTrue("Tile states changed to player hand", dit3.acqObj.player(joree).tiles().all { t -> dit3.acqObj.tile(t).state() is TileState.PlayerHand })
        Assert.assertEquals("Tile state is in joree's hand", joree, (dit3.acqObj.tile(dit3.acqObj.player(joree).tiles().iterator().next()).state() as TileState.PlayerHand).player)

        /*
         * DrawInitialTiles request for sal
         */

        val (r4, s4) = dit3.drawInitialTiles(sal)
        Assert.assertTrue("Successful response", r4.isSuccess())
        Assert.assertTrue("State is PlaceTile", s4 is PlaceTile)
        val pt4 = s4 as PlaceTile

        // confirm PlaceTile state updated correctly
        Assert.assertEquals("PlaceTile current player is odo", odo, pt4.gameState.currentPlayer)

        // confirm the correct tile was drawn, the draw pile, and player updated correctly
        Assert.assertEquals("sal has 6 tiles in hand", 6, pt4.acqObj.player(sal).tiles().size)
        Assert.assertEquals("The draw pile decreased in size by 6", dit3.acqObj.bank.drawPile().size - 6, pt4.acqObj.bank.drawPile().size)
        Assert.assertTrue("Draw pile does not contain any of the drawn tile", !pt4.acqObj.bank.drawPile().any { t -> pt4.acqObj.player(sal).tiles().contains(t) })

        // confirm tile state changed
        Assert.assertTrue("Tile states changed to player hand", pt4.acqObj.player(sal).tiles().all { t -> pt4.acqObj.tile(t).state() is TileState.PlayerHand })
        Assert.assertEquals("Tile state is in sal's hand", sal, (pt4.acqObj.tile(pt4.acqObj.player(sal).tiles().iterator().next()).state() as TileState.PlayerHand).player)
    }
}