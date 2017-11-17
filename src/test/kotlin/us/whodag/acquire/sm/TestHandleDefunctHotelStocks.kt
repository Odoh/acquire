package us.whodag.acquire.sm

import org.junit.Assert
import org.junit.Test
import us.whodag.acquire.*
import us.whodag.acquire.obj.AcqObjs
import us.whodag.acquire.obj.bank.Banks
import us.whodag.acquire.obj.bank.Banks.withStocks
import us.whodag.acquire.obj.board.Boards
import us.whodag.acquire.obj.board.Boards.withTile
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

class TestHandleDefunctHotelStocks {

    @Test
    fun invalid() {
        val (odo, joree, sal) = TestAcqObjs.stdPlayerIds
        val hotelItr = Hotels.STANDARD_ID_TIERS.keys.iterator()
        val hotel1 = hotelItr.next()
        val hotel2 = hotelItr.next()
        val hotel3 = hotelItr.next()
        val hotel1Tiles = listOf(TileId("1-A"), TileId("2-A"), TileId("3-A"), TileId("4-A"), TileId("5-A"))
        val hotel2Tiles = listOf(TileId("1-C"), TileId("2-C"), TileId("3-C"), TileId("4-C"), TileId("5-C"))
        val hotels = Hotels.standard().map { h ->
            when {
                h.id() == hotel1 -> h.withState(HotelState.OnBoard(hotel1Tiles))
                h.id() == hotel2 -> h.withState(HotelState.OnBoard(hotel2Tiles))
                else -> h
            }
        }
        val players = listOf(Players.standardPlayer(PlayerId("odo"), initialStocks = mapOf(hotel2 to 7)),
                             Players.standardPlayer(PlayerId("joree")),
                             Players.standardPlayer(PlayerId("sal")))
        val bank = Banks.standard().withStocks(2)
        val acqObj = AcqObjs.custom(Tiles.standard(), bank, Boards.standard(), hotels, players)
        val mergeContext = MergeContext(TestAcqObjs.stdGameState,
                                        TileId("1-B"),
                                        listOf(hotel1, hotel2))
        val mergeState = MergeState(mergeContext,
                                    survivingHotel = hotel1,
                                    defunctHotel = hotel2,
                                    remainingHotels = emptyList())
        val hdhs0 = HandleDefunctHotelStocks(acqObj, mergeState, listOf(odo))

        /*
         * sal HandleDefunctHotelStocks request (expect failure: not next in order)
         */

        val (r1, s1) = hdhs0.handleDefunctHotelStocks(sal, 0, 0, 0)
        Assert.assertTrue("Expect failure", r1.isFailure())
        Assert.assertTrue("StateMachine state is unchanged", hdhs0 == s1)
        val hdhs1 = s1 as HandleDefunctHotelStocks

        /*
         * odo HandleDefunctHotelStocks request (expect failure: negative trade)
         */

        val (r2, s2) = hdhs1.handleDefunctHotelStocks(odo, -1, 0, 0)
        Assert.assertTrue("Expect failure", r2.isFailure())
        Assert.assertTrue("StateMachine state is unchanged", hdhs1 == s2)
        val hdhs2 = s2 as HandleDefunctHotelStocks

        /*
         * odo HandleDefunctHotelStocks request (expect failure: negative sell)
         */

        val (r3, s3) = hdhs2.handleDefunctHotelStocks(odo, 0, -1, 0)
        Assert.assertTrue("Expect failure", r3.isFailure())
        Assert.assertTrue("StateMachine state is unchanged", hdhs2 == s3)
        val hdhs3 = s3 as HandleDefunctHotelStocks

        /*
         * odo HandleDefunctHotelStocks request (expect failure: negative keep)
         */

        val (r4, s4) = hdhs3.handleDefunctHotelStocks(odo, 0, 0, -1)
        Assert.assertTrue("Expect failure", r4.isFailure())
        Assert.assertTrue("StateMachine state is unchanged", hdhs3 == s4)
        val hdhs4 = s4 as HandleDefunctHotelStocks

        /*
         * odo HandleDefunctHotelStocks request (expect failure: trade an odd number)
         */

        val (r5, s5) = hdhs4.handleDefunctHotelStocks(odo, 3, 0, 0)
        Assert.assertTrue("Expect failure", r5.isFailure())
        Assert.assertTrue("StateMachine state is unchanged", hdhs4 == s5)
        val hdhs5 = s5 as HandleDefunctHotelStocks

        /*
         * odo HandleDefunctHotelStocks request (expect failure: all stocks not accounted for)
         */

        val (r6, s6) = hdhs5.handleDefunctHotelStocks(odo, 0, 0, 0)
        Assert.assertTrue("Expect failure", r6.isFailure())
        Assert.assertTrue("StateMachine state is unchanged", hdhs5 == s6)
        val hdhs6 = s6 as HandleDefunctHotelStocks

        /*
         * odo HandleDefunctHotelStocks request (expect failure: trade for more bank stocks than available)
         */

        val (r7, s7) = hdhs6.handleDefunctHotelStocks(odo, 6, 0, 1)
        Assert.assertTrue("Expect failure", r7.isFailure())
        Assert.assertTrue("StateMachine state is unchanged", hdhs6 == s7)
        val hdhs7 = s7 as HandleDefunctHotelStocks
    }

