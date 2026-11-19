package org.ergoplatform.explorer.protocol.models

import io.circe.Decoder
import org.ergoplatform.explorer.TxId

/** A box of the genesis state, as the node's `GET /utxo/genesis` returns it: an ErgoTransactionOutput plus the
  * `transactionId` (all zeros) and `index` the node gives genesis boxes.
  */
final case class ApiGenesisBox(transactionId: TxId, index: Int, output: ApiOutput)

object ApiGenesisBox {

  implicit val decoder: Decoder[ApiGenesisBox] = Decoder.instance { c =>
    for {
      txId   <- c.get[TxId]("transactionId")
      index  <- c.get[Int]("index")
      output <- c.as[ApiOutput]
    } yield ApiGenesisBox(txId, index, output)
  }
}
