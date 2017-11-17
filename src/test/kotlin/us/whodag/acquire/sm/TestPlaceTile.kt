package us.whodag.acquire.sm

import org.junit.Assert
import org.junit.Test
import us.whodag.acquire.*
import us.whodag.acquire.obj.AcqObjs
import us.whodag.acquire.obj.bank.Banks
import us.whodag.acquire.obj.board.Boards
import us.whodag.acquire.obj.board.Boards.withTile
import us.whodag.acquire.obj.board.Boards.withTiles
import us.whodag.acquire.obj.hotel
import us.whodag.acquire.obj.hotel.HotelState
import us.whodag.acquire.obj.hotel.Hotels
import us.whodag.acquire.obj.player
import us.whodag.acquire.obj.player.PlayerId
import us.whodag.acquire.obj.player.Players
import us.whodag.acquire.obj.tile
import us.whodag.acquire.obj.tile.TileId
import us.whodag.acquire.obj.tile.TileState
import us.whodag.acquire.obj.tile.Tiles
import us.whodag.acquire.req.isFailure
import us.whodag.acquire.req.isSuccess

class TestPlaceTile {

    @Test
    fun invalid() {
        val (odo, joree, sal) = TestAcqObjs.stdPlayerIds
        val odoTile = TileId("1-A")
        val players = listOf(Players.standardPlayer(PlayerId("odo"), initialTiles = listOf(odoTile)),
                                                    Players.standardPlayer(PlayerId("joree")),
                                                    Players.standardPlayer(PlayerId("sal")))
        val acqObj = AcqObjs.custom(Tiles.standard(), Banks.standard(), Boards.standard(), Hotels.standard(), players)
        val pt0 = PlaceTile(acqObj, TestAcqObjs.stdGameState)

        /*
         * sal PlaceTile request (expect failure: not current player)
         */

        val (r1, s1) = pt0.placeTile(sal, odoTile)
        Assert.assertTrue("Expect failure", r1.isFailure())
        Assert.assertTrue("StateMachine state is unchanged", pt0 == s1)
        val pt1 = s1 as PlaceTile

        /*
         * odo PlaceTile request (expect failure: tile not in hand)
         */

        val (r2, s2) = pt1.placeTile(odo, TileId("2-A"))
        Assert.assertTrue("Expect failure", r2.isFailure())
        Assert.assertTrue("StateMachine state is unchanged", pt1 == s2)
        val pt2 = s2 as PlaceTile

        /*
         * sal EndGame request (expect failure: not current player)
         */

        val (r3, s3) = pt2.endGame(sal)
        Assert.assertTrue("Expect failure", r3.isFailure())
        Assert.assertTrue("StateMachine state is unchanged", pt2 == s3)
        val pt3 = s3 as PlaceTile

        /*
         * odo EndGame request (expect failure: not in position to end game)
         */

        val (r4, s4) = pt3.endGame(odo)
        Assert.assertTrue("Expect failure", r4.isFailure())
        Assert.assertTrue("StateMachine state is unchanged", pt3 == s4)
    }

    @Test
    fun noNearby() {
        val (odo, joree, sal) = TestAcqObjs.stdPlayerIds
        val odoTile = TileId("1-A")
        val players = listOf(Players.standardPlayer(PlayerId("odo"), initialTiles = listOf(odoTile)),
                             Players.standardPlayer(PlayerId("joree")),
                             Players.standardPlayer(PlayerId("sal")))
        val acqObj = AcqObjs.custom(Tiles.standard(), Banks.standard(), Boards.standard(), Hotels.standard(), players)
        val pt0 = PlaceTile(acqObj, TestAcqObjs.stdGameState)

        /*
         * odo PlaceTile request
         */

        val (r1, s1) = pt0.placeTile(odo, odoTile)
        Assert.assertTrue("Expect success", r1.isSuccess())
        Assert.assertTrue("State is now DrawTile", s1 is DrawTile)
        val dt1 = s1 as DrawTile

        // confirm state is correct
        Assert.assertEquals("GameStates are equal", pt0.gameState, dt1.gameState)

        // confirm player updated correctly
        Assert.assertTrue("Tile is no longer in odo's hand", dt1.acqObj.player(odo).tiles().isEmpty())

        // confirm board update correctly
        Assert.assertTrue("Tile is now on the board", dt1.acqObj.board.containsTile(odoTile))
        Assert.assertEquals("Tile is in a tile group by itself", 1, dt1.acqObj.board.connectedTiles(odoTile).size)
        Assert.assertTrue("Tile's tile group only contains itself", dt1.acqObj.board.connectedTiles(odoTile).contains(odoTile))
        Assert.assertTrue("Tile does not have any neighbors", dt1.acqObj.board.adjacentTiles(odoTile).isEmpty())

        // confirm tile state updated correctly
        Assert.assertTrue("Tile state is OnBoard", dt1.acqObj.tile(odoTile).state() is TileState.OnBoard)
    }

