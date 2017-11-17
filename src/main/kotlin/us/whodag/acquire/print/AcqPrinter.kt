package us.whodag.acquire.print

import mu.KLogging
import us.whodag.acquire.Acquire
import us.whodag.acquire.obj.AcqObj
import us.whodag.acquire.sm.AcqSmState

/**
 * Print the game state of the objects which make up an Acquire game.
 */
class AcqPrinter(private val acqObjPrinter: AcqObjPrinter,
                 private val acqSmPrinter: AcqSmPrinter) {
    constructor(acqObj: AcqObj) : this(AcqObjPrinter(acqObj), AcqSmPrinter())
    constructor(smState: AcqSmState) : this(smState.acqObj)
    constructor(acquire: Acquire) : this(acquire.state().acqSmState)

    companion object : KLogging()

    /** Print an AcqObj. */
    fun print(acqObj: AcqObj) = acqObjPrinter.print(acqObj)

    /** Print an AcqSmState. */
    fun print(smState: AcqSmState) {
        logger.debug { "--------------- Acquire State Machine State ---------------" }
        acqSmPrinter.print(smState)
        logger.debug { "--------------- Acquire Objects ---------------" }
        acqObjPrinter.print(smState.acqObj)
    }

    /** Print the current state of an Acquire game. */
    fun print(acquire: Acquire) = print(acquire.state().acqSmState)
}