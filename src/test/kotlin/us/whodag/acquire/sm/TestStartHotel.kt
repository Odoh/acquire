package us.whodag.acquire.sm

import org.junit.Assert
import org.junit.Test
import us.whodag.acquire.obj.AcqObjs
import us.whodag.acquire.TestAcqObjs
import us.whodag.acquire.req.isFailure
import us.whodag.acquire.req.isSuccess
import us.whodag.acquire.obj.bank.Banks
import us.whodag.acquire.obj.bank.Banks.withStocks
import us.whodag.acquire.obj.board.Boards
import us.whodag.acquire.obj.board.Boards.withTiles
import us.whodag.acquire.obj.hotel
import us.whodag.acquire.obj.hotel.HotelState
import us.whodag.acquire.obj.hotel.Hotels
import us.whodag.acquire.obj.player.Players
import us.whodag.acquire.obj.tile
import us.whodag.acquire.obj.tile.TileId
import us.whodag.acquire.obj.tile.TileState
import us.whodag.acquire.obj.tile.Tiles

class TestStartHotel {

    @Test
    fun twoTiles() {
        val (odo, joree, sal) = TestAcqObjs.stdPlayerIds
        val tiles = listOf(TileId("1-A"), TileId("1-B"))
        val board = Boards.standard().withTiles(tiles)
        val acqObj = AcqObjs.custom(Tiles.standard(), Banks.standard(), board, Hotels.standard(), Players.standard(TestAcqObjs.stdPlayerIds))
        val hotel = Hotels.STANDARD_ID_TIERS.keys.iterator().next()
        val sh0 = StartHotel(acqObj, TestAcqObjs.stdGameState, tiles)

        /*
         * StartHotel request for sal (failure)
         */

        val (r1, s1) = sh0.startHotel(sal, hotel)
        Assert.assertTrue("Expect failure", r1.isFailure())
        Assert.assertTrue("State equals state before failed command", sh0 == s1)
        val sh1 = s1 as StartHotel

        /*
         * StartHotel for odo
         */

        val (r2, s2) = sh1.startHotel(odo, hotel)
        Assert.assertTrue("Expect success", r2.isSuccess())
        Assert.assertTrue("State is FoundersStock", s2 is FoundersStock)
        val fs2 = s2 as FoundersStock

        // verify FoundersStock is correct
        Assert.assertEquals("The FoundersStock is for the started hotel", hotel, fs2.startedHotel)
        Assert.assertEquals("Game states are equal", sh1.gameState, fs2.gameState)

        // verify hotel state updated
        Assert.assertTrue("Hotel is no longer available", !fs2.acqObj.hotel(hotel).isAvailable())
        Assert.assertTrue("Hotel is on board", fs2.acqObj.hotel(hotel).isOnBoard())
        Assert.assertTrue("Hotel state is OnBoard", fs2.acqObj.hotel(hotel).state() is HotelState.OnBoard)
        val hotelState = fs2.acqObj.hotel(hotel).state() as HotelState.OnBoard
        Assert.assertEquals("Hotel state contains two tiles", 2, hotelState.tiles.size)
        Assert.assertTrue("Hotel state contains the two tiles", tiles.all { tile -> hotelState.tiles.contains(tile) })

        // verify tile states updated
        Assert.assertTrue("Tile states are now OnBoardHotel", tiles.all { tile -> fs2.acqObj.tile(tile).state() is TileState.OnBoardHotel })
        Assert.assertEquals("Tiles are a part of the started hotel", hotel, (fs2.acqObj.tile(tiles[0]).state() as TileState.OnBoardHotel).hotel)
    }

    @Test
    fun unavailableHotel() {
        val (odo, joree, sal) = TestAcqObjs.stdPlayerIds
        val tiles = listOf(TileId("1-A"), TileId("1-B"))
        val hotelTiles = listOf(TileId("3-A"), TileId("3-B"))
        val board = Boards.standard().withTiles(tiles)
        val hotel = Hotels.STANDARD_ID_TIERS.keys.iterator().next()
        val hotels = Hotels.standard().map { h -> if (h.id() == hotel) h.withState(HotelState.OnBoard(hotelTiles))
                                                  else                 h }
        val acqObj = AcqObjs.custom(Tiles.standard(), Banks.standard(), board, hotels, Players.standard(TestAcqObjs.stdPlayerIds))
        val sh0 = StartHotel(acqObj, TestAcqObjs.stdGameState, tiles)

        /*
         * StartHotel request for odo with unavailable hotel (failure)
         */

        val (r1, s1) = sh0.startHotel(odo, hotel)
        Assert.assertTrue("Expect failure", r1.isFailure())
        Assert.assertTrue("State equals state before failed command", sh0 == s1)
    }

    @Test
    fun noFounderToBuyStock() {
        val (odo, joree, sal) = TestAcqObjs.stdPlayerIds
        val tiles = listOf(TileId("1-A"), TileId("1-B"))
        val hotelTiles = listOf(TileId("3-A"), TileId("3-B"))
        val board = Boards.standard().withTiles(tiles)
        val hotelItr = Hotels.STANDARD_ID_TIERS.keys.iterator()
        val hotel = hotelItr.next()
        val hotelAvailable = hotelItr.next()
        val bank = Banks.standard().withStocks(0).addStock(hotel, 1)
        val hotels = Hotels.standard().map { h -> if (h.id() == hotel) h.withState(HotelState.OnBoard(hotelTiles))
                                                  else                 h }
        val acqObj = AcqObjs.custom(Tiles.standard(), bank, board, hotels, Players.standard(TestAcqObjs.stdPlayerIds))
        val sh0 = StartHotel(acqObj, TestAcqObjs.stdGameState, tiles)

        /*
         * StartHotel for odo
         */

        val (r1, s1) = sh0.startHotel(odo, hotelAvailable)
        Assert.assertTrue("Expect success", r1.isSuccess())
        Assert.assertTrue("State is BuyStock", s1 is BuyStock)
        val dt1 = s1 as BuyStock

        // draw tile state is correct
        Assert.assertEquals("Game states are equal", sh0.gameState, dt1.gameState)
    }

    @Test
    fun noFounderToDrawTileNoStock() {
        val (odo, joree, sal) = TestAcqObjs.stdPlayerIds
        val tiles = listOf(TileId("1-A"), TileId("1-B"))
        val board = Boards.standard().withTiles(tiles)
        val bank = Banks.standard().withStocks(0)
        val acqObj = AcqObjs.custom(Tiles.standard(), bank, board, Hotels.standard(), Players.standard(TestAcqObjs.stdPlayerIds))
        val hotel = Hotels.STANDARD_ID_TIERS.keys.iterator().next()
        val sh0 = StartHotel(acqObj, TestAcqObjs.stdGameState, tiles)

        /*
         * StartHotel for odo
         */

        val (r1, s1) = sh0.startHotel(odo, hotel)
        Assert.assertTrue("Expect success", r1.isSuccess())
        Assert.assertTrue("State is DrawTile", s1 is DrawTile)
        val dt1 = s1 as DrawTile

        // draw tile state is correct
        Assert.assertEquals("Game states are equal", sh0.gameState, dt1.gameState)
    }
}