    @Test
    fun nearbyTileHotelsAvailable() {
        val (odo, joree, sal) = TestAcqObjs.stdPlayerIds
        val nearbyTile = TileId("1-B")
        val odoTile = TileId("1-A")
        val players = listOf(Players.standardPlayer(PlayerId("odo"), initialTiles = listOf(odoTile)),
                             Players.standardPlayer(PlayerId("joree")),
                             Players.standardPlayer(PlayerId("sal")))
        val board = Boards.standard().withTile(nearbyTile)
        val acqObj = AcqObjs.custom(Tiles.standard(), Banks.standard(), board, Hotels.standard(), players)
        val pt0 = PlaceTile(acqObj, TestAcqObjs.stdGameState)

        /*
         * odo PlaceTile request
         */

        val (r1, s1) = pt0.placeTile(odo, odoTile)
        Assert.assertTrue("Expect success", r1.isSuccess())
        Assert.assertTrue("State is now StartHotel", s1 is StartHotel)
        val sh1 = s1 as StartHotel

        // confirm state is correct
        Assert.assertEquals("GameState are equal", pt0.gameState, sh1.gameState)
        Assert.assertEquals("StartHotel contains both tiles on the board", 2, sh1.tiles.size)
        Assert.assertTrue("StartHotel contains the nearby tile", sh1.tiles.contains(nearbyTile))
        Assert.assertTrue("StartHotel contains odo's tile", sh1.tiles.contains(odoTile))

        // confirm player updated correctly
        Assert.assertTrue("Tile is no longer in odo's hand", sh1.acqObj.player(odo).tiles().isEmpty())

        // confirm board update correctly
        Assert.assertTrue("Tile is now on the board", sh1.acqObj.board.containsTile(odoTile))
        Assert.assertEquals("odoTile is in a tile group with nearbyTile", 2, sh1.acqObj.board.connectedTiles(odoTile).size)
        Assert.assertEquals("nearbyTile is in a tile group with odoTile", 2, sh1.acqObj.board.connectedTiles(nearbyTile).size)
        Assert.assertEquals("odoTile has one neighbor", 1, sh1.acqObj.board.adjacentTiles(odoTile).size)
        Assert.assertEquals("nearbyTile has one neighbor", 1, sh1.acqObj.board.adjacentTiles(odoTile).size)

        // confirm tile state updated correctly
        Assert.assertTrue("odoTile state is OnBoard", sh1.acqObj.tile(odoTile).state() is TileState.OnBoard)
        Assert.assertTrue("nearbyTile state is OnBoard", sh1.acqObj.tile(nearbyTile).state() is TileState.OnBoard)
    }

    @Test
    fun nearbyGroupedTilesHotelsAvailable() {
        val (odo, joree, sal) = TestAcqObjs.stdPlayerIds
        val groupedTile = TileId("1-C")
        val nearbyTile = TileId("1-B")
        val odoTile = TileId("1-A")
        val players = listOf(Players.standardPlayer(PlayerId("odo"), initialTiles = listOf(odoTile)),
                             Players.standardPlayer(PlayerId("joree")),
                             Players.standardPlayer(PlayerId("sal")))
        val board = Boards.standard().withTiles(listOf(nearbyTile, groupedTile))
        val acqObj = AcqObjs.custom(Tiles.standard(), Banks.standard(), board, Hotels.standard(), players)
        val pt0 = PlaceTile(acqObj, TestAcqObjs.stdGameState)

        /*
         * odo PlaceTile request
         */

        val (r1, s1) = pt0.placeTile(odo, odoTile)
        Assert.assertTrue("Expect success", r1.isSuccess())
        Assert.assertTrue("State is now StartHotel", s1 is StartHotel)
        val sh1 = s1 as StartHotel

        // confirm state is correct
        Assert.assertEquals("GameState are equal", pt0.gameState, sh1.gameState)
        Assert.assertEquals("StartHotel contains all tiles on the board", 3, sh1.tiles.size)
        Assert.assertTrue("StartHotel contains the nearby tile", sh1.tiles.contains(nearbyTile))
        Assert.assertTrue("StartHotel contains the grouped tile", sh1.tiles.contains(groupedTile))
        Assert.assertTrue("StartHotel contains odo's tile", sh1.tiles.contains(odoTile))

        // confirm player updated correctly
        Assert.assertTrue("Tile is no longer in odo's hand", sh1.acqObj.player(odo).tiles().isEmpty())

        // confirm board update correctly
        Assert.assertTrue("Tile is now on the board", sh1.acqObj.board.containsTile(odoTile))
        Assert.assertEquals("odoTile is in a tile group with nearbyTile and grouped tile", 3, sh1.acqObj.board.connectedTiles(odoTile).size)
        Assert.assertEquals("nearbyTile is in a tile group with odoTile and grouped tile", 3, sh1.acqObj.board.connectedTiles(nearbyTile).size)
        Assert.assertEquals("groupedTile is in a tile group with odoTile and nearby tile", 3, sh1.acqObj.board.connectedTiles(groupedTile).size)
        Assert.assertEquals("odoTile has one neighbor", 1, sh1.acqObj.board.adjacentTiles(odoTile).size)
        Assert.assertEquals("nearbyTile has two neighbors", 2, sh1.acqObj.board.adjacentTiles(nearbyTile).size)
        Assert.assertEquals("groupedTile has one neighbor", 1, sh1.acqObj.board.adjacentTiles(groupedTile).size)

        // confirm tile state updated correctly
        Assert.assertTrue("odoTile state is OnBoard", sh1.acqObj.tile(odoTile).state() is TileState.OnBoard)
        Assert.assertTrue("nearbyTile state is OnBoard", sh1.acqObj.tile(nearbyTile).state() is TileState.OnBoard)
        Assert.assertTrue("groupedTile state is OnBoard", sh1.acqObj.tile(groupedTile).state() is TileState.OnBoard)
    }

