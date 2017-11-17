package us.whodag.acquire.print

import mu.KLogging
import us.whodag.acquire.sm.*

/**
 * Printer for print AcqSm states.
 */
class AcqSmPrinter {

    companion object : KLogging()

    private fun print(gameState: GameState) {
        logger.debug { "PlayerTurnOrder: ${gameState.playerTurnOrder}" }
        logger.debug { "CurrentPlayer: ${gameState.currentPlayer}" }
    }

    private fun print(mergeContext: MergeContext) {
        print(mergeContext.gameState)
        logger.debug { "NearbyHotels: ${mergeContext.nearbyHotels}" }
        logger.debug { "PlacedTile: ${mergeContext.placedTile}" }
    }

    private fun print(mergeState: MergeState) {
        print(mergeState.mergeContext)
        logger.debug { "SurvivingHotel: ${mergeState.survivingHotel}" }
        logger.debug { "DefunctHotel: ${mergeState.defunctHotel}" }
        logger.debug { "RemainingHotels: ${mergeState.remainingHotels}" }
    }

    private fun print(drawTurnTile: DrawTurnTile) {
        logger.debug { "PlayersDrawn: ${drawTurnTile.playersDrawn}" }
    }

    private fun print(placeTurnTile: PlaceTurnTile) {
        logger.debug { "PlayersPlaced: ${placeTurnTile.playersPlaced}" }
    }

    private fun print(drawInitialTiles: DrawInitialTiles) {
        logger.debug { "PlayerTurnOrder: ${drawInitialTiles.playerTurnOrder}" }
        logger.debug { "PlayersDrawn: ${drawInitialTiles.playersDrawn}" }
    }

    private fun print(placeTile: PlaceTile) {
        print(placeTile.gameState)
    }

    private fun print(startHotel: StartHotel) {
        print(startHotel.gameState)
        logger.debug { "Tiles: ${startHotel.tiles}" }
    }

    private fun print(foundersStock: FoundersStock) {
        print(foundersStock.gameState)
        logger.debug { "StartedHotel: ${foundersStock.startedHotel}" }
    }

    private fun print(buyStock: BuyStock) {
        print(buyStock.gameState)
    }

    private fun print(drawTile: DrawTile) {
        print(drawTile.gameState)
    }

    private fun print(endGamePayout: EndGamePayout) {
        logger.debug { "PlayersPaid: ${endGamePayout.playersPaid}" }
    }

    private fun print(gameOver: GameOver) {
        logger.debug { "PlayerResults: ${gameOver.playerResults}" }
    }

    private fun print(chooseSurvivingHotel: ChooseSurvivingHotel) {
        print(chooseSurvivingHotel.mergeContext)
        logger.debug { "PotentialSurvivingHotels: ${chooseSurvivingHotel.potentialSurvivingHotels}" }
    }

    private fun print(chooseDefunctHotel: ChooseDefunctHotel) {
        print(chooseDefunctHotel.mergeContext)
        logger.debug { "SurvivingHotel: ${chooseDefunctHotel.survivingHotel}" }
        logger.debug { "DefunctHotels: ${chooseDefunctHotel.remainingHotels}" }
        logger.debug { "PotentialNextDefunctHotels: ${chooseDefunctHotel.potentialNextDefunctHotels}" }
    }

    private fun print(payBonuses: PayBonuses) {
        print(payBonuses.mergeState)
        logger.debug { "PlayersToPay: ${payBonuses.playersToPay}" }
    }

    private fun print(handleDefunctHotelStocks: HandleDefunctHotelStocks) {
        print(handleDefunctHotelStocks.mergeState)
        logger.debug { "PlayersWithStocksInTurnOrder: ${handleDefunctHotelStocks.playersWithStockInTurnOrder}" }
    }

    /** Print the specified AcqSmState. */
    fun print(smState: AcqSmState) {
        logger.debug { "State: ${smState.javaClass.simpleName!!}" }
        when (smState) {
            is DrawTurnTile -> print(smState)
            is PlaceTurnTile -> print(smState)
            is DrawInitialTiles -> print(smState)
            is PlaceTile -> print(smState)
            is StartHotel -> print(smState)
            is FoundersStock -> print(smState)
            is BuyStock -> print(smState)
            is DrawTile -> print(smState)
            is EndGamePayout -> print(smState)
            is GameOver -> print(smState)
            is ChooseSurvivingHotel -> print(smState)
            is ChooseDefunctHotel -> print(smState)
            is PayBonuses -> print(smState)
            is HandleDefunctHotelStocks -> print(smState)
        }
    }
}