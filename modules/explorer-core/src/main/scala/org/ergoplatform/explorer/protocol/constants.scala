package org.ergoplatform.explorer.protocol

import org.ergoplatform.{ErgoScriptPredef, ErgoTreePredef}
import scorex.util.encode.Base16
import sigma.crypto.BcDlogGroup
import sigma.crypto.CryptoConstants

// Ergo protocol constants. See: https://github.com/ergoplatform/ergo/blob/master/src/main/scala/org/ergoplatform/settings/Constants.scala
object constants {

  val PreGenesisHeight = 0

  val GenesisHeight: Int = PreGenesisHeight + 1

  /** Lowest global index of a box: the genesis boxes (four on YOLO, node `/utxo/genesis`) take -4 to -1. */
  val MinBoxGlobalIndex: Long = -4L

  val PublicKeyLength = 33

  val MinerRewardDelta = 720

  val group: BcDlogGroup = CryptoConstants.dlogGroup

  val FeePropositionScriptHex: String = {
    val script = ErgoTreePredef.feeProposition(MinerRewardDelta)
    Base16.encode(script.bytes)
  }

  val CoinsInOneErgo: Long = 1000000000L

  val ErgoDecimalPlacesNum: Int = 9
}
