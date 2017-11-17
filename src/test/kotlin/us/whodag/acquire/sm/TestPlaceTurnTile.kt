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
import us.whodag.acquire.obj.player.PlayerId
import us.whodag.acquire.obj.player.Players
import us.whodag.acquire.obj.tile
import us.whodag.acquire.obj.tile.TileId
import us.whodag.acquire.obj.tile.TileState
import us.whodag.acquire.obj.tile.Tiles

class TestPlaceTurnTile {

    @Test
    fun allAdjacent() {
        val (odo, joree, sal) = TestAcqObjs.stdPlayerIds
        val (odoTile, joreeTile, salTile) = listOf("1-A", "1-B", "1-C").map { n -> TileId(n) }
        val players = listOf(Players.standardPlayer(PlayerId("odo"), initialTiles = listOf(odoTile)),
                                                    Players.standardPlayer(PlayerId("joree"), initialTiles = listOf(joreeTile)),
                                                    Players.standardPlayer(PlayerId("sal"), initialTiles = listOf(salTile)))
        val acqObj = AcqObjs.custom(Tiles.standard(), Banks.standard(), Boards.standard(), Hotels.standard(), players)
        val ptt0 = PlaceTurnTile(acqObj, emptyMap())

        /*
         * odo placing a tile not in hand (failure)
         */

        val (r1, s1) = ptt0.placeTurnTile(odo, joreeTile)
        Assert.assertTrue("Expect failure", r1.isFailure())
        Assert.assertEquals("State equals state before failed command", ptt0, s1)
        val ptt1 = s1 as PlaceTurnTile

        /*
         * odo places turn tile
         */

        val (r2, s2) = ptt1.placeTurnTile(odo, odoTile)
        Assert.assertTrue("Expect success", r2.isSuccess())
        Assert.assertTrue("State is DrawTurnTile", s2 is PlaceTurnTile)
        val ptt2 = s2 as PlaceTurnTile

        // confirm PlaceTurnTile state updated correctly
        Assert.assertTrue("odo was added as a player who placed a turn tile", ptt2.playersPlaced.containsKey(odo))
        Assert.assertEquals("odo's placed tile was the tile placed", odoTile, ptt2.playersPlaced[odo]!!)
        Assert.assertEquals("Only odo is a player who placed a turn tile", 1, ptt2.playersPlaced.size)

        // confirm the tile was placed on the board and player updated correctly
        Assert.assertTrue("odo no longer has a tile in hand", ptt2.acqObj.player(odo).tiles().isEmpty())
        Assert.assertTrue("The board now contains the placed tile", ptt2.acqObj.board.containsTile(odoTile))
        Assert.assertTrue("The placed tile has no neighbors", ptt2.acqObj.board.adjacentTiles(odoTile).isEmpty())
        Assert.assertEquals("The placed tile is in a group by itself", 1, ptt2.acqObj.board.connectedTiles(odoTile).size)
        Assert.assertTrue("The placed tile is in its tile group", ptt2.acqObj.board.connectedTiles(odoTile).contains(odoTile))

        // confirm tile state changed
        Assert.assertTrue("Tile state changed to OnBoard", ptt2.acqObj.tile(odoTile).state() is TileState.OnBoard)

        /*
         * odo tries placing a tile again (failure)
         */

        val (r3, s3) = ptt2.placeTurnTile(odo, odoTile)
        Assert.assertTrue("Expect failure", r3.isFailure())
        Assert.assertTrue("State equals state before failed command", ptt2 == s3)
        val ptt3 = s3 as PlaceTurnTile

        /*
         * joree places turn tile
         */

        val (r4, s4) = ptt3.placeTurnTile(joree, joreeTile)
        Assert.assertTrue("Expect success", r4.isSuccess())
        Assert.assertTrue("State is PlaceTurnTile", s4 is PlaceTurnTile)
        val ptt4 = s4 as PlaceTurnTile

        // confirm PlaceTurnTile state updated correctly
        Assert.assertTrue("joree was added as a player who placed a turn tile", ptt4.playersPlaced.containsKey(joree))
        Assert.assertEquals("joree's placed tile was the tile placed", joreeTile, ptt4.playersPlaced[joree]!!)
        Assert.assertEquals("Only joree is a player who placed a turn tile", 2, ptt4.playersPlaced.size)

        // confirm the tile was placed on the board and player updated correctly
        Assert.assertTrue("joree no longer has a tile in hand", ptt4.acqObj.player(joree).tiles().isEmpty())
        Assert.assertTrue("The board now contains the placed tile", ptt4.acqObj.board.containsTile(joreeTile))
        // adjacency
        Assert.assertEquals("The placed tile has one neighbor", 1, ptt4.acqObj.board.adjacentTiles(joreeTile).size)
        Assert.assertTrue("The placed tile's neighbor is odo's tile", ptt4.acqObj.board.adjacentTiles(joreeTile).contains(odoTile))
        Assert.assertEquals("odo's tile has one neighbor", 1, ptt4.acqObj.board.adjacentTiles(odoTile).size)
        Assert.assertTrue("odo's tile's neighbor is joree's tile", ptt4.acqObj.board.adjacentTiles(odoTile).contains(joreeTile))
        // tile group
        Assert.assertEquals("The placed tile group contains two tiles", 2, ptt4.acqObj.board.connectedTiles(joreeTile).size)
        Assert.assertEquals("odo's tile group contains two tiles", 2, ptt4.acqObj.board.connectedTiles(odoTile).size)
        Assert.assertTrue("odo's tile is in his tile group", ptt4.acqObj.board.connectedTiles(odoTile).contains(odoTile))
        Assert.assertTrue("joree's tile is in odo's tile group", ptt4.acqObj.board.connectedTiles(odoTile).contains(joreeTile))
        Assert.assertTrue("joree's tile is in his tile group", ptt4.acqObj.board.connectedTiles(joreeTile).contains(joreeTile))
        Assert.assertTrue("odo's tile is in joree's tile group", ptt4.acqObj.board.connectedTiles(joreeTile).contains(odoTile))

        // confirm tile state changed
        Assert.assertTrue("Tile state changed to OnBoard", ptt4.acqObj.tile(joreeTile).state() is TileState.OnBoard)

        /*
         * sal places turn tile
         */

        val (r5, s5) = ptt4.placeTurnTile(sal, salTile)
        Assert.assertTrue("Expect success", r4.isSuccess())
        Assert.assertTrue("State is DrawInitialTiles", s5 is DrawInitialTiles)
        val dit5 = s5 as DrawInitialTiles

        // confirm DrawInitialTiles state updated correctly
        Assert.assertTrue("Players drawn is empty", dit5.playersDrawn.isEmpty())
        Assert.assertEquals("Player turn order first is odo", odo, dit5.playerTurnOrder[0])
        Assert.assertEquals("Player turn order second is joree", joree, dit5.playerTurnOrder[1])
        Assert.assertEquals("Player turn order second is sal", sal, dit5.playerTurnOrder[2])

        // confirm the tile was placed on the board and player updated correctly
        Assert.assertTrue("sal no longer has a tile in hand", dit5.acqObj.player(sal).tiles().isEmpty())
        Assert.assertTrue("The board now contains the placed tile", dit5.acqObj.board.containsTile(salTile))
        // adjacency
        Assert.assertEquals("The placed tile has one neighbor", 1, dit5.acqObj.board.adjacentTiles(salTile).size)
        Assert.assertTrue("The placed tile's neighbors contain joree's tile", dit5.acqObj.board.adjacentTiles(salTile).contains(joreeTile))
        Assert.assertEquals("odo's placed tile has one neighbor", 1, dit5.acqObj.board.adjacentTiles(odoTile).size)
        Assert.assertTrue("odo's placed tile's neighbors contain joree's tile", dit5.acqObj.board.adjacentTiles(odoTile).contains(joreeTile))
        Assert.assertEquals("joree's placed tile has two neighbors", 2, dit5.acqObj.board.adjacentTiles(joreeTile).size)
        Assert.assertTrue("joree's placed tile's neighbors contain odo's tile", dit5.acqObj.board.adjacentTiles(joreeTile).contains(odoTile))
        Assert.assertTrue("joree's placed tile's neighbors contain sals's tile", dit5.acqObj.board.adjacentTiles(joreeTile).contains(salTile))
        // tile group
        Assert.assertEquals("The placed tile group contains three tiles", 3, dit5.acqObj.board.connectedTiles(salTile).size)
        Assert.assertEquals("odo's tile group contains three tiles", 3, dit5.acqObj.board.connectedTiles(odoTile).size)
        Assert.assertEquals("joree's tile group contains three tiles", 3, dit5.acqObj.board.connectedTiles(joreeTile).size)
        Assert.assertTrue("sal's tile is in his tile group", dit5.acqObj.board.connectedTiles(salTile).contains(salTile))
        Assert.assertTrue("odo's tile is in sal's tile group", dit5.acqObj.board.connectedTiles(salTile).contains(odoTile))
        Assert.assertTrue("joree's tile is in sal's tile group", dit5.acqObj.board.connectedTiles(salTile).contains(joreeTile))
        Assert.assertTrue("odo's tile is in his tile group", dit5.acqObj.board.connectedTiles(odoTile).contains(odoTile))
        Assert.assertTrue("sal's tile is in odo's tile group", dit5.acqObj.board.connectedTiles(odoTile).contains(salTile))
        Assert.assertTrue("joree's tile is in odo's tile group", dit5.acqObj.board.connectedTiles(odoTile).contains(joreeTile))
        Assert.assertTrue("joree's tile is in his tile group", dit5.acqObj.board.connectedTiles(joreeTile).contains(joreeTile))
        Assert.assertTrue("odo's tile is in joree's tile group", dit5.acqObj.board.connectedTiles(joreeTile).contains(odoTile))
        Assert.assertTrue("sal's tile is in joree's tile group", dit5.acqObj.board.connectedTiles(joreeTile).contains(salTile))

        // confirm tile state changed
        Assert.assertTrue("Tile state changed to OnBoard", dit5.acqObj.tile(salTile).state() is TileState.OnBoard)
    }