    @Test
    fun nearbyTileNoHotelsAvailable() {
        val (odo, joree, sal) = TestAcqObjs.stdPlayerIds
        val nearbyTile = TileId("1-B")
        val odoTile1 = TileId("1-A")
        val odoTile2 = TileId("10-A")
        val hotelTileGroups = listOf(listOf(TileId("3-A"), TileId("3-B")),
                                     listOf(TileId("3-D"), TileId("3-E")),
                                     listOf(TileId("5-A"), TileId("5-B")),
                                     listOf(TileId("5-D"), TileId("5-E")),
                                     listOf(TileId("7-A"), TileId("7-B")),
                                     listOf(TileId("7-D"), TileId("7-E")),
                                     listOf(TileId("9-A"), TileId("9-B")))
        val hotels = Hotels.standard().mapIndexed { i, hotel -> hotel.withState(HotelState.OnBoard(hotelTileGroups[i])) }
        val players = listOf(Players.standardPlayer(PlayerId("odo"), initialTiles = listOf(odoTile1, odoTile2)),
                             Players.standardPlayer(PlayerId("joree")),
                             Players.standardPlayer(PlayerId("sal")))
        val board = Boards.standard().withTile(nearbyTile)
        val acqObj = AcqObjs.custom(Tiles.standard(), Banks.standard(), board, hotels, players)
        val pt0 = PlaceTile(acqObj, TestAcqObjs.stdGameState)

        /*
         * odo PlaceTile request (expect failure: no hotels available to start)
         */

        val (r1, s1) = pt0.placeTile(odo, odoTile1)
        Assert.assertTrue("Expect failure", r1.isFailure())
        Assert.assertTrue("State is the same as before the failure", pt0 == s1)
    }

    @Test
    fun nearbyTileNoHotelsAvailableAllTiles() {
        val (odo, joree, sal) = TestAcqObjs.stdPlayerIds
        val nearbyTiles = listOf(TileId("1-B"), TileId("11-B"))
        val odoTile1 = TileId("1-A")
        val odoTile2 = TileId("11-A")
        val hotelTileGroups = listOf(listOf(TileId("3-A"), TileId("3-B")),
                                     listOf(TileId("3-D"), TileId("3-E")),
                                     listOf(TileId("5-A"), TileId("5-B")),
                                     listOf(TileId("5-D"), TileId("5-E")),
                                     listOf(TileId("7-A"), TileId("7-B")),
                                     listOf(TileId("7-D"), TileId("7-E")),
                                     listOf(TileId("9-A"), TileId("9-B")))
        val hotels = Hotels.standard().mapIndexed { i, hotel -> hotel.withState(HotelState.OnBoard(hotelTileGroups[i])) }
        val players = listOf(Players.standardPlayer(PlayerId("odo"), initialTiles = listOf(odoTile1, odoTile2)),
                             Players.standardPlayer(PlayerId("joree")),
                             Players.standardPlayer(PlayerId("sal")))
        val board = Boards.standard().withTiles(nearbyTiles)
        val acqObj = AcqObjs.custom(Tiles.standard(), Banks.standard(), board, hotels, players)
        val pt0 = PlaceTile(acqObj, TestAcqObjs.stdGameState)

        /*
         * odo PlaceTile request
         */

        val (r1, s1) = pt0.placeTile(odo, odoTile1)
        Assert.assertTrue("Expect success", r1.isSuccess())
        Assert.assertTrue("State is now BuyStock", s1 is BuyStock)
        val bs1 = s1 as BuyStock

        // confirm state is correct
        Assert.assertEquals("GameStates are equal", pt0.gameState, bs1.gameState)

        // confirm player updated correct
        Assert.assertTrue("odoTile1 is still in odo's hand", bs1.acqObj.player(odo).tiles().contains(odoTile1))
        Assert.assertTrue("odoTile2 is still in odo's hand", bs1.acqObj.player(odo).tiles().contains(odoTile2))

        // confirm board updated correctly
        Assert.assertTrue("odoTile1 is not on the board", !bs1.acqObj.board.containsTile(odoTile1))
        Assert.assertTrue("odoTile2 is not on the board", !bs1.acqObj.board.containsTile(odoTile2))

        // confirm tile state stayed the same
        Assert.assertTrue("odoTile1 is still in Player's hand", bs1.acqObj.tile(odoTile1).state() is TileState.PlayerHand)
        val odoTile1State = bs1.acqObj.tile(odoTile1).state() as TileState.PlayerHand
        Assert.assertEquals("odoTile1 is still in odo's hand", odo, odoTile1State.player)
        Assert.assertTrue("odoTile2 is still in Player's hand", bs1.acqObj.tile(odoTile2).state() is TileState.PlayerHand)
        val odoTile2State = bs1.acqObj.tile(odoTile2).state() as TileState.PlayerHand
        Assert.assertEquals("odoTile2 is still in odo's hand", odo, odoTile2State.player)
    }

