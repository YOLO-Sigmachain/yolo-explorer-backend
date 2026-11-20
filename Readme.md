# Ergo Blockchain Explorer

## About this fork

This is the YOLO SigmaChain fork of [explorer-backend](https://github.com/ergoplatform/explorer-backend).

- **Starts from:** upstream commit `f811ccd55a0a3f2f38069ce8a91b09c77321c1e3`, 8 commits after tag `10.0.0`.
- **What changes:**
  - Its own emission schedule for the YOLO chain, with each emission spend split 93 / 4 / 3 between miner, treasury and liquidity fund (`protocol.yolo` settings: `treasury-percent = 4`, `lp-percent = 3`).
  - Reads the YOLO genesis boxes and NFTs, and takes chain settings such as the voting epoch length (128) from configuration.
  - Reference settings files and tests for these rules.
- **Fixed to the test genesis:** the lowest box index (four genesis boxes) and the API text "201 YOLO in genesis boxes" follow the test genesis. Both change if the launch genesis differs.
- **Compare with upstream:** Every change is in the commits after the starting commit above. GitHub's compare view against the upstream project shows them.
- **Licence:** the upstream project does not state one. Publication of this fork is pending confirmation from the upstream authors.

This repository contains 4 services:

### 1. Chain Grabber

`modules/chain-grabber`

Scans blockchain, dumps aggregated data to database

Dependencies:
- Postgres (Schema `modules/explorer-core/src/main/resources/db/V9__Schema.sql`)

### 2. Explorer API

`modules/explorer-api`

Provides a set of HTTP API methods for querying chain/off-chain data.

Specification: https://api.ergoplatform.com/docs/openapi

Dependencies:
- Postgres (Schema `modules/explorer-core/src/main/resources/db/V9__Schema.sql`)
- Redis

### 3. UTX Tracker

`modules/utx-tracker`

Dumps unconfirmed transactions from mempool to local database

Dependencies:
- Postgres (Schema `modules/explorer-core/src/main/resources/db/V9__Schema.sql`)

### 4. UTX Broadcaster

`modules/utx-broadcaster`

Broadcasts unconfirmed transactions to a set of known peers

Dependencies:
- Redis

## Assembly

Service JAR can be build with `sbt <service-name>/assembly`, assembly will appear in `modules/<service-name>/target` directory.

## Configuration

Config samples can be found in `modules/<service-name>/src/main/resources/application.conf`

### TestNet Configuration

To establish TestNet Connection modify .conf parameters in modules to:

```application.conf
 network.master-nodes = ["http://195.201.82.115:9052"]
 
 protocol.network-prefix = 16
 protocol.genesis-address = "AfYgQf5PappexKq8Vpig4vwEuZLjrq7gV97BWBVcKymTYqRzCoJLE9cDBpGHvtAAkAgQf8Yyv7NQUjSphKSjYxk3dB3W8VXzHzz5MuCcNbqqKHnMDZAa6dbHH1uyMScq5rXPLFD5P8MWkD5FGE6RbHKrKjANcr6QZHcBpppdjh9r5nra4c7dsCgULFZfWYTaYqHpx646BUHhhp8jDCHzzF33G8XfgKYo93ABqmdqagbYRzrqCgPHv5kxRmFt7Y99z26VQTgXoEmXJ2aRu6LoB59rKN47JxWGos27D79kKzJRiyYNEVzXU8MYCxtAwV"

```

## Run

SBT project:
`sbt <service-name>/run`

Assembly JAR:
`java -jar <<service-name>-assembly-*.jar>`

Tests:
`sbt test`