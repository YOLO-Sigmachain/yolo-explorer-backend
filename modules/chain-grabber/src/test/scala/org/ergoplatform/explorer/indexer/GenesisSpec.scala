package org.ergoplatform.explorer.indexer

import cats.data.NonEmptyList
import cats.effect.IO
import eu.timepit.refined.refineMV
import org.ergoplatform.explorer.Err.ProcessingErr.GenesisMismatch
import org.ergoplatform.explorer.SigmaType.{SCollection, STupleN}
import org.ergoplatform.explorer.SigmaType.SimpleKindSigmaType.{SByte, SLong}
import org.ergoplatform.explorer.indexer.extractors.{blockStats, genesis}
import org.ergoplatform.explorer.protocol.models.ApiGenesisBox
import org.ergoplatform.explorer.settings.{ProtocolSettings, YoloSettings}
import org.ergoplatform.explorer.{testConstants, Address, RegisterId, TokenId}
import org.ergoplatform.settings.MonetarySettings
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import pureconfig.ConfigSource
import pureconfig.generic.auto._
import tofu.{Context, WithContext}

class GenesisSpec extends AnyFlatSpec with should.Matchers {
  import GenesisSpec._

  "genesis.rows" should "file the node's genesis boxes under the zero header, below global index 0, with their tokens" in {
    implicit val c: WithContext[IO, ProtocolSettings] = ctx(settings)
    val rows                                          = genesis.rows[IO](boxes).unsafeRunSync()
    rows.outputs.map(_.boxId.value) shouldBe List(
      "fc7b9e4425d3f30992d79a1cb4c5a720a792719c5621c025f75cac23fc9d8fa1",
      "fa97b89ca4113e0f9f5d84ca583bf9543ece6c75d2e6f49635fff073404542bd",
      "fee96215874226af958ef335028bb46f7e3c226653f99ddb22c55dab804e83f4",
      "3bfaf76c824df668822dfce71abaf688d0281f91c3ac2a271f92fa28c3efaac7"
    )
    rows.outputs.map(_.value) shouldBe List(177291250000000000L, 100000000000L, 100000000000L, 1000000000L)
    rows.outputs.map(_.globalIndex) shouldBe List(-4L, -3L, -2L, -1L)
    rows.outputs.map(_.headerId).distinct shouldBe List(genesis.GenesisHeaderId)
    rows.outputs.map(_.txId).distinct shouldBe List(genesis.GenesisTxId)
    rows.outputs.map(_.settlementHeight).distinct shouldBe List(0)
    rows.outputs.forall(_.mainChain) shouldBe true
    rows.assets.map(a => (a.tokenId, a.boxId, a.amount)) shouldBe List(
      (testConstants.DevnetEmissionNftId, rows.outputs(0).boxId, 1L),
      (TreasuryNft, rows.outputs(1).boxId, 1L),
      (LpNft, rows.outputs(2).boxId, 1L)
    )
    rows.tokens.map(t => (t.id, t.boxId, t.emissionAmount)) shouldBe rows.assets.map(a => (a.tokenId, a.boxId, 1L))
    rows.registers.size shouldBe 16
    // LP fund box R4: the LP whitelist, Coll[(Coll[Byte], Long)], empty on the devnet
    rows.registers
      .filter(r => r.boxId == rows.outputs(2).boxId && r.id == RegisterId.R4)
      .map(r => (r.sigmaType, r.rawValue.unwrapped, r.renderedValue)) shouldBe List(
      (SCollection(STupleN(NonEmptyList.of(SCollection(SByte), SLong))), "0c4d0e00", "[]")
    )
  }

  "genesis.checkEmissionBox" should "accept the node's genesis when the config names its emission box" in {
    genesis.checkEmissionBox[IO](boxes)(implicitly, implicitly, ctx(settings)).attempt.unsafeRunSync() shouldBe Right(())
  }

  it should "refuse a config with another genesis-address" in {
    val wrong = settings.copy(genesisAddress = testConstants.DevnetLpAccumulationAddress)
    failure(wrong) should include("its tree is not that of genesis-address")
  }

  it should "refuse a config with another genesis value" in {
    val wrong = settings.copy(yolo = settings.yolo.copy(genesisValue = settings.yolo.genesisValue + 1))
    failure(wrong) should include("not yolo.genesis-value 177291250000000001")
  }

  it should "refuse a config with another emission NFT" in {
    failure(settings.copy(emissionNftId = TreasuryNft)) should include(s"not emission-nft-id $TreasuryNft")
  }