    @Test
    fun nearbyHotel() {
        val (odo, joree, sal) = TestAcqObjs.stdPlayerIds
        val odoTile = TileId("1-A")
        val hotelTiles = listOf(TileId("1-B"), TileId("1-C"))
        val hotel = Hotels.STANDARD_ID_TIERS.keys.iterator().next()
        val hotels = Hotels.standard().map { h -> if (h.id() == hotel) h.withState(HotelState.OnBoard(hotelTiles))
                                                  else h }
        val players = listOf(Players.standardPlayer(PlayerId("odo"), initialTiles = listOf(odoTile)),
                             Players.standardPlayer(PlayerId("joree")),
                             Players.standardPlayer(PlayerId("sal")))
        val acqObj = AcqObjs.custom(Tiles.standard(), Banks.standard(), Boards.standard(), hotels, players)
        val pt0 = PlaceTile(acqObj, TestAcqObjs.stdGameState)

        /*
         * odo PlaceTile request
         */

        val (r1, s1) = pt0.placeTile(odo, odoTile)
        Assert.assertTrue("Expect success", r1.isSuccess())
        Assert.assertTrue("State is now BuyStock", s1 is BuyStock)
        val bs1 = s1 as BuyStock

        // confirm state is correct
        Assert.assertEquals("GameStates are equal", pt0.gameState, bs1.gameState)

        // confirm player updated correctly
        Assert.assertTrue("Tile is no longer in odo's hand", bs1.acqObj.player(odo).tiles().isEmpty())

        // confirm board updated correctly
        Assert.assertTrue("Tile is now on the board", bs1.acqObj.board.containsTile(odoTile))
        Assert.assertEquals("odoTile is in a tile group with hotel tiles", 3, bs1.acqObj.board.connectedTiles(odoTile).size)
        Assert.assertTrue("Hotel tiles all have three tiles in their TileGroup", hotelTiles.all { t -> bs1.acqObj.board.connectedTiles(t).size == 3 })
        Assert.assertEquals("odoTile has one neighbor", 1, bs1.acqObj.board.adjacentTiles(odoTile).size)

        // confirm hotel updated correctly
        Assert.assertEquals("Hotel grew in size", pt0.acqObj.hotel(hotel).size() + 1, bs1.acqObj.hotel(hotel).size())
        Assert.assertEquals("Hotel size is three", 3, bs1.acqObj.hotel(hotel).size())

        // confirm tile state updated correctly
        Assert.assertTrue("odoTile state is OnBoardHotel", bs1.acqObj.tile(odoTile).state() is TileState.OnBoardHotel)
        val odoTileState = bs1.acqObj.tile(odoTile).state() as TileState.OnBoardHotel
        Assert.assertEquals("odoTile state OnBoardHotel is of hotel", hotel, odoTileState.hotel)
    }

    @Test
    fun nearbyHotelsSafe() {
        val (odo, joree, sal) = TestAcqObjs.stdPlayerIds
        val odoTile = TileId("1-B")
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
        val players = listOf(Players.standardPlayer(PlayerId("odo"), initialTiles = listOf(odoTile)),
                             Players.standardPlayer(PlayerId("joree")),
                             Players.standardPlayer(PlayerId("sal")))
        val acqObj = AcqObjs.custom(Tiles.standard(), Banks.standard(), Boards.standard(), hotels, players)
        val pt0 = PlaceTile(acqObj, TestAcqObjs.stdGameState)

        /*
         * odo PlaceTile request
         */

        val (r1, s1) = pt0.placeTile(odo, odoTile)
        Assert.assertTrue("Expect success", r1.isSuccess())
        Assert.assertTrue("State is now PlaceTile", s1 is PlaceTile)
        val pt1 = s1 as PlaceTile

        // confirm state is correct
        Assert.assertEquals("GameStates are equal", pt0.gameState, pt1.gameState)

        // confirm player updated correctly
        Assert.assertTrue("Tile has been discarded from odo's hand", pt1.acqObj.player(odo).tiles().isEmpty())

        // confirm board updated correctly
        Assert.assertTrue("The tile is not on the board", !pt1.acqObj.board.containsTile(odoTile))

        // confirm hotels updated correctly
        Assert.assertEquals("Hotel1 stayed the same size", pt0.acqObj.hotel(hotel1).size(), pt1.acqObj.hotel(hotel1).size())
        Assert.assertEquals("Hotel2 stayed the same size", pt0.acqObj.hotel(hotel2).size(), pt1.acqObj.hotel(hotel2).size())
        Assert.assertTrue("Hotel1 is still safe", pt1.acqObj.hotel(hotel1).isSafe())
        Assert.assertTrue("Hotel2 is still safe", pt1.acqObj.hotel(hotel2).isSafe())

        // confirm tile state update correctly
        Assert.assertTrue("odoTile state is Discarded", pt1.acqObj.tile(odoTile).state() is TileState.Discarded)
    }

