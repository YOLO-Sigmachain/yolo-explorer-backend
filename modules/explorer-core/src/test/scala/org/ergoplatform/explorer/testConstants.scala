package org.ergoplatform.explorer

import cats.instances.try_._

import scala.util.Try

object testConstants {

  val MainNetMinerPk: HexString = HexString
    .fromString[Try](
    "0377d854c54490abc6c565d8e548d5fc92a6a6c2f4415ed96f0c340ece92e1ed2f"
    )
    .get

  // YOLO devnet (node GET /emission/scripts and /utxo/genesis, 16 Nov 2026), network prefix 16

  val DevnetEmissionNftId: TokenId =
    TokenId.fromStringUnsafe("44bc63258d371f08ba0ac4b87eb356cf8e84dbfa31afef89a5b4a953cabdad2a")

  val DevnetEmissionAddress: Address = Address.fromStringUnsafe(
    "cjJBXpeBnyebqDp7E8Tpc2ByoLgM4WrhvnveqHvikRVFstQEquepiUZjCkDRkZhHpkx7jQ7ME94npqmmgKtvhqwwdgswBpnr" +
    "r4FPSBQXQqRQCXQUduNhd2YYQ2na9S9A4k7g49MBiRxMSz8U24UX5NdYvq6XJjuR26bzTb32Be5fDkz49cNQgS5vNmdPHZxX" +
    "px1sU7Kukzzv9jnM9Qyoxx2cT3C9K3joM38kZMnmaPuZQ7rVTZGSUW8hqBrmkoztH6PtDUjWd4hRkSPYtyM1Zz8j6rtzdsG6" +
    "iWid6kEivXHjG6rssNHwHKNEBQKCrKNHptwqYjP7o62wtdHETkYspBMeCoW5X4pJxFHBHTbmTkLrDE6WbKATL5qKhZENpLay" +
    "Lt5ZDMvT8QKPR91LBRVBfvD7KTddPQUtxNbkD4ndWts9k7Vpo8zjSspqbxSZax5z2z7nsezHDT6mvo6ng1NUC7yue5MggfkA" +
    "nUDJ5FBsrqigJdhQzEp8h8kw1Ecn9Xw2MkEjwhL9hoEouk39hC4MtPPJQsyP2KgfVZseQJAu6sE8LXgA6qbmbjXu4bF3MSdt" +
    "om1CB6qJ73uKgESJyjQSxUvTFyAJT5vM9VVU415n8av3wTcKV56sa4yzSh1Vtd9JyBAo9pXpbnrxEDB51ZFQ4h6qPNHdqax4" +
    "GiHA7Jy18iqGeWoJNKcwVdsDtYGoY1hou7Cktn2uZFvLbpMSzZow8zLjEUKuDRVG9Lc6HWTt13why2Rw1wRPDCps5ZME1TU5" +
    "ZCp4kTnUUZJHFobaMU1jt76jW3pSSbd4F5c4zD45PzKK1p4mXvCKGTpfHmruCkvWJ56xjhk7q2QavvncdxfSRrj6LRxTV11Z" +
    "NNLPZRs3HRhW22PKK2sHtM7S3poG7NdfuCcj5kAExx6sfrnC33xVzum5S3rCUdURfCWLegXxCzudhNt4S78A4PmC7jtjXhdt" +
    "MdSjT"
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