  it should "refuse a genesis-address that does not decode, and an empty genesis" in {
    failure(settings.copy(genesisAddress = Address.fromStringUnsafe("PutTheYoLoEmissionAddressHere"))) should include(
      "genesis-address PutTheYoLoEmissionAddressHere does not decode"
    )
    genesis
      .checkEmissionBox[IO](Nil)(implicitly, implicitly, ctx(settings))
      .attempt
      .unsafeRunSync()
      .left
      .map(_.getMessage) shouldBe Left(GenesisMismatch("the node returned no genesis boxes").getMessage)
  }

  "blockStats.saturatingAdd" should "add, and pin at Long.MaxValue instead of wrapping" in {
    blockStats.saturatingAdd(2L, 3L) shouldBe 5L
    blockStats.saturatingAdd(Long.MaxValue - 1L, 1L) shouldBe Long.MaxValue
    blockStats.saturatingAdd(Long.MaxValue - 1L, 177291451000000000L) shouldBe Long.MaxValue
    blockStats.saturatingAdd(Long.MaxValue, Long.MaxValue) shouldBe Long.MaxValue
  }
}

object GenesisSpec {

  val TreasuryNft: TokenId = TokenId.fromStringUnsafe("26adf62f0d439cd5cccaad2343e2768085d7def9a0230a9251ce8477581f9148")
  val LpNft: TokenId       = TokenId.fromStringUnsafe("d6f3e22aca5641481ec05e2f134f451bfe11e489820eb149181e59eae7ce6967")

  val settings: ProtocolSettings =
    ProtocolSettings(
      refineMV("16"),
      testConstants.DevnetEmissionAddress,
      testConstants.DevnetEmissionNftId,
      testConstants.DevnetTreasuryAccumulationAddress,
      testConstants.DevnetLpAccumulationAddress,
      MonetarySettings(minerRewardDelay = 2880),
      ConfigSource.default.at("protocol.voting-epoch-length").loadOrThrow[Int],
      ConfigSource.default.at("protocol.yolo").loadOrThrow[YoloSettings]
    )

  def ctx(s: ProtocolSettings): WithContext[IO, ProtocolSettings] = Context.const(s)

  def failure(s: ProtocolSettings): String =
    genesis.checkEmissionBox[IO](boxes)(implicitly, implicitly, ctx(s)).attempt.unsafeRunSync() match {
      case Left(e: GenesisMismatch) => e.getMessage
      case other                    => fail(s"expected GenesisMismatch, got $other")
    }

  private def fail(msg: String): Nothing = throw new AssertionError(msg)

  lazy val boxes: List[ApiGenesisBox] =
    io.circe.parser.decode[List[ApiGenesisBox]](sample.stripMargin.replace("\n", "")).fold(throw _, identity)