    @Test
    fun nearbyHotelsMerge() {
        val (odo, joree, sal) = TestAcqObjs.stdPlayerIds
        val odoTile = TileId("1-B")
        val hotelItr = Hotels.STANDARD_ID_TIERS.keys.iterator()
        val hotel1 = hotelItr.next()
        val hotel2 = hotelItr.next()
        val hotel1Tiles = listOf(TileId("1-A"), TileId("2-A"), TileId("3-A"), TileId("4-A"), TileId("5-A"))
        val hotel2Tiles = listOf(TileId("1-C"), TileId("2-C"), TileId("3-C"), TileId("4-C"), TileId("5-C"),
                                 TileId("6-C"), TileId("7-C"), TileId("8-C"), TileId("9-C"), TileId("10-C"), TileId("11-C"))
        val hotels = Hotels.standard().map { h ->
            when {
                h.id() == hotel1 -> h.withState(HotelState.OnBoard(hotel1Tiles))
                h.id() == hotel2 -> h.withState(HotelState.OnBoard(hotel2Tiles))
                else -> h
            }
        }
        val players = listOf(Players.standardPlayer(PlayerId("odo"), initialTiles = listOf(odoTile)),
                             Players.standardPlayer(PlayerId("joree"), initialStocks = mapOf(hotel1 to 1)),
                             Players.standardPlayer(PlayerId("sal")))
        val acqObj = AcqObjs.custom(Tiles.standard(), Banks.standard(), Boards.standard(), hotels, players)
        val pt0 = PlaceTile(acqObj, TestAcqObjs.stdGameState)

        /*
         * odo PlaceTile request
         */

        val (r1, s1) = pt0.placeTile(odo, odoTile)
        Assert.assertTrue("Expect success", r1.isSuccess())
        Assert.assertTrue("State is now PayBonuses", s1 is PayBonuses)
        val pb1 = s1 as PayBonuses

        // confirm state is correct
        val hotelnTiles = hotel1Tiles + hotel2Tiles
        val hotelns = listOf(hotel1, hotel2)
        Assert.assertEquals("GameStates are equal", pt0.gameState, pb1.mergeState.mergeContext.gameState)
        Assert.assertEquals("Placed tile is odoTile", odoTile, pb1.mergeState.mergeContext.placedTile)
        Assert.assertEquals("Nearby hotels contains hotels", hotelns, pb1.mergeState.mergeContext.nearbyHotels)

        Assert.assertEquals("Surviving hotel is hotel2", hotel2, pb1.mergeState.survivingHotel)
        Assert.assertEquals("Defunct hotel is hotel1", hotel1, pb1.mergeState.defunctHotel)
        Assert.assertTrue("Remaining hotels is empty", pb1.mergeState.remainingHotels.isEmpty())

        // confirm player updated correctly
        Assert.assertTrue("Tile was removed from odo's hand", pb1.acqObj.player(odo).tiles().isEmpty())

        // confirm board updated correctly
        Assert.assertTrue("Tile is on the board", pb1.acqObj.board.containsTile(odoTile))

        // confirm hotels updated correctly
        Assert.assertEquals("Hotel1 stayed the same size", pt0.acqObj.hotel(hotel1).size(), pb1.acqObj.hotel(hotel1).size())
        Assert.assertEquals("Hotel2 stayed the same size", pt0.acqObj.hotel(hotel2).size(), pb1.acqObj.hotel(hotel2).size())
        Assert.assertTrue("Hotel1 is on the board", pb1.acqObj.hotel(hotel1).isOnBoard())
        Assert.assertTrue("Hotel2 is on the board", pb1.acqObj.hotel(hotel2).isOnBoard())

        // confirm tile state update correctly
        Assert.assertTrue("odoTile state is OnBoard", pb1.acqObj.tile(odoTile).state() is TileState.OnBoard)
    }

