package org.ergoplatform.explorer.indexer.extractors

import cats.Monad
import cats.instances.list._
import cats.instances.try_._
import cats.syntax.traverse._
import io.circe.syntax._
import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.explorer.db.models.{Asset, BoxRegister, Output, Token}
import org.ergoplatform.explorer.protocol.models.{ApiGenesisBox, RegisterValue}
import org.ergoplatform.explorer.protocol.{constants, registers, sigmaWrappers, RegistersParser, TokenPropsParser}
import org.ergoplatform.explorer.settings.ProtocolSettings
import org.ergoplatform.explorer.Err.ProcessingErr.GenesisMismatch
import org.ergoplatform.explorer.{Address, BlockId, HexString, TokenType, TxId}
import tofu.syntax.context._
import tofu.syntax.monadic._
import tofu.syntax.raise._
import tofu.{Throws, WithContext}

import scala.util.Try

/** Rows for the boxes of the genesis state (node `GET /utxo/genesis`). No block creates them, so the indexer
  * writes them once, filed under `GenesisHeaderId` (block 1's parent id, which no header has) and always main chain.
  * They take the global indexes just below 0, and each token they hold is minted in the box that holds it.
  * There is no transaction row for them (node_transactions.header_id must name a header), so queries that take
  * an output's height or chain status from its transaction do not see them; the balance queries use the output's.
  */
object genesis {

  val GenesisHeaderId: BlockId = BlockId.fromStringUnsafe("0" * 64)

  /** Transaction id the node gives every genesis box (`ErgoState.createGenesisBox`). */
  val GenesisTxId: TxId = TxId("0" * 64)

  final case class GenesisRows(
    outputs: List[Output],
    assets: List[Asset],
    registers: List[BoxRegister],
    tokens: List[Token]
  )

  /** Fails unless the first genesis box is the emission box the config names: at `genesis-address`, holding
    * `emission-nft-id` first among its tokens, of value `yolo.genesis-value`. Block coins and the supply figures rest
    * on these, so a grabber whose config does not match the node's genesis must not index. The two accumulation
    * addresses must decode too (they find the terminal emission spend).
    */
  def checkEmissionBox[F[_]: Monad: Throws: WithContext[*[_], ProtocolSettings]](boxes: List[ApiGenesisBox]): F[Unit] =
    context.flatMap { protocolSettings =>
      implicit val e: ErgoAddressEncoder = protocolSettings.addressEncoder
      def tree(key: String, address: Address): Either[String, HexString] =
        Try(sigmaWrappers.addressToErgoTreeHex(address)).toEither.left.map(_ => s"$key $address does not decode")
      val problems = boxes.headOption.map(_.output) match {
        case None => List("the node returned no genesis boxes")
        case Some(o) =>
          val firstToken = o.assets.headOption.map(_.tokenId)
          List(
            tree("genesis-address", protocolSettings.genesisAddress) match {
              case Right(t) if t == o.ergoTree => None
              case Right(_)                    => Some("its tree is not that of genesis-address")
              case Left(err)                   => Some(err)
            },
            if (firstToken.contains(protocolSettings.emissionNftId)) None
            else Some(s"its first token is $firstToken, not emission-nft-id ${protocolSettings.emissionNftId}"),
            if (o.value == protocolSettings.yolo.genesisValue) None
            else Some(s"its value is ${o.value}, not yolo.genesis-value ${protocolSettings.yolo.genesisValue}"),
            tree("treasury-accumulation-address", protocolSettings.treasuryAccumulationAddress).left.toOption,
            tree("lp-accumulation-address", protocolSettings.lpAccumulationAddress).left.toOption
          ).flatten.map(p => s"first genesis box ${o.boxId}: $p")
      }
      if (problems.isEmpty) unit[F]
      else (GenesisMismatch(problems.mkString("; ")): Throwable).raise[F, Unit]
    }

  def rows[F[_]: Monad: Throws: WithContext[*[_], ProtocolSettings]](boxes: List[ApiGenesisBox]): F[GenesisRows] =
    context.flatMap { protocolSettings =>
      implicit val e: ErgoAddressEncoder = protocolSettings.addressEncoder
      boxes.zipWithIndex
        .traverse { case (ApiGenesisBox(txId, index, o), i) =>
          for {
            address <- sigmaWrappers
                         .ergoTreeToAddress[F](o.ergoTree)
                         .map(_.toString)
                         .flatMap(Address.fromString[F])
            scriptTemplateHash <- sigmaWrappers.deriveErgoTreeTemplateHash[F](o.ergoTree)
          } yield Output(
            o.boxId,
            txId,
            GenesisHeaderId,
            o.value,
            o.creationHeight,
            constants.PreGenesisHeight,
            index,
            i.toLong - boxes.size,
            o.ergoTree,
            scriptTemplateHash,
            address,
            registers.expand(o.additionalRegisters).asJson,
            0L,
            mainChain = true
          )
        }
        .map { outputs =>
          val outs = boxes.map(_.output)
          val assets = for {
            out            <- outs
            (asset, index) <- out.assets.zipWithIndex
          } yield Asset(asset.tokenId, out.boxId, GenesisHeaderId, index, asset.amount)
          val boxRegisters = for {
            out                           <- outs
            (id, rawValue)                <- out.additionalRegisters.toList
            RegisterValue(typeSig, value) <- RegistersParser[Try].parseAny(rawValue).toOption
          } yield BoxRegister(id, out.boxId, typeSig, rawValue, value)
          val tokens = for {
            out              <- outs
            (tokenId, items) <- out.assets.groupBy(_.tokenId).toList
            props = TokenPropsParser.eip4Partial.parse(out.additionalRegisters)
          } yield Token(
            tokenId,
            out.boxId,
            items.map(_.amount).sum,
            props.map(_.name),
            props.map(_.description),
            props.map(_ => TokenType.Eip004),
            props.map(_.decimals)
          )
          GenesisRows(outputs, assets, boxRegisters, tokens)
        }
    }
}
