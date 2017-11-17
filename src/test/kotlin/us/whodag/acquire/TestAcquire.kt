package us.whodag.acquire

import org.junit.Assert
import org.junit.Test
import us.whodag.acquire.obj.hotel.HotelId
import us.whodag.acquire.obj.tile.TileId
import us.whodag.acquire.sm.*
import us.whodag.acquire.Acquires.filePersist
import us.whodag.acquire.req.*

class TestAcquire {

    @Test
    fun undo() {
        val (odo, joree, sal) = TestAcqObjs.stdPlayerIds
        val players = TestAcqObjs.stdPlayerIds
        val acquire = Acquires.standardShuffle(players, 1)
        Assert.assertTrue("The Acquire name is not empty", acquire.id().name.isNotEmpty())
        Assert.assertEquals("The initial turns is 0", 0, acquire.turn())
        Assert.assertTrue("The initial request is StartGameReq", acquire.state().acqReq is StartGameReq)
        Assert.assertTrue("The initial state is DrawTurnTile", acquire.state().acqSmState is DrawTurnTile)
        Assert.assertTrue("Each player has to draw a turn tile", (acquire.state().acqSmState as DrawTurnTile).playersDrawn.isEmpty())

        acquire.submit(DrawTileReq(odo))

        /*
         * joree undo request (expect failure: last request was from odo)
         */

        val r1 = acquire.submit(UndoReq(joree))
        Assert.assertTrue("Request failed", r1.isFailure())
        Assert.assertTrue("State is still DrawTurnTile", acquire.state().acqSmState is DrawTurnTile)
        Assert.assertEquals("odo has drawn his turn tile", listOf(odo), (acquire.state().acqSmState as DrawTurnTile).playersDrawn)

        /*
         * joree undo request accept (expect failure: no undo is active)
         */

        val r2 = acquire.submit(AcceptUndoReq(joree))
        Assert.assertTrue("Request failed", r2.isFailure())
        Assert.assertTrue("State is still DrawTurnTile", acquire.state().acqSmState is DrawTurnTile)
        Assert.assertEquals("odo has drawn his turn tile", listOf(odo), (acquire.state().acqSmState as DrawTurnTile).playersDrawn)

        /*
         * odo undo request
         */

        val r3 = acquire.submit(UndoReq(odo))
        Assert.assertTrue("Request is successful", r3.isSuccess())
        Assert.assertTrue("State is still DrawTurnTile", acquire.state().acqSmState is DrawTurnTile)
        Assert.assertEquals("odo has drawn his turn tile", listOf(odo), (acquire.state().acqSmState as DrawTurnTile).playersDrawn)

        /*
         * joree accept undo request
         */

        val r4 = acquire.submit(AcceptUndoReq(joree))
        Assert.assertTrue("Request is successful", r4.isSuccess())
        Assert.assertTrue("State is still DrawTurnTile", acquire.state().acqSmState is DrawTurnTile)
        Assert.assertEquals("odo has drawn his turn tile", listOf(odo), (acquire.state().acqSmState as DrawTurnTile).playersDrawn)

        /*
         * sal draw turn tile
         */

        val r5 = acquire.submit(DrawTileReq(sal))
        Assert.assertTrue("Request is successful", r5.isSuccess())
        Assert.assertTrue("State is still DrawTurnTile", acquire.state().acqSmState is DrawTurnTile)
        Assert.assertEquals("odo and sal have drawn their turn tiles", listOf(odo, sal).sorted(), (acquire.state().acqSmState as DrawTurnTile).playersDrawn.sorted())

        /*
         * sal accept undo (expect failure: no undo is active - undo should reset after any other game request)
         */

        val r6 = acquire.submit(AcceptUndoReq(sal))
        Assert.assertTrue("Request failed", r6.isFailure())
        Assert.assertTrue("State is still DrawTurnTile", acquire.state().acqSmState is DrawTurnTile)
        Assert.assertEquals("odo and sal have drawn their turn tiles", listOf(odo, sal).sorted(), (acquire.state().acqSmState as DrawTurnTile).playersDrawn.sorted())

        /*
         * sal undo request
         */

        val r7 = acquire.submit(UndoReq(sal))
        Assert.assertTrue("Request is successful", r7.isSuccess())
        Assert.assertTrue("State is still DrawTurnTile", acquire.state().acqSmState is DrawTurnTile)
        Assert.assertEquals("odo and sal have drawn their turn tiles", listOf(odo, sal).sorted(), (acquire.state().acqSmState as DrawTurnTile).playersDrawn.sorted())

        /*
         * odo accept undo request
         */

        val r8 = acquire.submit(AcceptUndoReq(odo))
        Assert.assertTrue("Request is successful", r8.isSuccess())
        Assert.assertTrue("State is still DrawTurnTile", acquire.state().acqSmState is DrawTurnTile)
        Assert.assertEquals("odo and sal have drawn their turn tiles", listOf(odo, sal).sorted(), (acquire.state().acqSmState as DrawTurnTile).playersDrawn.sorted())

        /*
         * joree accept undo request
         */

        val r9 = acquire.submit(AcceptUndoReq(odo))
        Assert.assertTrue("Request is successful", r9.isSuccess())
        Assert.assertTrue("State is still DrawTurnTile", acquire.state().acqSmState is DrawTurnTile)
        Assert.assertEquals("odo has drawn his turn tile", listOf(odo), (acquire.state().acqSmState as DrawTurnTile).playersDrawn)
    }

