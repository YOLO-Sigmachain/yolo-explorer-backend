package org.ergoplatform.explorer.protocol

import org.ergoplatform.explorer.settings.YoloSettings
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import pureconfig.ConfigSource
import pureconfig.generic.auto._

class EmissionSpec extends AnyFlatSpec with should.Matchers {

  // the split and genesis value the services run with (explorer-core reference.conf)
  val yolo: YoloSettings = ConfigSource.default.at("protocol.yolo").loadOrThrow[YoloSettings]
  val emission           = new Emission(yolo)

  val Yolo: Long = constants.CoinsInOneErgo

  // GET /emission/at/{height} on the YOLO devnet node (ergo 6.0.8-16-714e3746, split 4/3), 19 Nov 2026:
  // (height, minerReward, treasuryReward, lpReward, blockReward, totalCoinsIssued, totalRemainCoins)
  val node: List[(Long, Long, Long, Long, Long, Long, Long)] = List(
    (0L, 0L, 0L, 0L, 0L, 0L, 177291250000000000L),
    (1L, 69750000000L, 3000000000L, 2250000000L, 75000000000L, 75000000000L, 177291175000000000L),
    (2L, 69750000000L, 3000000000L, 2250000000L, 75000000000L, 150000000000L, 177291100000000000L),
    (128L, 69750000000L, 3000000000L, 2250000000L, 75000000000L, 9600000000000L, 177281650000000000L),
    (525600L, 69750000000L, 3000000000L, 2250000000L, 75000000000L, 39420000000000000L, 137871250000000000L),
    (1051199L, 69750000000L, 3000000000L, 2250000000L, 75000000000L, 78839925000000000L, 98451325000000000L),
    (1051200L, 34875000000L, 1500000000L, 1125000000L, 37500000000L, 78839962500000000L, 98451287500000000L),
    (1051201L, 34875000000L, 1500000000L, 1125000000L, 37500000000L, 78840000000000000L, 98451250000000000L),
    (2102399L, 34875000000L, 1500000000L, 1125000000L, 37500000000L, 118259925000000000L, 59031325000000000L),
    (2102400L, 17437500000L, 750000000L, 562500000L, 18750000000L, 118259943750000000L, 59031306250000000L),
    (6307199L, 2179687500L, 93750000L, 70312500L, 2343750000L, 155216175000000000L, 22075075000000000L),
    (6307200L, 1395000000L, 60000000L, 45000000L, 1500000000L, 155216176500000000L, 22075073500000000L),
    (21023915L, 1395000000L, 60000000L, 45000000L, 1500000000L, 177291249000000000L, 1000000000L),
    (21023916L, 930000000L, 40000000L, 30000000L, 1000000000L, 177291250000000000L, 0L),
    (21023917L, 0L, 0L, 0L, 0L, 177291250000000000L, 0L),
    (30000000L, 0L, 0L, 0L, 0L, 177291250000000000L, 0L)
  )

  "Emission" should "match the node's /emission/at split, reward and supply at every sampled height" in {
    node.foreach { case (h, miner, treasury, lp, block, issued, remain) =>
      withClue(s"height $h: ") {
        emission.splitAt(h) shouldBe EmissionSplit(treasury, lp, miner)
        emission.minerRewardAt(h) shouldBe miner
        emission.emissionAt(h) shouldBe block
        emission.issuedCoinsAfterHeight(h) shouldBe issued
        emission.remainingAfter(h) shouldBe remain
      }
    }
  }

  it should "end at height 21,023,916 with 177,291,250 YOLO mined, paid block by block" in {
    emission.terminalHeight shouldBe 21023916L
    emission.issuedCoinsAfterHeight(emission.terminalHeight) shouldBe 177291250L * Yolo
    emission.issuedCoinsAfterHeight(emission.terminalHeight - 1) should be < 177291250L * Yolo
    var sum = 0L
    var h   = 1L
    while (h <= emission.terminalHeight + 10) { sum += emission.emissionAt(h); h += 1 }
    sum shouldBe 177291250L * Yolo
  }

  it should "leave total supply at 177,291,451 YOLO with the three other genesis boxes (100 + 100 + 1 YOLO)" in {
    // GET /utxo/genesis: emission box, treasury fund, LP fund, no-premine box
    val genesisBoxes = List(177291250000000000L, 100000000000L, 100000000000L, 1000000000L)
    genesisBoxes.head shouldBe yolo.genesisValue
    (emission.issuedCoinsAfterHeight(30000000L) + genesisBoxes.tail.sum) shouldBe 177291451L * Yolo
  }

  "Emission.split" should "round down treasury and LP and give the miner the residual, as emission.es" in {
    emission.split(999L) shouldBe EmissionSplit(39L, 29L, 931L)
    emission.split(1L) shouldBe EmissionSplit(0L, 0L, 1L)
  }

  it should "follow the configured percentages" in {
    val other = new Emission(yolo.copy(treasuryPercent = 12L, lpPercent = 6L))
    other.splitAt(1L) shouldBe EmissionSplit(9L * Yolo, 4500000000L, 61500000000L)
    other.issuedCoinsAfterHeight(1L) shouldBe 75L * Yolo
  }

  it should "refuse a split over 100 percent" in {
    an[IllegalArgumentException] should be thrownBy new Emission(yolo.copy(treasuryPercent = 60L, lpPercent = 50L))
  }
}
