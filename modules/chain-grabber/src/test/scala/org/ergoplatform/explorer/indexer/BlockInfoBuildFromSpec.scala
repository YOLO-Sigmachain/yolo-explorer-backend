package org.ergoplatform.explorer.indexer

import cats.data.NonEmptyList
import cats.effect.IO
import io.circe.Json
import org.ergoplatform.explorer.db.models.BlockStats
import org.ergoplatform.explorer.indexer.models.SlotData
import org.ergoplatform.explorer.protocol.models._
import org.ergoplatform.explorer.protocol.{constants, sigmaWrappers}
import org.ergoplatform.explorer.settings.ProtocolSettings
import org.ergoplatform.explorer.{testConstants, BlockId, BoxId, HexString, TxId}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import tofu.WithContext

class BlockInfoBuildFromSpec extends AnyFlatSpec with should.Matchers {
  import BlockInfoBuildFromSpec._

  "BlockInfoBuildFrom" should "count nothing but the emission spend at height 1" in {
    val stats = build(1, List(emissionTx(1, 177291175000000000L)))
    stats.minerAddress.unwrapped shouldBe MinerAddress
    stats.minerReward shouldBe 63750000000L
    stats.blockFee shouldBe 0L
    stats.blockCoins shouldBe 0L
    stats.totalCoinsIssued shouldBe 75000000000L
  }

  it should "find the emission spend by its NFT, not by a payment to the emission script" in {
    val lookalike = tx("aa", out(EmissionTree, 5000000000L))
    val user      = tx("bb", out(UserTree, 10000000000L), out(FeeTree, 1000000L))
    val stats     = build(24256, List(lookalike, emissionTx(24256, 175472050000000000L), user))
    stats.minerReward shouldBe 63750000000L
    stats.minerRevenue shouldBe 63751000000L
    stats.blockFee shouldBe 1000000L
    stats.blockCoins shouldBe 15000000000L
    stats.totalCoinsIssued shouldBe 1819200000000000L
  }

  it should "find the terminal emission spend (no successor, NFT burned) by its two accumulation outputs" in {
    val h     = 21023916
    val user  = tx("cc", out(UserTree, 2000000000L), out(FeeTree, 2000000L))
    val spoof = tx("dd", out(TreasuryTree, 1000000000L), out(LpTree, 1000000000L))
    val terminal = tx(
      "ee",
      out(TreasuryTree, 100000000L),
      out(LpTree, 50000000L),
      out(MinerTree, 850000000L)
    )
    val stats = build(h, List(user, spoof, terminal))
    settings.emission.terminalHeight shouldBe h.toLong
    stats.minerReward shouldBe 850000000L
    stats.blockFee shouldBe 2000000L
    stats.blockCoins shouldBe 4000000000L
    stats.totalCoinsIssued shouldBe settings.yolo.genesisValue
  }

  it should "count every non-fee output after the terminal height, where there is no emission spend" in {
    val stats = build(21023917, List(tx("ff", out(UserTree, 3000000000L), out(FeeTree, 3000000L))))
    stats.minerReward shouldBe 0L
    stats.blockFee shouldBe 3000000L
    stats.blockCoins shouldBe 3000000000L
    stats.totalCoinsIssued shouldBe settings.yolo.genesisValue
  }

  it should "count no coins in a block whose only other transaction pays a fee" in {
    val stats = build(24256, List(emissionTx(24256, 175472050000000000L), tx("ab", out(FeeTree, 4000000L))))
    stats.minerReward shouldBe 63750000000L
    stats.blockFee shouldBe 4000000L
    stats.minerRevenue shouldBe 63754000000L
    stats.blockCoins shouldBe 0L
  }
}

object BlockInfoBuildFromSpec {

  val settings: ProtocolSettings = GenesisSpec.settings

  implicit val ctx: WithContext[IO, ProtocolSettings] = GenesisSpec.ctx(settings)

  private val enc = settings.addressEncoder

  // Header key and miner reward address of block 1 on the YOLO devnet (rewardOutputScript(2880, pk))
  val MinerPk: HexString =
    HexString.fromStringUnsafe("03fb491749bf59b3023bd1cdc907836089158e061c5b8c901d75196b223aa3231e")
  val MinerAddress: String = "mPdcmK7h1xBCpEUpotRWwX99P4Ryb97UFhe7AXHSbDLJdqQyeSJ44yJuGgepASymTBqtxz27ut7ychgK"
  val MinerTree: HexString = HexString.fromStringUnsafe(
    "100204802d08cd03fb491749bf59b3023bd1cdc907836089158e061c5b8c901d75196b223aa3231eea02d192a39a8cc7a70173007301"
  )

  val EmissionTree: HexString = GenesisSpec.boxes.head.output.ergoTree
  val TreasuryTree: HexString = sigmaWrappers.addressToErgoTreeHex(testConstants.DevnetTreasuryAccumulationAddress)(enc)
  val LpTree: HexString       = sigmaWrappers.addressToErgoTreeHex(testConstants.DevnetLpAccumulationAddress)(enc)
  val FeeTree: HexString      = HexString.fromStringUnsafe(constants.FeePropositionScriptHex)
  val UserTree: HexString     = HexString.fromStringUnsafe("0008cd" + MinerPk.unwrapped)

  private def id(seed: String): String = (seed * 32).take(64)

  def out(tree: HexString, value: Long, assets: List[ApiAsset] = Nil): ApiOutput =
    ApiOutput(BoxId(id(tree.unwrapped.takeRight(2) + value.toString.takeRight(6))), value, 0, tree, assets, Map.empty)

  def tx(seed: String, first: ApiOutput, rest: ApiOutput*): ApiTransaction =
    ApiTransaction(
      TxId(id(seed)),
      NonEmptyList.one(ApiInput(BoxId(id(seed.reverse)), ApiSpendingProof(None, Json.obj()))),
      Nil,
      NonEmptyList(first, rest.toList),
      100
    )

  /** Normal-path emission spend at height `h`: successor holding the NFT, treasury, LP and miner shares. */
  def emissionTx(h: Int, successorValue: Long): ApiTransaction = {
    val split = settings.emission.splitAt(h.toLong)
    tx(
      "e" + h,
      out(EmissionTree, successorValue, List(ApiAsset(testConstants.DevnetEmissionNftId, 1L))),
      out(TreasuryTree, split.treasury),
      out(LpTree, split.lp),
      out(MinerTree, split.miner)
    )
  }

  def build(height: Int, txs: List[ApiTransaction]): BlockStats = {
    val blockId = BlockId.fromStringUnsafe(id("b" + height))
    val header = ApiHeader(
      id               = blockId,
      parentId         = BlockId.fromStringUnsafe(id("fa" + height)),
      version          = 4,
      height           = height,
      nBits            = 100,
      difficulty       = ApiDifficulty(BigDecimal(1103)),
      timestamp        = 1794827426813L,
      stateRoot        = MinerPk,
      adProofsRoot     = MinerPk,
      transactionsRoot = MinerPk,
      extensionHash    = MinerPk,
      minerPk          = MinerPk,
      w                = MinerPk,
      n                = MinerPk,
      d                = null,
      votes            = null
    )
    val block = ApiFullBlock(header, ApiBlockTransactions(blockId, txs), null, None, 1000)
    org.ergoplatform.explorer.indexer.extractors.blockInfoBuildFrom[IO].apply(SlotData(block, None)).unsafeRunSync()
  }
}