    @Test
    fun keepAll() {
        val (odo, joree, sal) = TestAcqObjs.stdPlayerIds
        val mergeTile = TileId("1-B")
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
        val players = listOf(Players.standardPlayer(PlayerId("odo"), initialStocks = mapOf(hotel2 to 7)),
                             Players.standardPlayer(PlayerId("joree")),
                             Players.standardPlayer(PlayerId("sal")))
        val bank = Banks.standard().withStocks(2)
        val board = Boards.standard().withTile(mergeTile)
        val acqObj = AcqObjs.custom(Tiles.standard(), bank, board, hotels, players)
        val mergeContext = MergeContext(TestAcqObjs.stdGameState,
                                        mergeTile,
                                        listOf(hotel1, hotel2))
        val mergeState = MergeState(mergeContext,
                                    survivingHotel = hotel1,
                                    defunctHotel = hotel2,
                                    remainingHotels = emptyList())
        val postMergeTiles = hotel1Tiles + hotel2Tiles + mergeTile
        val hdhs0 = HandleDefunctHotelStocks(acqObj, mergeState, listOf(odo))

        /*
         * odo HandleDefunctHotelStocks request
         */

        val (r1, s1) = hdhs0.handleDefunctHotelStocks(odo, 0, 0, 7)
        Assert.assertTrue("Expect success", r1.isSuccess())
        Assert.assertTrue("StateMachine state is BuyStock", s1 is BuyStock)
        val bs1 = s1 as BuyStock

        // ensure state correct
        Assert.assertEquals("GameStates are equal", hdhs0.mergeState.mergeContext.gameState, bs1.gameState)

        // ensure player correct
        Assert.assertEquals("odo has the same amount of money as before", hdhs0.acqObj.player(odo).money(), bs1.acqObj.player(odo).money())
        Assert.assertEquals("odo has the same stocks as before", hdhs0.acqObj.player(odo).stocks(), bs1.acqObj.player(odo).stocks())

        // ensure bank correct
        Assert.assertEquals("Bank stocks remain unchanged", hdhs0.acqObj.bank.stocks(), bs1.acqObj.bank.stocks())

        // ensure board correct
        Assert.assertEquals("All tiles are on the board", postMergeTiles.sorted(), bs1.acqObj.board.tiles().sorted())

        // ensure hotels correct
        Assert.assertTrue("Hotel1 is on the board", bs1.acqObj.hotel(hotel1).isOnBoard())
        Assert.assertEquals("Hotel1 is made up of the merged tiles", postMergeTiles.sorted(), (bs1.acqObj.hotel(hotel1).state() as HotelState.OnBoard).tiles.sorted())
        Assert.assertTrue("Hotel2 is now available", bs1.acqObj.hotel(hotel2).isAvailable())

        // ensure tiles correct
        Assert.assertTrue("All mergedTiles are OnBoardHotel", postMergeTiles.all { tile -> bs1.acqObj.tile(tile).state() is TileState.OnBoardHotel })
        Assert.assertTrue("All mergedTiles are OnBoardHotel for hotel1", postMergeTiles.all { tile -> (bs1.acqObj.tile(tile).state() as TileState.OnBoardHotel).hotel == hotel1 })
    }

