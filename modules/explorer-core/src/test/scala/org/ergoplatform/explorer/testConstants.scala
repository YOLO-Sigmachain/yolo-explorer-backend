package org.ergoplatform.explorer

import cats.instances.try_._

import scala.util.Try

object testConstants {

  val MainNetMinerPk: HexString = HexString
    .fromString[Try](
      "0377d854c54490abc6c565d8e548d5fc92a6a6c2f4415ed96f0c340ece92e1ed2f"
    )
    .get

  // YOLO devnet (node GET /emission/scripts and /utxo/genesis, 19 Nov 2026, 93 / 4 / 3 build), network prefix 16

  val DevnetEmissionNftId: TokenId =
    TokenId.fromStringUnsafe("44bc63258d371f08ba0ac4b87eb356cf8e84dbfa31afef89a5b4a953cabdad2a")

  val DevnetEmissionAddress: Address = Address.fromStringUnsafe(
    "cjJBXpeBnyebqDp7E8Tpc2ByoLgM4WrhvnveqHvikRVFstQEquepiUZjCkDRkZhHpkx7jQ7ME94npqmmgKtvhqwwdgswBpnr" +
    "r4FPSBQXQqRQCXQUduNhd2YYQ2na9S9A4k7g49MBiRxMSz8U24UX5NdYvq6XJjqGTY22cRPGkurUqwnh5CzF4x8rUfB6C1ob" +
    "DWapvbt1TJHcNozSSCdzRBMAg3qoLrj5jcWtsY3EuVKSpSJdkpSubKQqMFYJUywSyjbWWhCcxqkqLejKP5XJzSFqN5m9YJsf" +
    "JVaAgPqVhZuSfHgPpb1mQWh5hxDTo68D3vqrzr2oQNviCJyX5jY7swUYWqs5WBCfyCdmz3pqvFCdqU9wwfH7TzMzGhy3doSM" +
    "NZ8jg7WHjTLDXbLvDBCy5i2wVTBCCq4j2tddfbkpAizwnCtY1rpjkefxwUT5vwxG4xbD3SJk3EdvSBMvxDFAh15akiwkdUho" +
    "SoQb8AsvpzepbSa9GFmJVEvxLDG3XU4RH7LzSSZMAXwvxRCt7GtCJ4bbfwbx8x14HxWmqPt415FRNScDACCtBhLNB7F9v2dN" +
    "g9gnT2fAanTfwVFZTNbKxm1cCTLyChjECy5w1Zj2BE7VhSJHqzaPYQ3yS5ZZGSSwa4ggVvRvJjyKZgZEPYAPgPpEkA9xo81z" +
    "kDJJTgd4G9ou12ydL8jYxoJBBB1rLerzZA6fe1z3P9WbZ9oxBkWe8X6q5LDTMPXWRTU4br969NDNfCBV4jqvQ9nnCJ3D4MGm" +
    "3CwT2qi5ToAUy4VEvc2wBbVmtPeSVYDvQnd2DB3rrmmNpLhzUwh4fhupQfSRpBka1for2ikbM32QYNHexx4XZrENh37simRG" +
    "dmaCV3Tz5g2tSZLEsHwopzgrCJPLBVd9xqpgEvLoWwgtHD75Sob4GoM6UqLgzCg7YUQ7SArc3rn7SJAzcV4cfnFQhw53V6Rn" +
    "cNy8p"
  )

  val DevnetTreasuryAccumulationAddress: Address = Address.fromStringUnsafe(
    "8ZSexs2sWGRRjFSWMvXi8F5CGhRYZZgnVKHmXVNMAoygEgNeHDkzJ1f2aNQbPum4NVxvTjyahrQnA8rtVP2YgPk8d85HvVhv" +
    "j2dA2Akia8UbTnQubChtYGe1vE9ibMmst1rrxceiN6tiXxyD8bnzXfE6qhzkjzLnLWCi1E2jeemK7qhAmoKei5mqHHBGd6EZ" +
    "4Uri35NnvRBnfAg5TK5JhQpGaJzSQUn3tMu4c88k244fo69Dp6CMoHspKJuFqaj5JH9UQHHV5bT5tSAEBe55MevSeJYeuaQ2" +
    "vrA1K4h88ZUMh3jenJSfds1eXvwGvjKiE8N35X2ck9qi6E9Afo9rWtnXnTef7dyKgNbYFpSekG8PCg1oQbqiyga3p2r4RCTq" +
    "tyAdMicrjDRv18kW"
  )

  val DevnetLpAccumulationAddress: Address = Address.fromStringUnsafe(
    "8ZSexs2sWGRRjFTcE8LKwJ23gpLWKdropHYiVHNfTTCP3kRAjV6CaMEAXz6prAoMr9Zbff3NSuEAZFMrECBZ7MdqyuXrMVQJ" +
    "dWKZiuFRNm1Yk2ALsM6XtrFXexTAap6KxxKkemR9dxkww6HHAd6rhZC43fGjJgifRoDoF7ybBhBks2yVd5kpvb2Xf5hEHwg5" +
    "VpUNnEZNxPtY1fxfPmBTgDiJwNMoFTL2fNYxKRXQ4RskHwqnLF5zgHLXqNLzd3iCr1QbeA8uqNEAg6KxGPqAo8zVTK1ay1sd" +
    "RmNJBjewiM1Yoj6u8mbiDdddTLYquXWHwEPDpVMisZWCRFCr7rkH5cwqmztzL6duZ5GjswXAbBCemFMoj376AnJxQZ11Cmkp" +
    "kHreWUjh1eGvRGan"
  )
}
