package org.ergoplatform.explorer.protocol

import org.ergoplatform.explorer.settings.YoloSettings

/** Treasury, LP and miner parts of one YOLO emission spend, in nanocoins. */
final case class EmissionSplit(treasury: Long, lp: Long, miner: Long) {

  def total: Long = treasury + lp + miner
}

/** YOLO emission schedule, as the node fork computes it (`YoloEmissionRules` and
  * `EmissionApiRoute.yoloSplitAtHeight` in ergo 6.0.8 YOLO). There is no EIP-27 re-emission on YOLO.
  *
  * Curve: `halvings = height / BlocksPerHalving`; 75 YOLO, then halved for halvings 1 to 5, then the 1.5 YOLO floor.
  * The genesis emission box is spent first at height 1. At the terminal height (the first height at which the box
  * holds no more than the reward) the whole box value is split, and from the next height on nothing is paid.
  * Split: treasury `amount * treasuryPercent / 100`, LP `amount * lpPercent / 100`, miner the residual.
  */
final class Emission(settings: YoloSettings) {
  import Emission._

  require(settings.genesisValue > 0, s"genesis-value must be positive, got ${settings.genesisValue}")
  require(
    settings.treasuryPercent >= 0 && settings.lpPercent >= 0 && settings.treasuryPercent + settings.lpPercent <= 100,
    s"bad split: treasury-percent ${settings.treasuryPercent}, lp-percent ${settings.lpPercent}"
  )

  /** First height h >= 1 at which the emission box value before the spend is at most `rewardAt(h)`. */
  lazy val terminalHeight: Long = {
    @annotation.tailrec
    def loop(arm: Int, before: Long): Long = {
      val start: Long = if (arm == 0) 1L else arm.toLong * BlocksPerHalving
      val r           = if (arm < ExplicitArms) InitialReward >> arm else MinReward
      val k: Long     = if (before <= r) 0L else (before - 1) / r
      val endExclusive: Long = (arm.toLong + 1) * BlocksPerHalving
      if (arm >= ExplicitArms || start + k < endExclusive) start + k
      else loop(arm + 1, before - r * (endExclusive - start))
    }
    loop(0, settings.genesisValue)
  }

  /** Value left in the emission box once the spend at height `h` is done. */
  def remainingAfter(h: Long): Long =
    if (h <= 0) settings.genesisValue
    else if (h >= terminalHeight) 0L
    else settings.genesisValue - paidThrough(h)

  /** Coins released from the emission box at heights 1 to `h` (the node's `/emission/at/h` `totalCoinsIssued`).
    * The genesis fund boxes and the no-premine box are not counted: they are not paid out of the emission box.
    */
  def issuedCoinsAfterHeight(h: Long): Long = settings.genesisValue - remainingAfter(h)

  /** Treasury, LP and miner parts of the emission spend at height `h` (all 0 when there is none). */
  def splitAt(h: Long): EmissionSplit =
    if (h < 1) EmissionSplit(0L, 0L, 0L)
    else {
      val boxValue = remainingAfter(h - 1)
      if (boxValue <= rewardAt(h)) split(boxValue) else split(rewardAt(h))
    }

  /** Whole amount released from the emission box at height `h` (treasury + LP + miner). */
  def emissionAt(h: Long): Long = splitAt(h).total

  /** Miner part of the emission spend at height `h`. */
  def minerRewardAt(h: Long): Long = splitAt(h).miner

  def split(amount: Long): EmissionSplit = {
    val treasury = Math.multiplyExact(amount, settings.treasuryPercent) / 100L
    val lp       = Math.multiplyExact(amount, settings.lpPercent) / 100L
    EmissionSplit(treasury, lp, amount - treasury - lp)
  }
}

object Emission {

  /** emission.es `blocksPerHalving`: one 365-day year at 30 s blocks. */
  val BlocksPerHalving: Int = 1051200

  /** emission.es `initialReward`: 75 YOLO. */
  val InitialReward: Long = 75L * constants.CoinsInOneErgo

  /** emission.es `minReward`: the 1.5 YOLO floor. */
  val MinReward: Long = 1500000000L

  /** Halving arms paid before the floor (halvings 0 to 5). */
  val ExplicitArms: Int = 6

  /** Block reward at height `h` as emission.es `blockReward`, ignoring exhaustion of the box. */
  def rewardAt(h: Long): Long = {
    val halvings = h / BlocksPerHalving
    val computed = if (halvings <= 0) InitialReward else if (halvings < ExplicitArms) InitialReward >> halvings else MinReward
    math.max(computed, MinReward)
  }

  /** Sum of `rewardAt` over heights 1 to `h`. */
  def paidThrough(h: Long): Long =
    if (h < 1) 0L
    else
      (0 to ExplicitArms).foldLeft(0L) { (total, arm) =>
        val start: Long = if (arm == 0) 1L else arm.toLong * BlocksPerHalving
        val end: Long   = if (arm < ExplicitArms) (arm.toLong + 1) * BlocksPerHalving - 1 else Long.MaxValue
        val r           = if (arm < ExplicitArms) InitialReward >> arm else MinReward
        if (h >= start) Math.addExact(total, Math.multiplyExact(r, math.min(h, end) - start + 1)) else total
      }
}