    @Test
    fun sellAll() {
        val (odo, joree, sal) = TestAcqObjs.stdPlayerIds
        val mergeTile = TileId("1-B")
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
        val players = listOf(Players.standardPlayer(PlayerId("odo"), initialStocks = mapOf(hotel2 to 7)),
                             Players.standardPlayer(PlayerId("joree")),
                             Players.standardPlayer(PlayerId("sal")))
        val bank = Banks.standard().withStocks(2)
        val board = Boards.standard().withTile(mergeTile)
        val acqObj = AcqObjs.custom(Tiles.standard(), bank, board, hotels, players)
        val mergeContext = MergeContext(TestAcqObjs.stdGameState,
                                        mergeTile,
                                        listOf(hotel1, hotel2))
        val mergeState = MergeState(mergeContext,
                                    survivingHotel = hotel1,
                                    defunctHotel = hotel2,
                                    remainingHotels = emptyList())
        val postMergeTiles = hotel1Tiles + hotel2Tiles + mergeTile
        val hdhs0 = HandleDefunctHotelStocks(acqObj, mergeState, listOf(odo))

        /*
         * odo HandleDefunctHotelStocks request
         */

        val (r1, s1) = hdhs0.handleDefunctHotelStocks(odo, 0, 7, 0)
        Assert.assertTrue("Expect success", r1.isSuccess())
        Assert.assertTrue("StateMachine state is BuyStock", s1 is BuyStock)
        val bs1 = s1 as BuyStock

        // ensure state correct
        Assert.assertEquals("GameStates are equal", hdhs0.mergeState.mergeContext.gameState, bs1.gameState)

        // ensure player correct
        Assert.assertEquals("odo's money increased by original hotel size", hdhs0.acqObj.player(odo).money() + hdhs0.acqObj.hotel(hotel2).stockPrice() * 7, bs1.acqObj.player(odo).money())
        Assert.assertEquals("odo no longer has stocks in hotel2", hdhs0.acqObj.player(odo).stock(hotel2) - 7, bs1.acqObj.player(odo).stock(hotel2))

        // ensure bank correct
        Assert.assertEquals("Bank gained 7 stocks of hotel2", hdhs0.acqObj.bank.stock(hotel2) + 7, bs1.acqObj.bank.stock(hotel2))

        // ensure board correct
        Assert.assertEquals("All tiles are on the board", postMergeTiles.sorted(), bs1.acqObj.board.tiles().sorted())

        // ensure hotels correct
        Assert.assertTrue("Hotel1 is on the board", bs1.acqObj.hotel(hotel1).isOnBoard())
        Assert.assertEquals("Hotel1 is made up of the merged tiles", postMergeTiles.sorted(), (bs1.acqObj.hotel(hotel1).state() as HotelState.OnBoard).tiles.sorted())
        Assert.assertTrue("Hotel2 is now available", bs1.acqObj.hotel(hotel2).isAvailable())

        // ensure tiles correct
        Assert.assertTrue("All mergedTiles are OnBoardHotel", postMergeTiles.all { tile -> bs1.acqObj.tile(tile).state() is TileState.OnBoardHotel })
        Assert.assertTrue("All mergedTiles are OnBoardHotel for hotel1", postMergeTiles.all { tile -> (bs1.acqObj.tile(tile).state() as TileState.OnBoardHotel).hotel == hotel1 })
    }