    @Test
    fun standard() {
        val (odo, joree, sal) = TestAcqObjs.stdPlayerIds
        val players = TestAcqObjs.stdPlayerIds
        val acquire = Acquires.standardShuffle(players, 1)
        Assert.assertTrue("The Acquire name is not empty", acquire.id().name.isNotEmpty())
        Assert.assertEquals("The initial turns is 0", 0, acquire.turn())
        Assert.assertTrue("The initial request is StartGameReq", acquire.state().acqReq is StartGameReq)
        Assert.assertTrue("The initial state is DrawTurnTile", acquire.state().acqSmState is DrawTurnTile)
        Assert.assertTrue("Each player has to draw a turn tile", (acquire.state().acqSmState as DrawTurnTile).playersDrawn.isEmpty())

        acquire.submit(DrawTileReq(odo))
        acquire.submit(DrawTileReq(sal))
        acquire.submit(DrawTileReq(joree))

        acquire.submit(PlaceTileReq(joree, TileId("12-C")))
        acquire.submit(PlaceTileReq(odo, TileId("12-G")))
        acquire.submit(PlaceTileReq(sal, TileId("6-G")))

        acquire.submit(DrawTileReq(sal))
        acquire.submit(DrawTileReq(odo))
        acquire.submit(DrawTileReq(joree))

        acquire.submit(PlaceTileReq(sal, TileId("6-H")))
        acquire.submit(ChooseHotelReq(sal, HotelId("continental")))
        acquire.submit(AcceptStockReq(sal))
        acquire.submit(BuyStockReq(sal, mapOf(HotelId("continental") to 3)))
        acquire.submit(DrawTileReq(sal))

        acquire.submit(PlaceTileReq(joree, TileId("9-E")))
        acquire.submit(BuyStockReq(joree, mapOf(HotelId("continental") to 3)))
        acquire.submit(DrawTileReq(joree))

        acquire.submit(PlaceTileReq(odo, TileId("4-B")))
        acquire.submit(BuyStockReq(odo, emptyMap()))
        acquire.submit(DrawTileReq(odo))

        acquire.submit(PlaceTileReq(sal, TileId("8-I")))
        acquire.submit(BuyStockReq(sal, mapOf(HotelId("continental") to 3)))
        acquire.submit(DrawTileReq(sal))

        acquire.submit(PlaceTileReq(joree, TileId("9-A")))
        acquire.submit(BuyStockReq(joree, mapOf(HotelId("continental") to 1)))
        acquire.submit(DrawTileReq(joree))

        acquire.submit(PlaceTileReq(odo, TileId("5-B")))
        acquire.submit(ChooseHotelReq(odo, HotelId("imperial")))
        acquire.submit(AcceptStockReq(odo))
        acquire.submit(BuyStockReq(odo, mapOf(HotelId("imperial") to 3)))
        acquire.submit(DrawTileReq(odo))

        acquire.submit(PlaceTileReq(sal, TileId("8-A")))
        acquire.submit(ChooseHotelReq(sal, HotelId("tower")))
        acquire.submit(AcceptStockReq(sal))
        acquire.submit(BuyStockReq(sal, mapOf(HotelId("tower") to 3)))
        acquire.submit(DrawTileReq(sal))

        acquire.submit(PlaceTileReq(joree, TileId("12-F")))
        acquire.submit(ChooseHotelReq(joree, HotelId("festival")))
        acquire.submit(AcceptStockReq(joree))
        acquire.submit(BuyStockReq(joree, mapOf(HotelId("festival") to 3)))
        acquire.submit(DrawTileReq(joree))

        acquire.submit(PlaceTileReq(odo, TileId("12-A")))
        acquire.submit(BuyStockReq(odo, mapOf(HotelId("tower") to 3)))
        acquire.submit(DrawTileReq(odo))

        acquire.submit(PlaceTileReq(sal, TileId("9-C")))
        acquire.submit(BuyStockReq(sal, mapOf(HotelId("tower") to 3)))
        acquire.submit(DrawTileReq(sal))

        acquire.submit(PlaceTileReq(joree, TileId("9-F")))
        acquire.submit(ChooseHotelReq(joree, HotelId("american")))
        acquire.submit(AcceptStockReq(joree))
        acquire.submit(BuyStockReq(joree, mapOf(HotelId("american") to 3)))
        acquire.submit(DrawTileReq(joree))

        acquire.submit(PlaceTileReq(odo, TileId("3-B")))
        acquire.submit(BuyStockReq(odo, mapOf(HotelId("american") to 3)))
        acquire.submit(DrawTileReq(odo))

        acquire.submit(PlaceTileReq(sal, TileId("9-G")))
        acquire.submit(BuyStockReq(sal, mapOf(HotelId("festival") to 2)))
        acquire.submit(DrawTileReq(sal))

        acquire.submit(PlaceTileReq(joree, TileId("7-C")))
        acquire.submit(BuyStockReq(joree, emptyMap()))
        acquire.submit(DrawTileReq(joree))

        acquire.submit(PlaceTileReq(odo, TileId("11-H")))
        acquire.submit(BuyStockReq(odo, mapOf(HotelId("tower") to 3)))
        acquire.submit(DrawTileReq(odo))

        acquire.submit(PlaceTileReq(sal, TileId("4-E")))
        acquire.submit(BuyStockReq(sal, emptyMap()))
        acquire.submit(DrawTileReq(sal))

        acquire.submit(PlaceTileReq(joree, TileId("7-B")))
        acquire.submit(ChooseHotelReq(joree, HotelId("worldwide")))
        acquire.submit(AcceptStockReq(joree))
        acquire.submit(BuyStockReq(joree, mapOf(HotelId("worldwide") to 3)))
        acquire.submit(DrawTileReq(joree))

        acquire.submit(PlaceTileReq(odo, TileId("4-F")))
        acquire.submit(ChooseHotelReq(odo, HotelId("luxor")))
        acquire.submit(AcceptStockReq(odo))
        acquire.submit(BuyStockReq(odo, mapOf(HotelId("tower") to 3)))
        acquire.submit(DrawTileReq(odo))

        acquire.submit(PlaceTileReq(sal, TileId("3-I")))
        acquire.submit(BuyStockReq(sal, mapOf(HotelId("tower") to 3)))
        acquire.submit(DrawTileReq(sal))

        acquire.submit(PlaceTileReq(joree, TileId("7-E")))
        acquire.submit(BuyStockReq(joree, mapOf(HotelId("luxor") to 3)))
        acquire.submit(DrawTileReq(joree))

        acquire.submit(PlaceTileReq(odo, TileId("5-D")))
        acquire.submit(BuyStockReq(odo, mapOf(HotelId("worldwide") to 3)))
        acquire.submit(DrawTileReq(odo))

        acquire.submit(PlaceTileReq(sal, TileId("5-I")))
        acquire.submit(BuyStockReq(sal, mapOf(HotelId("luxor") to 3)))
        acquire.submit(DrawTileReq(sal))

        acquire.submit(PlaceTileReq(joree, TileId("9-D")))
        acquire.submit(BuyStockReq(joree, mapOf(HotelId("luxor") to 3)))
        acquire.submit(DrawTileReq(joree))

        acquire.submit(PlaceTileReq(odo, TileId("12-E")))
        acquire.submit(BuyStockReq(odo, mapOf(HotelId("worldwide") to 3)))
        acquire.submit(DrawTileReq(odo))

        acquire.submit(PlaceTileReq(sal, TileId("2-A")))
        acquire.submit(BuyStockReq(sal, mapOf(HotelId("luxor") to 3)))
        acquire.submit(DrawTileReq(sal))

        acquire.submit(PlaceTileReq(joree, TileId("2-G")))
        acquire.submit(BuyStockReq(joree, mapOf(HotelId("luxor") to 1, HotelId("worldwide") to 1)))
        acquire.submit(DrawTileReq(joree))

        acquire.submit(PlaceTileReq(odo, TileId("7-A")))
        acquire.submit(ChooseHotelReq(odo, HotelId("tower")))
        acquire.submit(AcceptMoneyReq(joree))
        acquire.submit(AcceptMoneyReq(odo))
        acquire.submit(HandleStocksReq(odo, trade = 6))
        acquire.submit(HandleStocksReq(joree, keep = 5))
        acquire.submit(BuyStockReq(odo, mapOf(HotelId("tower") to 1, HotelId("luxor") to 2)))
        acquire.submit(DrawTileReq(odo))

        acquire.submit(PlaceTileReq(sal, TileId("4-D")))
        acquire.submit(DrawTileReq(sal))

        acquire.submit(PlaceTileReq(joree, TileId("12-B")))
        acquire.submit(ChooseHotelReq(joree, HotelId("worldwide")))
        acquire.submit(AcceptStockReq(joree))
        acquire.submit(BuyStockReq(joree, emptyMap()))
        acquire.submit(DrawTileReq(joree))

        acquire.submit(PlaceTileReq(odo, TileId("7-H")))
        acquire.submit(BuyStockReq(odo, mapOf(HotelId("luxor") to 3)))
        acquire.submit(DrawTileReq(odo))

        acquire.submit(PlaceTileReq(sal, TileId("8-H")))
        acquire.submit(DrawTileReq(sal))

        acquire.submit(PlaceTileReq(joree, TileId("10-F")))
        acquire.submit(BuyStockReq(joree, mapOf(HotelId("imperial") to 1)))
        acquire.submit(DrawTileReq(joree))

        acquire.submit(PlaceTileReq(odo, TileId("5-H")))
        acquire.submit(BuyStockReq(odo, mapOf(HotelId("festival") to 3)))
        acquire.submit(DrawTileReq(odo))

        acquire.submit(PlaceTileReq(sal, TileId("8-E")))
        acquire.submit(DrawTileReq(sal))

        acquire.submit(PlaceTileReq(joree, TileId("12-D")))
        acquire.submit(ChooseHotelReq(joree, HotelId("festival")))
        acquire.submit(AcceptMoneyReq(joree))
        acquire.submit(HandleStocksReq(joree, trade = 6))
        acquire.submit(BuyStockReq(joree, mapOf(HotelId("imperial") to 3)))
        acquire.submit(DrawTileReq(joree))

        acquire.submit(PlaceTileReq(odo, TileId("9-B")))
        acquire.submit(AcceptMoneyReq(sal))
        acquire.submit(AcceptMoneyReq(odo))
        acquire.submit(HandleStocksReq(odo, trade = 12, sell = 1))
        acquire.submit(HandleStocksReq(sal, trade = 8, keep = 2))
        acquire.submit(BuyStockReq(odo, mapOf(HotelId("imperial") to 3)))
        acquire.submit(DrawTileReq(odo))

        acquire.submit(PlaceTileReq(sal, TileId("6-D")))
        acquire.submit(BuyStockReq(sal, mapOf(HotelId("luxor") to 3)))
        acquire.submit(DrawTileReq(sal))

        acquire.submit(PlaceTileReq(joree, TileId("11-D")))
        acquire.submit(BuyStockReq(joree, mapOf(HotelId("luxor") to 3)))
        acquire.submit(DrawTileReq(joree))

        acquire.submit(PlaceTileReq(odo, TileId("11-C")))
        acquire.submit(BuyStockReq(odo, mapOf(HotelId("imperial") to 1, HotelId("american") to 2)))
        acquire.submit(DrawTileReq(odo))

        acquire.submit(PlaceTileReq(sal, TileId("7-D")))
        acquire.submit(AcceptMoneyReq(sal))
        acquire.submit(AcceptMoneyReq(joree))
        acquire.submit(HandleStocksReq(sal, trade = 8, sell = 1))
        acquire.submit(HandleStocksReq(joree, trade = 4, sell = 6))
        acquire.submit(HandleStocksReq(odo, sell = 6))
        acquire.submit(BuyStockReq(sal, mapOf(HotelId("festival") to 2, HotelId("continental") to 1)))
        acquire.submit(DrawTileReq(sal))

        acquire.submit(PlaceTileReq(joree, TileId("1-G")))
        acquire.submit(ChooseHotelReq(joree, HotelId("worldwide")))
        acquire.submit(AcceptStockReq(joree))
        acquire.submit(BuyStockReq(joree, mapOf(HotelId("worldwide") to 3)))
        acquire.submit(DrawTileReq(joree))

        acquire.submit(PlaceTileReq(odo, TileId("11-F")))
        acquire.submit(AcceptMoneyReq(sal))
        acquire.submit(AcceptMoneyReq(joree))
        acquire.submit(HandleStocksReq(odo, sell = 3))
        acquire.submit(HandleStocksReq(sal, sell = 4))
        acquire.submit(HandleStocksReq(joree, sell = 7))
        acquire.submit(BuyStockReq(odo, mapOf(HotelId("worldwide") to 3)))
        acquire.submit(DrawTileReq(odo))

        acquire.submit(PlaceTileReq(sal, TileId("5-C")))
        acquire.submit(AcceptMoneyReq(odo))
        acquire.submit(AcceptMoneyReq(joree))
        acquire.submit(HandleStocksReq(joree, sell = 4))
        acquire.submit(HandleStocksReq(odo, sell = 8))
        acquire.submit(BuyStockReq(sal, mapOf(HotelId("worldwide") to 3)))
        acquire.submit(DrawTileReq(sal))

        acquire.submit(PlaceTileReq(joree, TileId("1-A")))
        acquire.submit(ChooseHotelReq(joree, HotelId("imperial")))
        acquire.submit(AcceptStockReq(joree))
        acquire.submit(BuyStockReq(joree, mapOf(HotelId("imperial") to 3)))
        acquire.submit(DrawTileReq(joree))

        acquire.submit(PlaceTileReq(odo, TileId("10-H")))
        acquire.submit(ChooseHotelReq(odo, HotelId("festival")))
        acquire.submit(AcceptStockReq(odo))
        acquire.submit(BuyStockReq(odo, mapOf(HotelId("festival") to 3)))
        acquire.submit(DrawTileReq(odo))

        acquire.submit(PlaceTileReq(sal, TileId("10-B")))
        acquire.submit(BuyStockReq(sal, mapOf(HotelId("imperial") to 1, HotelId("festival") to 1, HotelId("worldwide") to 1)))
        acquire.submit(DrawTileReq(sal))

        acquire.submit(PlaceTileReq(joree, TileId("10-A")))
        acquire.submit(BuyStockReq(joree, mapOf(HotelId("festival") to 2, HotelId("worldwide") to 1)))
        acquire.submit(DrawTileReq(joree))

        acquire.submit(PlaceTileReq(odo, TileId("11-B")))
        acquire.submit(BuyStockReq(odo, mapOf(HotelId("festival") to 2, HotelId("worldwide") to 1)))
        acquire.submit(DrawTileReq(odo))

        acquire.submit(PlaceTileReq(sal, TileId("5-E")))
        acquire.submit(BuyStockReq(sal, mapOf(HotelId("festival") to 3)))
        acquire.submit(DrawTileReq(sal))

        acquire.submit(PlaceTileReq(joree, TileId("10-D")))
        acquire.submit(BuyStockReq(joree, mapOf(HotelId("festival") to 3)))
        acquire.submit(DrawTileReq(joree))

        acquire.submit(PlaceTileReq(odo, TileId("3-A")))
        acquire.submit(AcceptMoneyReq(sal))
        acquire.submit(AcceptMoneyReq(joree))
        acquire.submit(HandleStocksReq(sal, sell = 1))
        acquire.submit(HandleStocksReq(joree, sell = 4))
        acquire.submit(BuyStockReq(odo, mapOf(HotelId("worldwide") to 2)))
        acquire.submit(EndGameReq(odo))

        acquire.submit(AcceptMoneyReq(odo))
        acquire.submit(AcceptMoneyReq(joree))
        acquire.submit(AcceptMoneyReq(sal))
    }
}