    @Test
    fun nearbyHotelsMergeAndAddTiles() {
        val (odo, joree, sal) = TestAcqObjs.stdPlayerIds
        val odoTile = TileId("1-C")
        val extraTile = TileId("1-D")
        val hotelItr = Hotels.STANDARD_ID_TIERS.keys.iterator()
        val hotel1 = hotelItr.next()
        val hotel2 = hotelItr.next()
        val hotel1Tiles = listOf(TileId("1-A"), TileId("1-B"), TileId("2-A"), TileId("3-A"), TileId("4-A"), TileId("5-A"))
        val hotel2Tiles = listOf(TileId("2-C"), TileId("3-C"), TileId("4-C"), TileId("5-C"),
                                 TileId("6-C"), TileId("7-C"), TileId("8-C"), TileId("9-C"), TileId("10-C"), TileId("11-C"))
        val hotels = Hotels.standard().map { h ->
            when {
                h.id() == hotel1 -> h.withState(HotelState.OnBoard(hotel1Tiles))
                h.id() == hotel2 -> h.withState(HotelState.OnBoard(hotel2Tiles))
                else -> h
            }
        }
        val players = listOf(Players.standardPlayer(PlayerId("odo"), initialTiles = listOf(odoTile)),
                             Players.standardPlayer(PlayerId("joree"), initialStocks = mapOf(hotel1 to 1)),
                             Players.standardPlayer(PlayerId("sal")))
        val board = Boards.standard().withTile(extraTile)
        val acqObj = AcqObjs.custom(Tiles.standard(), Banks.standard(), board, hotels, players)
        val pt0 = PlaceTile(acqObj, TestAcqObjs.stdGameState)

        /*
         * odo PlaceTile request
         */

        val (r1, s1) = pt0.placeTile(odo, odoTile)
        Assert.assertTrue("Expect success", r1.isSuccess())
        Assert.assertTrue("State is now PayBonuses", s1 is PayBonuses)
        val pb1 = s1 as PayBonuses

        // confirm state is correct
        val hotelnTilesAndExtra = hotel1Tiles + extraTile + hotel2Tiles
        val hotelns = listOf(hotel1, hotel2)
        Assert.assertEquals("GameStates are equal", pt0.gameState, pb1.mergeState.mergeContext.gameState)
        Assert.assertEquals("Placed tile is odoTile", odoTile, pb1.mergeState.mergeContext.placedTile)
        Assert.assertEquals("Nearby hotels contains hotels", hotelns, pb1.mergeState.mergeContext.nearbyHotels)

        Assert.assertEquals("Surviving hotel is hotel2", hotel2, pb1.mergeState.survivingHotel)
        Assert.assertEquals("Defunct hotel is hotel1", hotel1, pb1.mergeState.defunctHotel)
        Assert.assertTrue("Remaining hotels is empty", pb1.mergeState.remainingHotels.isEmpty())

        // confirm player updated correctly
        Assert.assertTrue("Tile was removed from odo's hand", pb1.acqObj.player(odo).tiles().isEmpty())

        // confirm board updated correctly
        Assert.assertTrue("Tile is on the board", pb1.acqObj.board.containsTile(odoTile))

        // confirm hotels updated correctly
        Assert.assertEquals("Hotel1 stayed the same size", pt0.acqObj.hotel(hotel1).size(), pb1.acqObj.hotel(hotel1).size())
        Assert.assertEquals("Hotel2 stayed the same size", pt0.acqObj.hotel(hotel2).size(), pb1.acqObj.hotel(hotel2).size())
        Assert.assertTrue("Hotel1 is on the board", pb1.acqObj.hotel(hotel1).isOnBoard())
        Assert.assertTrue("Hotel2 is on the board", pb1.acqObj.hotel(hotel2).isOnBoard())

        // confirm tile state update correctly
        Assert.assertTrue("odoTile state is OnBoard", pb1.acqObj.tile(odoTile).state() is TileState.OnBoard)
    }

    @Test
    fun nearbyHotelsMergeChooseSurviving() {
        val (odo, joree, sal) = TestAcqObjs.stdPlayerIds
        val odoTile = TileId("1-B")
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
        val players = listOf(Players.standardPlayer(PlayerId("odo"), initialTiles = listOf(odoTile)),
                             Players.standardPlayer(PlayerId("joree")),
                             Players.standardPlayer(PlayerId("sal")))
        val acqObj = AcqObjs.custom(Tiles.standard(), Banks.standard(), Boards.standard(), hotels, players)
        val pt0 = PlaceTile(acqObj, TestAcqObjs.stdGameState)

        /*
         * odo PlaceTile request
         */

        val (r1, s1) = pt0.placeTile(odo, odoTile)
        Assert.assertTrue("Expect success", r1.isSuccess())
        Assert.assertTrue("State is now ChooseSurvivingHotel", s1 is ChooseSurvivingHotel)
        val csh1 = s1 as ChooseSurvivingHotel

        // confirm state is correct
        val hotelnTiles = hotel1Tiles + hotel2Tiles
        val hotelns = listOf(hotel1, hotel2)
        Assert.assertEquals("GameStates are equal", pt0.gameState, csh1.mergeContext.gameState)
        Assert.assertEquals("Placed tile is odoTile", odoTile, csh1.mergeContext.placedTile)
        Assert.assertEquals("Nearby hotels contains hotels", hotelns, csh1.mergeContext.nearbyHotels)

        Assert.assertEquals("Potential surviving hotels include the two hotels", hotelns, csh1.potentialSurvivingHotels)

        // confirm player updated correctly
        Assert.assertTrue("Tile was removed from odo's hand", csh1.acqObj.player(odo).tiles().isEmpty())

        // confirm board updated correctly
        Assert.assertTrue("Tile is on the board", csh1.acqObj.board.containsTile(odoTile))

        // confirm hotels updated correctly
        Assert.assertEquals("Hotel1 stayed the same size", pt0.acqObj.hotel(hotel1).size(), csh1.acqObj.hotel(hotel1).size())
        Assert.assertEquals("Hotel2 stayed the same size", pt0.acqObj.hotel(hotel2).size(), csh1.acqObj.hotel(hotel2).size())
        Assert.assertTrue("Hotel1 is on the board", csh1.acqObj.hotel(hotel1).isOnBoard())
        Assert.assertTrue("Hotel2 is on the board", csh1.acqObj.hotel(hotel2).isOnBoard())

        // confirm tile state update correctly
        Assert.assertTrue("odoTile state is OnBoard", csh1.acqObj.tile(odoTile).state() is TileState.OnBoard)
    }