    @Test
    fun tradeAll() {
        val (odo, joree, sal) = TestAcqObjs.stdPlayerIds
        val mergeTile = TileId("1-B")
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
        val players = listOf(Players.standardPlayer(PlayerId("odo"), initialStocks = mapOf(hotel2 to 6)),
                             Players.standardPlayer(PlayerId("joree")),
                             Players.standardPlayer(PlayerId("sal")))
        val bank = Banks.standard().withStocks(3)
        val board = Boards.standard().withTile(mergeTile)
        val acqObj = AcqObjs.custom(Tiles.standard(), bank, board, hotels, players)
        val mergeContext = MergeContext(TestAcqObjs.stdGameState,
                                        mergeTile,
                                        listOf(hotel1, hotel2))
        val mergeState = MergeState(mergeContext,
                                    survivingHotel = hotel1,
                                    defunctHotel = hotel2,
                                    remainingHotels = emptyList())
        val postMergeTiles = hotel1Tiles + hotel2Tiles + mergeTile
        val hdhs0 = HandleDefunctHotelStocks(acqObj, mergeState, listOf(odo))

        /*
         * odo HandleDefunctHotelStocks request
         */

        val (r1, s1) = hdhs0.handleDefunctHotelStocks(odo, 6, 0, 0)
        Assert.assertTrue("Expect success", r1.isSuccess())
        Assert.assertTrue("StateMachine state is BuyStock", s1 is BuyStock)
        val bs1 = s1 as BuyStock

        // ensure state correct
        Assert.assertEquals("GameStates are equal", hdhs0.mergeState.mergeContext.gameState, bs1.gameState)

        // ensure player correct
        Assert.assertEquals("odo has the same amount of money as before", hdhs0.acqObj.player(odo).money(), bs1.acqObj.player(odo).money())
        Assert.assertEquals("odo no longer has stocks in hotel2", hdhs0.acqObj.player(odo).stock(hotel2) - 6, bs1.acqObj.player(odo).stock(hotel2))
        Assert.assertEquals("odo has more stocks in hotel1", hdhs0.acqObj.player(odo).stock(hotel1) + 3, bs1.acqObj.player(odo).stock(hotel1))

        // ensure bank correct
        Assert.assertEquals("Bank lost 3 stocks of hotel1", hdhs0.acqObj.bank.stock(hotel1) - 3, bs1.acqObj.bank.stock(hotel1))
        Assert.assertEquals("Bank gained 6 stocks of hotel2", hdhs0.acqObj.bank.stock(hotel2) + 6, bs1.acqObj.bank.stock(hotel2))

        // ensure board correct
        Assert.assertEquals("All tiles are on the board", postMergeTiles.sorted(), bs1.acqObj.board.tiles().sorted())

        // ensure hotels correct
        Assert.assertTrue("Hotel1 is on the board", bs1.acqObj.hotel(hotel1).isOnBoard())
        Assert.assertEquals("Hotel1 is made up of the merged tiles", postMergeTiles.sorted(), (bs1.acqObj.hotel(hotel1).state() as HotelState.OnBoard).tiles.sorted())
        Assert.assertTrue("Hotel2 is now available", bs1.acqObj.hotel(hotel2).isAvailable())

        // ensure tiles correct
        Assert.assertTrue("All mergedTiles are OnBoardHotel", postMergeTiles.all { tile -> bs1.acqObj.tile(tile).state() is TileState.OnBoardHotel })
        Assert.assertTrue("All mergedTiles are OnBoardHotel for hotel1", postMergeTiles.all { tile -> (bs1.acqObj.tile(tile).state() as TileState.OnBoardHotel).hotel == hotel1 })
    }

