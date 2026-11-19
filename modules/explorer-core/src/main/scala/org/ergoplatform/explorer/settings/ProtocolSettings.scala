package org.ergoplatform.explorer.settings

import eu.timepit.refined.api.Refined
import eu.timepit.refined.string._
import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.explorer.{Address, TokenId}
import org.ergoplatform.explorer.protocol.Emission
import org.ergoplatform.settings.MonetarySettings

/** YOLO emission inputs (`protocol.yolo`, defaults in explorer-core `reference.conf`).
  *
  * @param genesisValue    value of the genesis emission box, nanocoins (node `ergo.chain.yolo.emission.genesisValue`)
  * @param treasuryPercent treasury share of each emission spend, percent (emission.es `treasuryReward`)
  * @param lpPercent       LP share of each emission spend, percent (emission.es `lpReward`)
  */
final case class YoloSettings(genesisValue: Long, treasuryPercent: Long, lpPercent: Long)

/** @param genesisAddress              emission script address; the first genesis box must be at it
  * @param emissionNftId               token held by the emission box (node `ergo.chain.yolo.emission.emissionNftId`);
  *                                    the emission spend of a block is the transaction with an output holding it
  * @param treasuryAccumulationAddress treasury accumulation script (node `GET /emission/scripts`)
  * @param lpAccumulationAddress       LP accumulation script (node `GET /emission/scripts`); these two find the
  *                                    emission spend at the terminal height, where the NFT is burned
  */
final case class ProtocolSettings(
  networkPrefix: String Refined ValidByte,
  genesisAddress: Address,
  emissionNftId: TokenId,
  treasuryAccumulationAddress: Address,
  lpAccumulationAddress: Address,
  monetary: MonetarySettings,
  votingEpochLength: Int,
  yolo: YoloSettings
) {

  require(votingEpochLength > 0, s"voting-epoch-length must be positive, got $votingEpochLength")

  val emission = new Emission(yolo)

  val addressEncoder: ErgoAddressEncoder =
    ErgoAddressEncoder(networkPrefix.value.toByte)
}