    @Test
    fun nearbyHotelsMergeChooseSurvivingWithDefunct() {
        val (odo, joree, sal) = TestAcqObjs.stdPlayerIds
        val odoTile = TileId("4-A")
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
        val players = listOf(Players.standardPlayer(PlayerId("odo"), initialTiles = listOf(odoTile)),
                             Players.standardPlayer(PlayerId("joree")),
                             Players.standardPlayer(PlayerId("sal")))
        val acqObj = AcqObjs.custom(Tiles.standard(), Banks.standard(), Boards.standard(), hotels, players)
        val pt0 = PlaceTile(acqObj, TestAcqObjs.stdGameState)

        /*
         * odo PlaceTile request
         */

        val (r1, s1) = pt0.placeTile(odo, odoTile)
        Assert.assertTrue("Expect success", r1.isSuccess())
        Assert.assertTrue("State is now ChooseSurvivingHotel", s1 is ChooseSurvivingHotel)
        val csh1 = s1 as ChooseSurvivingHotel

        // confirm state is correct
        val hotelnTiles = hotel1Tiles + hotel2Tiles + hotel3Tiles
        val hotelns = listOf(hotel1, hotel2, hotel3)
        Assert.assertEquals("GameStates are equal", pt0.gameState, csh1.mergeContext.gameState)
        Assert.assertEquals("Placed tile is odoTile", odoTile, csh1.mergeContext.placedTile)
        Assert.assertEquals("Nearby hotels contains hotels", hotelns.sorted(), csh1.mergeContext.nearbyHotels.sorted())

        Assert.assertEquals("Potential surviving hotels include the two 3 size hotels", listOf(hotel1, hotel2).sorted(), csh1.potentialSurvivingHotels.sorted())

        // confirm player updated correctly
        Assert.assertTrue("Tile was removed from odo's hand", csh1.acqObj.player(odo).tiles().isEmpty())

        // confirm board updated correctly
        Assert.assertTrue("Tile is on the board", csh1.acqObj.board.containsTile(odoTile))

        // confirm hotels updated correctly
        Assert.assertEquals("Hotel1 stayed the same size", pt0.acqObj.hotel(hotel1).size(), csh1.acqObj.hotel(hotel1).size())
        Assert.assertEquals("Hotel2 stayed the same size", pt0.acqObj.hotel(hotel2).size(), csh1.acqObj.hotel(hotel2).size())
        Assert.assertEquals("Hotel3 stayed the same size", pt0.acqObj.hotel(hotel3).size(), csh1.acqObj.hotel(hotel3).size())
        Assert.assertTrue("Hotel1 is on the board", csh1.acqObj.hotel(hotel1).isOnBoard())
        Assert.assertTrue("Hotel2 is on the board", csh1.acqObj.hotel(hotel2).isOnBoard())
        Assert.assertTrue("Hotel3 is on the board", csh1.acqObj.hotel(hotel3).isOnBoard())

        // confirm tile state update correctly
        Assert.assertTrue("odoTile state is OnBoard", csh1.acqObj.tile(odoTile).state() is TileState.OnBoard)
    }