    @Test
    fun combinationOfAll() {
        val (odo, joree, sal) = TestAcqObjs.stdPlayerIds
        val mergeTile = TileId("1-B")
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
        val players = listOf(Players.standardPlayer(PlayerId("odo"), initialStocks = mapOf(hotel2 to 6)),
                             Players.standardPlayer(PlayerId("joree")),
                             Players.standardPlayer(PlayerId("sal")))
        val bank = Banks.standard().withStocks(3)
        val board = Boards.standard().withTile(mergeTile)
        val acqObj = AcqObjs.custom(Tiles.standard(), bank, board, hotels, players)
        val mergeContext = MergeContext(TestAcqObjs.stdGameState,
                                        mergeTile,
                                        listOf(hotel1, hotel2))
        val mergeState = MergeState(mergeContext,
                                    survivingHotel = hotel1,
                                    defunctHotel = hotel2,
                                    remainingHotels = emptyList())
        val postMergeTiles = hotel1Tiles + hotel2Tiles + mergeTile
        val hdhs0 = HandleDefunctHotelStocks(acqObj, mergeState, listOf(odo))

        /*
         * odo HandleDefunctHotelStocks request
         */

        val (r1, s1) = hdhs0.handleDefunctHotelStocks(odo, 2, 2,2)
        Assert.assertTrue("Expect success", r1.isSuccess())
        Assert.assertTrue("StateMachine state is BuyStock", s1 is BuyStock)
        val bs1 = s1 as BuyStock

        // ensure state correct
        Assert.assertEquals("GameStates are equal", hdhs0.mergeState.mergeContext.gameState, bs1.gameState)

        // ensure player correct
        Assert.assertEquals("odo's money increased by original hotel size", hdhs0.acqObj.player(odo).money() + hdhs0.acqObj.hotel(hotel2).stockPrice() * 2, bs1.acqObj.player(odo).money())
        Assert.assertEquals("odo has 2 stocks in hotel2", hdhs0.acqObj.player(odo).stock(hotel2) - 4, bs1.acqObj.player(odo).stock(hotel2))
        Assert.assertEquals("odo has stocks in hotel1", hdhs0.acqObj.player(odo).stock(hotel1) + 1, bs1.acqObj.player(odo).stock(hotel1))

        // ensure bank correct
        Assert.assertEquals("Bank lost 1 stocks of hotel1", hdhs0.acqObj.bank.stock(hotel1) - 1, bs1.acqObj.bank.stock(hotel1))
        Assert.assertEquals("Bank gained 4 stocks of hotel2", hdhs0.acqObj.bank.stock(hotel2) + 4, bs1.acqObj.bank.stock(hotel2))

        // ensure board correct
        Assert.assertEquals("All tiles are on the board", postMergeTiles.sorted(), bs1.acqObj.board.tiles().sorted())

        // ensure hotels correct
        Assert.assertTrue("Hotel1 is on the board", bs1.acqObj.hotel(hotel1).isOnBoard())
        Assert.assertEquals("Hotel1 is made up of the merged tiles", postMergeTiles.sorted(), (bs1.acqObj.hotel(hotel1).state() as HotelState.OnBoard).tiles.sorted())
        Assert.assertTrue("Hotel2 is now available", bs1.acqObj.hotel(hotel2).isAvailable())

        // ensure tiles correct
        Assert.assertTrue("All mergedTiles are OnBoardHotel", postMergeTiles.all { tile -> bs1.acqObj.tile(tile).state() is TileState.OnBoardHotel })
        Assert.assertTrue("All mergedTiles are OnBoardHotel for hotel1", postMergeTiles.all { tile -> (bs1.acqObj.tile(tile).state() as TileState.OnBoardHotel).hotel == hotel1 })
    }