    @Test
    fun twoAdjacent() {
        val (odo, joree, sal) = TestAcqObjs.stdPlayerIds
        val (odoTile, joreeTile, salTile) = listOf("1-A", "1-B", "1-D").map { n -> TileId(n) }
        val players = listOf(Players.standardPlayer(PlayerId("odo"), initialTiles = listOf(odoTile)),
                             Players.standardPlayer(PlayerId("joree"), initialTiles = listOf(joreeTile)),
                             Players.standardPlayer(PlayerId("sal"), initialTiles = listOf(salTile)))
        val acqObj = AcqObjs.custom(Tiles.standard(), Banks.standard(), Boards.standard(), Hotels.standard(), players)
        val ptt0 = PlaceTurnTile(acqObj, emptyMap())

        /*
         * odo places turn tile
         */

        val (r1, s1) = ptt0.placeTurnTile(odo, odoTile)
        Assert.assertTrue("Expect success", r1.isSuccess())
        Assert.assertTrue("State is DrawTurnTile", s1 is PlaceTurnTile)
        val ptt1 = s1 as PlaceTurnTile

        // confirm the tile was placed on the board and player updated correctly
        Assert.assertTrue("The board now contains the placed tile", ptt1.acqObj.board.containsTile(odoTile))
        Assert.assertTrue("The placed tile has no neighbors", ptt1.acqObj.board.adjacentTiles(odoTile).isEmpty())
        Assert.assertEquals("The placed tile is in a group by itself", 1, ptt1.acqObj.board.connectedTiles(odoTile).size)
        Assert.assertTrue("The placed tile is in its tile group", ptt1.acqObj.board.connectedTiles(odoTile).contains(odoTile))

        // confirm tile state changed
        Assert.assertTrue("Tile state changed to OnBoard", ptt1.acqObj.tile(odoTile).state() is TileState.OnBoard)

        /*
         * joree places turn tile
         */

        val (r2, s2) = ptt1.placeTurnTile(joree, joreeTile)
        Assert.assertTrue("Expect success", r2.isSuccess())
        Assert.assertTrue("State is PlaceTurnTile", s2 is PlaceTurnTile)
        val ptt2 = s2 as PlaceTurnTile

        // confirm the tile was placed on the board and player updated correctly
        Assert.assertTrue("The board now contains the placed tile", ptt2.acqObj.board.containsTile(joreeTile))
        // adjacency
        Assert.assertEquals("The placed tile has one neighbor", 1, ptt2.acqObj.board.adjacentTiles(joreeTile).size)
        Assert.assertTrue("The placed tile's neighbor is odo's tile", ptt2.acqObj.board.adjacentTiles(joreeTile).contains(odoTile))
        Assert.assertEquals("odo's tile has one neighbor", 1, ptt2.acqObj.board.adjacentTiles(odoTile).size)
        Assert.assertTrue("odo's tile's neighbor is joree's tile", ptt2.acqObj.board.adjacentTiles(odoTile).contains(joreeTile))
        // tile group
        Assert.assertEquals("The placed tile group contains two tiles", 2, ptt2.acqObj.board.connectedTiles(joreeTile).size)
        Assert.assertEquals("odo's tile group contains two tiles", 2, ptt2.acqObj.board.connectedTiles(odoTile).size)
        Assert.assertTrue("odo's tile is in his tile group", ptt2.acqObj.board.connectedTiles(odoTile).contains(odoTile))
        Assert.assertTrue("joree's tile is in odo's tile group", ptt2.acqObj.board.connectedTiles(odoTile).contains(joreeTile))
        Assert.assertTrue("joree's tile is in his tile group", ptt2.acqObj.board.connectedTiles(joreeTile).contains(joreeTile))
        Assert.assertTrue("odo's tile is in joree's tile group", ptt2.acqObj.board.connectedTiles(joreeTile).contains(odoTile))

        // confirm tile state changed
        Assert.assertTrue("Tile state changed to OnBoard", ptt2.acqObj.tile(joreeTile).state() is TileState.OnBoard)

        /*
         * sal places turn tile
         */

        val (r3, s3) = ptt2.placeTurnTile(sal, salTile)
        Assert.assertTrue("Expect success", r3.isSuccess())
        Assert.assertTrue("State is DrawInitialTiles", s3 is DrawInitialTiles)
        val dit3 = s3 as DrawInitialTiles

        // confirm the tile was placed on the board and player updated correctly
        Assert.assertTrue("The board now contains the placed tile", dit3.acqObj.board.containsTile(salTile))
        // adjacency
        Assert.assertEquals("The placed tile has no neighbors", 0, dit3.acqObj.board.adjacentTiles(salTile).size)
        Assert.assertEquals("odo's placed tile has one neighbor", 1, dit3.acqObj.board.adjacentTiles(odoTile).size)
        Assert.assertTrue("odo's placed tile's neighbors contains joree's tile", dit3.acqObj.board.adjacentTiles(odoTile).contains(joreeTile))
        Assert.assertEquals("joree's placed tile has one neighbor", 1, dit3.acqObj.board.adjacentTiles(joreeTile).size)
        Assert.assertTrue("joree's placed tile's neighbors contain odo's tile", dit3.acqObj.board.adjacentTiles(joreeTile).contains(odoTile))
        // tile group
        Assert.assertEquals("The placed tile group contains one tile", 1, dit3.acqObj.board.connectedTiles(salTile).size)
        Assert.assertEquals("odo's tile group contains two tiles", 2, dit3.acqObj.board.connectedTiles(odoTile).size)
        Assert.assertEquals("joree's tile group contains two tiles", 2, dit3.acqObj.board.connectedTiles(joreeTile).size)
        Assert.assertTrue("sal's tile is in his tile group", dit3.acqObj.board.connectedTiles(salTile).contains(salTile))
        Assert.assertTrue("odo's tile is in his tile group", dit3.acqObj.board.connectedTiles(odoTile).contains(odoTile))
        Assert.assertTrue("joree's tile is in odo's tile group", dit3.acqObj.board.connectedTiles(odoTile).contains(joreeTile))
        Assert.assertTrue("joree's tile is in his tile group", dit3.acqObj.board.connectedTiles(joreeTile).contains(joreeTile))
        Assert.assertTrue("odo's tile is in joree's tile group", dit3.acqObj.board.connectedTiles(joreeTile).contains(odoTile))

        // confirm tile state changed
        Assert.assertTrue("Tile state changed to OnBoard", dit3.acqObj.tile(salTile).state() is TileState.OnBoard)
    }