    @Test
    fun nearbyHotelsMergeChooseDefunct() {
        val (odo, joree, sal) = TestAcqObjs.stdPlayerIds
        val odoTile = TileId("5-E")
        val hotelItr = Hotels.STANDARD_ID_TIERS.keys.iterator()
        val hotel1 = hotelItr.next()
        val hotel2 = hotelItr.next()
        val hotel3 = hotelItr.next()
        val hotel4 = hotelItr.next()
        val hotel1Tiles = listOf(TileId("1-E"), TileId("2-E"), TileId("3-E"), TileId("4-E"))
        val hotel2Tiles = listOf(TileId("6-E"), TileId("7-E"))
        val hotel3Tiles = listOf(TileId("5-B"), TileId("5-C"), TileId("5-D"))
        val hotel4Tiles = listOf(TileId("5-F"), TileId("5-G"), TileId("5-H"))
        val hotels = Hotels.standard().map { h ->
            when {
                h.id() == hotel1 -> h.withState(HotelState.OnBoard(hotel1Tiles))
                h.id() == hotel2 -> h.withState(HotelState.OnBoard(hotel2Tiles))
                h.id() == hotel3 -> h.withState(HotelState.OnBoard(hotel3Tiles))
                h.id() == hotel4 -> h.withState(HotelState.OnBoard(hotel4Tiles))
                else -> h
            }
        }
        val players = listOf(Players.standardPlayer(PlayerId("odo"), initialTiles = listOf(odoTile)),
                             Players.standardPlayer(PlayerId("joree")),
                             Players.standardPlayer(PlayerId("sal")))
        val acqObj = AcqObjs.custom(Tiles.standard(), Banks.standard(), Boards.standard(), hotels, players)
        val pt0 = PlaceTile(acqObj, TestAcqObjs.stdGameState)

        /*
         * odo PlaceTile request
         */

        val (r1, s1) = pt0.placeTile(odo, odoTile)
        Assert.assertTrue("Expect success", r1.isSuccess())
        Assert.assertTrue("State is now ChooseDefunctHotel", s1 is ChooseDefunctHotel)
        val cdh1 = s1 as ChooseDefunctHotel

        // confirm state is correct
        val hotelnTiles = hotel1Tiles + hotel2Tiles + hotel3Tiles + hotel4Tiles
        val hotelns = listOf(hotel1, hotel2, hotel3, hotel4)
        Assert.assertEquals("GameStates are equal", pt0.gameState, cdh1.mergeContext.gameState)
        Assert.assertEquals("Placed tile is odoTile", odoTile, cdh1.mergeContext.placedTile)
        Assert.assertEquals("Nearby hotels contains hotels", hotelns.sorted(), cdh1.mergeContext.nearbyHotels.sorted())

        Assert.assertEquals("Surviving hotel is hotel1", hotel1, cdh1.survivingHotel)
        Assert.assertEquals("Remaining hotels contains hotel2, hotel3, and hotel4", listOf(hotel2, hotel3, hotel4).sorted(), cdh1.remainingHotels.sorted())
        Assert.assertEquals("Potential remaining hotels contains hotel3 and hotel4", listOf(hotel3, hotel4).sorted(), cdh1.potentialNextDefunctHotels.sorted())

        // confirm player updated correctly
        Assert.assertTrue("Tile was removed from odo's hand", cdh1.acqObj.player(odo).tiles().isEmpty())

        // confirm board updated correctly
        Assert.assertTrue("Tile is on the board", cdh1.acqObj.board.containsTile(odoTile))

        // confirm hotels updated correctly
        Assert.assertEquals("Hotel1 stayed the same size", pt0.acqObj.hotel(hotel1).size(), cdh1.acqObj.hotel(hotel1).size())
        Assert.assertEquals("Hotel2 stayed the same size", pt0.acqObj.hotel(hotel2).size(), cdh1.acqObj.hotel(hotel2).size())
        Assert.assertEquals("Hotel3 stayed the same size", pt0.acqObj.hotel(hotel3).size(), cdh1.acqObj.hotel(hotel3).size())
        Assert.assertEquals("Hotel4 stayed the same size", pt0.acqObj.hotel(hotel4).size(), cdh1.acqObj.hotel(hotel4).size())
        Assert.assertTrue("Hotel1 is on the board", cdh1.acqObj.hotel(hotel1).isOnBoard())
        Assert.assertTrue("Hotel2 is on the board", cdh1.acqObj.hotel(hotel2).isOnBoard())
        Assert.assertTrue("Hotel3 is on the board", cdh1.acqObj.hotel(hotel3).isOnBoard())
        Assert.assertTrue("Hotel4 is on the board", cdh1.acqObj.hotel(hotel4).isOnBoard())

        // confirm tile state update correctly
        Assert.assertTrue("odoTile state is OnBoard", cdh1.acqObj.tile(odoTile).state() is TileState.OnBoard)
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
        val pt0 = PlaceTile(acqObj, TestAcqObjs.stdGameState)

        /*
         * odo EndGame request
         */

        val (r1, s1) = pt0.endGame(odo)
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
        val pt0 = PlaceTile(acqObj, TestAcqObjs.stdGameState)

        /*
         * odo EndGame request
         */

        val (r1, s1) = pt0.endGame(odo)
        Assert.assertTrue("Expect success", r1.isSuccess())
        Assert.assertTrue("State is now EndGamePayout", s1 is EndGamePayout)
        val egp1 = s1 as EndGamePayout

        // confirm state is correct
        Assert.assertTrue("Players paid is empty", egp1.playersPaid.isEmpty())
    }
}
