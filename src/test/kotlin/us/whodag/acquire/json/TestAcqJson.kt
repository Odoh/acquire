package us.whodag.acquire.json

import com.eclipsesource.json.Json
import org.junit.Assert
import org.junit.Test
import us.whodag.acquire.acq.AcqType
import us.whodag.acquire.acq.AcqVersion
import us.whodag.acquire.acq.AcquireId
import us.whodag.acquire.obj.hotel.HotelId
import us.whodag.acquire.obj.player.PlayerId
import us.whodag.acquire.obj.tile.TileId

class TestAcqJson {

    @Test
    fun all() {
        val acquireId = AcquireId("acquire-name")
        Assert.assertEquals("AcquireIds are equal", acquireId, Json.value(acquireId.json()).toAcquireId())

        val playerId = PlayerId("odo")
        Assert.assertEquals("PlayerIds are equal", playerId, Json.value(playerId.json()).toPlayerId())

        val hotelId = HotelId("luxor")
        Assert.assertEquals("HotelIds are equal", hotelId, Json.value(hotelId.json()).toHotelId())

        val tileId = TileId("10-A")
        Assert.assertEquals("TileIds are equal", tileId, Json.value(tileId.json()).toTileId())

        val acqVersion = AcqVersion(1, 2)
        Assert.assertEquals("AcqVersions are equal", acqVersion, parseJson(acqVersion.json().toString()).toAcqVersion())

        val acqTypes = enumValues<AcqType>()
        acqTypes.forEach { acqType -> Assert.assertEquals("AcqTypes are equal", acqType, parseJson(acqType.json().toString()).toAcqType()) }

        val stocks = mapOf(HotelId("tower") to 1, HotelId("luxor") to 2)
        Assert.assertEquals("Stocks are equal", stocks, parseJson(stocks.stockJson().toString()).toStocks())
    }
}