    @Test
    fun noAdjacent() {
        val (odo, joree, sal) = TestAcqObjs.stdPlayerIds
        val (odoTile, joreeTile, salTile) = listOf("1-A", "1-C", "1-E").map { n -> TileId(n) }
        val players = listOf(Players.standardPlayer(PlayerId("odo"), initialTiles = listOf(odoTile)),
                             Players.standardPlayer(PlayerId("joree"), initialTiles = listOf(joreeTile)),
                             Players.standardPlayer(PlayerId("sal"), initialTiles = listOf(salTile)))
        val acqObj = AcqObjs.custom(Tiles.standard(), Banks.standard(), Boards.standard(), Hotels.standard(), players)
        val ptt0 = PlaceTurnTile(acqObj, emptyMap())

        /*
         * odo places turn tile
         */

        val (r1, s1) = ptt0.placeTurnTile(odo, odoTile)
        Assert.assertTrue("Expect success", r1.isSuccess())
        Assert.assertTrue("State is DrawTurnTile", s1 is PlaceTurnTile)
        val ptt1 = s1 as PlaceTurnTile

        // confirm the tile was placed on the board and player updated correctly
        Assert.assertTrue("The board now contains the placed tile", ptt1.acqObj.board.containsTile(odoTile))
        Assert.assertTrue("The placed tile has no neighbors", ptt1.acqObj.board.adjacentTiles(odoTile).isEmpty())
        Assert.assertEquals("The placed tile is in a group by itself", 1, ptt1.acqObj.board.connectedTiles(odoTile).size)
        Assert.assertTrue("The placed tile is in its tile group", ptt1.acqObj.board.connectedTiles(odoTile).contains(odoTile))

        // confirm tile state changed
        Assert.assertTrue("Tile state changed to OnBoard", ptt1.acqObj.tile(odoTile).state() is TileState.OnBoard)

        /*
         * joree places turn tile
         */

        val (r2, s2) = ptt1.placeTurnTile(joree, joreeTile)
        Assert.assertTrue("Expect success", r2.isSuccess())
        Assert.assertTrue("State is PlaceTurnTile", s2 is PlaceTurnTile)
        val ptt2 = s2 as PlaceTurnTile

        // confirm the tile was placed on the board and player updated correctly
        Assert.assertTrue("The board now contains the placed tile", ptt2.acqObj.board.containsTile(joreeTile))
        // adjacency
        Assert.assertEquals("The placed tile has no neighbor", 0, ptt2.acqObj.board.adjacentTiles(joreeTile).size)
        Assert.assertEquals("odo's tile has no neighbor", 0, ptt2.acqObj.board.adjacentTiles(odoTile).size)
        // tile group
        Assert.assertEquals("The placed tile group contains itself", 1, ptt2.acqObj.board.connectedTiles(joreeTile).size)
        Assert.assertEquals("odo's tile group contains one tile", 1, ptt2.acqObj.board.connectedTiles(odoTile).size)
        Assert.assertTrue("odo's tile is in his tile group", ptt2.acqObj.board.connectedTiles(odoTile).contains(odoTile))
        Assert.assertTrue("joree's tile is in his tile group", ptt2.acqObj.board.connectedTiles(joreeTile).contains(joreeTile))

        // confirm tile state changed
        Assert.assertTrue("Tile state changed to OnBoard", ptt2.acqObj.tile(joreeTile).state() is TileState.OnBoard)

        /*
         * sal places turn tile
         */

        val (r3, s3) = ptt2.placeTurnTile(sal, salTile)
        Assert.assertTrue("Expect success", r3.isSuccess())
        Assert.assertTrue("State is DrawInitialTiles", s3 is DrawInitialTiles)
        val dit3 = s3 as DrawInitialTiles

        // confirm the tile was placed on the board and player updated correctly
        Assert.assertTrue("The board now contains the placed tile", dit3.acqObj.board.containsTile(salTile))
        // adjacency
        Assert.assertEquals("The placed tile has no neighbors", 0, dit3.acqObj.board.adjacentTiles(salTile).size)
        Assert.assertEquals("odo's placed tile has no neighbor", 0, dit3.acqObj.board.adjacentTiles(odoTile).size)
        Assert.assertEquals("joree's placed tile has no neighbor", 0, dit3.acqObj.board.adjacentTiles(joreeTile).size)
        // tile group
        Assert.assertEquals("The placed tile group contains one tile", 1, dit3.acqObj.board.connectedTiles(salTile).size)
        Assert.assertEquals("odo's tile group contains one tile", 1, dit3.acqObj.board.connectedTiles(odoTile).size)
        Assert.assertEquals("joree's tile group contains one tile", 1, dit3.acqObj.board.connectedTiles(joreeTile).size)
        Assert.assertTrue("sal's tile is in his tile group", dit3.acqObj.board.connectedTiles(salTile).contains(salTile))
        Assert.assertTrue("odo's tile is in his tile group", dit3.acqObj.board.connectedTiles(odoTile).contains(odoTile))
        Assert.assertTrue("joree's tile is in his tile group", dit3.acqObj.board.connectedTiles(joreeTile).contains(joreeTile))

        // confirm tile state changed
        Assert.assertTrue("Tile state changed to OnBoard", dit3.acqObj.tile(salTile).state() is TileState.OnBoard)
    }
}