  // GET /utxo/genesis on the YOLO devnet node, 16 Nov 2026: the emission box, the treasury and LP fund boxes (each
  // holding its NFT) and the no-premine box
  val sample: String =
    """
      |[{"boxId":"fc7b9e4425d3f30992d79a1cb4c5a720a792719c5621c025f75cac23fc9d8fa1","value":177291250000000000,"ergoT
      |ree":"10290e2044bc63258d371f08ba0ac4b87eb356cf8e84dbfa31afef89a5b4a953cabdad2a04020480a9800104000580b8c9e5ae04
      |04020580dce4b2970204040580aeb2d98b010406058097d9ec45040805c0cbacf622040a05e0a596bb110580bcc1960b0580bcc1960b05
      |80bcc1960b0400051405c80104000402050a05c801051405c8010404050a05c801040005020e07100204802d08cd0e0eea02d192a39a8c
      |c7a701730073010400040204000400050204080406d815d6017300d602b1a5d603b2a5997202730100d604c1a7d6059da37302d6069590
      |72057303730495937205730573069593720573077308959372057309730a95937205730b730c95937205730d730e730fd6079591720673
      |1072067311d608b2a5731200d609c17208d60a9d9c720473137314d60bc27208d60ce4c6a7040ed60dd9010d63eded93b1db6308720d73
      |15efe6c6720d040e938cc7720d01a3d60eb2a5731600d60fc1720ed6109d9c720473177318d611e4c6a7050ed612db63087208d6139d9c
      |72077319731ad614b2a5731b00d6159d9c7207731c731dd1ededed93b2db6308a7731e0086027201731f91a38cc7a701eded93c27203b3
      |b37320db650afe7321938cc7720301a393b1db630872037322ecedededed9272047207edededededed93b172127323938cb27212732400
      |017201938cb2721273250002732693720bc2a79372099972047207eded93e4c67208040e720c93e4c67208050e7211efe6c67208060e93
      |8cc7720801a3eded93720f721393cbc2720e720cda720d01720eeded93c17214721593cbc272147211da720d017214ed937202732793c1
      |72039999720772137215edededed9072047207eded937209720a93cb720b720cda720d017208eded93720f721093cbc2720e7211da720d
      |01720eafa5d9011663afdb63087216d901184d0e948c7218017201ed937202732893c1720399997204720a7210","assets":[{"tokenI
      |d":"44bc63258d371f08ba0ac4b87eb356cf8e84dbfa31afef89a5b4a953cabdad2a","amount":1}],"creationHeight":0,"additio
      |nalRegisters":{"R4":"0e2065f06d8d0986c6655ba90e137d597bedf7cdbaf31e5d9020c140dcdab0161763","R5":"0e2090482c952
      |d0eb7c083da506892c5921429618c64f41b31a3e0c83dddfa655582"},"transactionId":"00000000000000000000000000000000000
      |00000000000000000000000000000","index":0},{"boxId":"fa97b89ca4113e0f9f5d84ca583bf9543ece6c75d2e6f49635fff07340
      |4542bd","value":100000000000,"ergoTree":"10330e2026adf62f0d439cd5cccaad2343e2768085d7def9a0230a9251ce8477581f9
      |14804000500058087010580bb0204000400050205040580897a040408cd034646ae5047316b4230d0086c8acec687f00b1cd9d1dc634f6
      |cb358ac0a9a8fff08cd0288e2ddeb04657dbd0edadf9c1f98da3b3895faa1f00527934dd35d17542ffe9b08cd020305c75318f36537e1a
      |5d0db4dfcdc94a9708a84a38f81ea7cfbe239252e01f2040004000502050004040514050005020580a0b787e9050580a0b787e90504a00
      |b050005000402040404020e691005040004000e36100204a00b08cd0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f28
      |15b16f81798ea02d192a39a8cc7a701730073011001020402d19683030193a38cc7b2a57300000193c2b2a57301007473027303830108c
      |deeac93b1a573040580dac4090500050005000500050005020502050005000504044004000500050005000500050005000500d811d601d
      |b6308a7d6027300d603b2a5730100d604c27203d605e4c6a70805d606e4c6a70505d6079172067302d6087ea305d6099a72067303d60ae
      |d91720872099072089a72097304d60be4c6a70705d60ce4c6a7060ed60de4c6a7040ed60edb63087203d60fed91b1720e730593b2720e7
      |30600860272027307d6109372057308d611ededededed72107207720a93cb7204720d720f92c1720399c1a77309ea0298730a830308730
      |b730c730dd1eded91b17201730e93b27201730f0086027202731095937204c2a7d80dd612c17203d613c1a7d6149372057311d615b4a57
      |312b1a5d616e4c672030505d617e4c672030805d618e4c67203040ed619e4c67203060ed61ae4c672030705d61beded937218720d93721
      |9720c93721a720bd61ced720f92c17203c1a7d61deded927216997208731390721672089172167314d61e9472057315eded927212958f7
      |213731672137317928cc772030199a37318ecececececececededededed72149072067319721c721d937217731a937218cbb372197a721
      |aededededededededededed72147207720a93720dcbb3720c7a720baeb4a5731b731cd9011f63ed93c2721f720c92c1721f720b90b1721
      |5731daf7215d9011f63ed93c2721f731e90c1721f731f720f92721299997213720bb072157320d9011f41639a8c721f01c18c721f02937
      |21673219372177322721bedededed7207721c937216732393721795721073247205721bedededed721e721c93721672069372177205721
      |bedededed721e721c93721672069372177325721bedededed9372057326721c93721672069372177327721bededededededed721490720
      |67328721c721d937217732993b17218732a93b17219732b93721a732cededed7211937216732d937217732e721beded721193e5c672030
      |505732f733093e5c67203080573317332","assets":[{"tokenId":"26adf62f0d439cd5cccaad2343e2768085d7def9a0230a9251ce8
      |477581f9148","amount":1}],"creationHeight":0,"additionalRegisters":{"R4":"0e2000000000000000000000000000000000
      |00000000000000000000000000000000","R5":"0500","R6":"0e00","R7":"0500","R8":"0500"},"transactionId":"0000000000
      |000000000000000000000000000000000000000000000000000000","index":0},{"boxId":"fee96215874226af958ef335028bb46f7
      |e3c226653f99ddb22c55dab804e83f4","value":100000000000,"ergoTree":"10300e20d6f3e22aca5641481ec05e2f134f451bfe11
      |e489820eb149181e59eae7ce696704000400040005020504050005808701058087010580bb020580897a040408cd034646ae5047316b42
      |30d0086c8acec687f00b1cd9d1dc634f6cb358ac0a9a8fff08cd0288e2ddeb04657dbd0edadf9c1f98da3b3895faa1f00527934dd35d17
      |542ffe9b08cd020305c75318f36537e1a5d0db4dfcdc94a9708a84a38f81ea7cfbe239252e01f2040004000502050204020e6910050400
      |04000e36100204a00b08cd0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798ea02d192a39a8cc7a70173
      |0073011001020402d19683030193a38cc7b2a57300000193c2b2a57301007473027303830108cdeeac93b1a573040580dac4090580a0b7
      |87e9050580a0b787e90504a00b040205808701048001040004400514050005020502050005000504051405000440050005000500050005
      |00050005000500d80dd601db6308a7d6027300d603b2a5730100d604c27203d605e4c6a70505d606db63087203d607ed91b17206730293
      |b27206730300860272027304d608e4c6a70605d609e4c6a7070ed60ae4c6a7040c4d0ed60b7ea305d60c9372057305d60dedededededed
      |720c917208730691720b9a7208730790720b9a9a72087308730993cb72047209720792c1720399c1a7730aea0298730b830308730c730d
      |730ed1eded91b17201730f93b2720173100086027202731195937204c2a7d80bd60ec1a7d60f9472057312d610ed720792c17203c1a7d6
      |11e4c672030505d612e4c672030605d613e4c67203070ed614ed93721272089372137209d615e4c67203040c4d0ed616937215720ad617
      |b4a57313b1a5d618d9011863ed93c27218731490c172187315eded92c17203958f720e7316720e7317928cc772030199a37318ecececec
      |ecececedededededed720f720772169372117205721490b1b5721772187319af7217d9011963ecda7218017219dad9011b63ae720ad901
      |1d4d0eed938c721d01cbc2721b91720b9a8c721d02731a017219ededededededed720f72109372117205721491b17215b1720a90b17215
      |731b93b47215731cb1720a720aafb47215b1720ab17215d901194d0ed801d61b8c721902ededed93b18c721901731d92721b99720b731e
      |90721b720b91721b731fedededed720f7210721693721172057214edededed720f7210721693721173207214edededed93720573217210
      |721693721173227214ededededededed937205732372107216937211732492721299720b7325907212720b917212732693b172137327ed
      |edededed720c72107216937211732893721273299372137209edededed720d7216937211732a937212732b9372137209eded720d93e5c6
      |72030505732c732d93e5c672030605732e732f","assets":[{"tokenId":"d6f3e22aca5641481ec05e2f134f451bfe11e489820eb149
      |181e59eae7ce6967","amount":1}],"creationHeight":0,"additionalRegisters":{"R4":"0c4d0e00","R5":"0500","R6":"050
      |0","R7":"0e200000000000000000000000000000000000000000000000000000000000000000"},"transactionId":"0000000000000
      |000000000000000000000000000000000000000000000000000","index":0},{"boxId":"3bfaf76c824df668822dfce71abaf688d028
      |1f91c3ac2a271f92fa28c3efaac7","value":1000000000,"ergoTree":"10010100d17300","assets":[],"creationHeight":0,"a
      |dditionalRegisters":{"R4":"0e41274368616f7320726569676e73273a20776861742074686520706170657273207361792061626f7
      |57420746865206e6f2d6465616c2042726578697420766f7465","R5":"0e49e4b9a0e8bf91e5b9b3e79a84e4b8a4e4bc9ae697b6e997b
      |47ce8bf99e9878ce69c89e4bbbde4b9a0e8bf91e5b9b3e4b8a4e4bc9ae697a5e58e86efbc8ce8afb7e69fa5e694b6efbc81","R6":"0e8
      |d01d0a2d090d0a1d0a120d181d0bed0bed0b1d189d0b8d0bb20d0bed0b120d0bed0b1d0bdd0b0d180d183d0b6d0b5d0bdd0b8d0b820d0b
      |dd0b5d181d0bad0bed0bbd18cd0bad0b8d18520d0bcd0b0d0b9d0bdd0b8d0bdd0b3d0bed0b2d18bd18520d184d0b5d180d0bc20d0bdd0b
      |020d181d182d0bed0bbd0b8d187d0bdd18bd18520d180d18bd0bdd0bad0b0d185","R7":"0e40303030303030303030303030303030303
      |03031333961336536316264353732313832376235316135333039613862666563613062386334623563303630393331","R8":"0e42307
      |86566316435383464373765373465336335303964653632356463313738393362323262373364303430623564353330326262663833323
      |0363566393238643033"},"transactionId":"0000000000000000000000000000000000000000000000000000000000000000","inde
      |x":0}]
      |"""
}