    @Test
    fun keepAllMultiplePlayers() {
        val (odo, joree, sal) = TestAcqObjs.stdPlayerIds
        val mergeTile = TileId("1-B")
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
        val players = listOf(Players.standardPlayer(PlayerId("odo"), initialStocks = mapOf(hotel2 to 7)),
                             Players.standardPlayer(PlayerId("joree"), initialStocks = mapOf(hotel2 to 6)),
                             Players.standardPlayer(PlayerId("sal")))
        val bank = Banks.standard().withStocks(2)
        val board = Boards.standard().withTile(mergeTile)
        val acqObj = AcqObjs.custom(Tiles.standard(), bank, board, hotels, players)
        val mergeContext = MergeContext(TestAcqObjs.stdGameState,
                                        mergeTile,
                                        listOf(hotel1, hotel2))
        val mergeState = MergeState(mergeContext,
                                    survivingHotel = hotel1,
                                    defunctHotel = hotel2,
                                    remainingHotels = emptyList())
        val postMergeTiles = hotel1Tiles + hotel2Tiles + mergeTile
        val hdhs0 = HandleDefunctHotelStocks(acqObj, mergeState, listOf(odo, joree))

        /*
         * odo HandleDefunctHotelStocks request
         */

        val (r1, s1) = hdhs0.handleDefunctHotelStocks(odo, 0, 0, 7)
        Assert.assertTrue("Expect success", r1.isSuccess())
        Assert.assertTrue("StateMachine state is HandleDefunctHotelStocks", s1 is HandleDefunctHotelStocks)
        val hdhs1 = s1 as HandleDefunctHotelStocks

        // ensure state correct
        Assert.assertEquals("MergeStates are equal", hdhs0.mergeState, hdhs1.mergeState)
        Assert.assertEquals("Player with stocks in turn order removed odo", (hdhs0.playersWithStockInTurnOrder - odo), hdhs1.playersWithStockInTurnOrder)
        Assert.assertEquals("joree is the next player to handle stocks", joree, hdhs1.playersWithStockInTurnOrder[0])

        /*
         * joree HandleDefunctHotelStocks request
         */

        val (r2, s2) = hdhs1.handleDefunctHotelStocks(joree, 0, 6,0)
        Assert.assertTrue("Expect success", r1.isSuccess())
        Assert.assertTrue("StateMachine state is BuyStock", s2 is BuyStock)
        val bs2 = s2 as BuyStock

        // ensure state correct
        Assert.assertEquals("GameStates are equal", hdhs1.mergeState.mergeContext.gameState, bs2.gameState)

        // ensure player correct
        Assert.assertEquals("joree money increased by stock sold", hdhs1.acqObj.player(joree).money() + hdhs1.acqObj.hotel(hotel2).stockPrice() * 6, bs2.acqObj.player(joree).money())
        Assert.assertEquals("joree sold all stocks of hotel2", hdhs1.acqObj.player(joree).stock(hotel2) - 6, bs2.acqObj.player(joree).stock(hotel2))

        // ensure bank correct
        Assert.assertEquals("Bank gained 6 stocks of hotel2", hdhs0.acqObj.bank.stock(hotel2) + 6, bs2.acqObj.bank.stock(hotel2))

        // ensure board correct
        Assert.assertEquals("All tiles are on the board", postMergeTiles.sorted(), bs2.acqObj.board.tiles().sorted())

        // ensure hotels correct
        Assert.assertTrue("Hotel1 is on the board", bs2.acqObj.hotel(hotel1).isOnBoard())
        Assert.assertEquals("Hotel1 is made up of the merged tiles", postMergeTiles.sorted(), (bs2.acqObj.hotel(hotel1).state() as HotelState.OnBoard).tiles.sorted())
        Assert.assertTrue("Hotel2 is now available", bs2.acqObj.hotel(hotel2).isAvailable())

        // ensure tiles correct
        Assert.assertTrue("All mergedTiles are OnBoardHotel", postMergeTiles.all { tile -> bs2.acqObj.tile(tile).state() is TileState.OnBoardHotel })
        Assert.assertTrue("All mergedTiles are OnBoardHotel for hotel1", postMergeTiles.all { tile -> (bs2.acqObj.tile(tile).state() as TileState.OnBoardHotel).hotel == hotel1 })
    }
}
