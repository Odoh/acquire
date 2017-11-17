package us.whodag.acquire.json

import org.junit.Assert
import org.junit.Test
import us.whodag.acquire.obj.hotel.HotelId
import us.whodag.acquire.obj.player.PlayerId
import us.whodag.acquire.obj.tile.TileId
import us.whodag.acquire.req.*

class TestAcqReqJson {

    @Test
    fun all() {
        val odo = PlayerId("odo")
        val luxor = HotelId("luxor")
        val tile = TileId("4-A")

        val acceptMoneyReq = AcceptMoneyReq(odo)
        Assert.assertEquals("AcceptMoneyReqs are equal", acceptMoneyReq, parseJson(acceptMoneyReq.json().toString()).toAcceptMoneyReq())

        val acceptStockReq = AcceptStockReq(odo)
        Assert.assertEquals("AcceptStockReqs are equal", acceptStockReq, parseJson(acceptStockReq.json().toString()).toAcceptStockReq())

        val acceptUndoReq = AcceptUndoReq(odo)
        Assert.assertEquals("AcceptUndoReqs are equal", acceptUndoReq, parseJson(acceptUndoReq.json().toString()).toAcceptUndoReq())

        val buyStockReq = BuyStockReq(odo, mapOf(luxor to 3))
        Assert.assertEquals("BuyStockReqs are equal", buyStockReq, parseJson(buyStockReq.json().toString()).toBuyStockReq())

        val chooseHotelReq = ChooseHotelReq(odo, luxor)
        Assert.assertEquals("ChooseHotelReqs are equal", chooseHotelReq, parseJson(chooseHotelReq.json().toString()).toChooseHotelReq())

        val drawTileReq = DrawTileReq(odo)
        Assert.assertEquals("DrawTileReqs are equal", drawTileReq, parseJson(drawTileReq.json().toString()).toDrawTileReq())

        val endGameReq = EndGameReq(odo)
        Assert.assertEquals("EndGameReqs are equal", endGameReq, parseJson(endGameReq.json().toString()).toEndGameReq())

        val handleStocksReq = HandleStocksReq(odo, sell = 1)
        Assert.assertEquals("HandleStocksReqs are equal", handleStocksReq, parseJson(handleStocksReq.json().toString()).toHandleStocksReq())

        val placeTileReq = PlaceTileReq(odo, tile)
        Assert.assertEquals("PlaceTileReqs are equal", placeTileReq, parseJson(placeTileReq.json().toString()).toPlaceTileReq())

        val startGameReq = StartGameReq(odo)
        Assert.assertEquals("StartGameReqs are equal", startGameReq, parseJson(startGameReq.json().toString()).toStartGameReq())

        val undoReq = UndoReq(odo)
        Assert.assertEquals("UndoReqs are equal", undoReq, parseJson(undoReq.json().toString()).toUndoReq())
    }
}