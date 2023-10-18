package com.rarible.protocol.union.core.test

import com.rarible.protocol.union.dto.BlockchainDto

fun nativeTestCurrencies() = listOf(
    com.rarible.protocol.currency.dto.CurrencyDto(
        currencyId = "ethereum",
        address = "0x0000000000000000000000000000000000000000",
        blockchain = BlockchainDto.ETHEREUM.name,
    ),
    com.rarible.protocol.currency.dto.CurrencyDto(
        currencyId = "matic-network",
        address = "0x0000000000000000000000000000000000000000",
        blockchain = BlockchainDto.POLYGON.name,
    ),
    com.rarible.protocol.currency.dto.CurrencyDto(
        currencyId = "mantle",
        address = "0x0000000000000000000000000000000000000000",
        blockchain = BlockchainDto.MANTLE.name,
    ),
    com.rarible.protocol.currency.dto.CurrencyDto(
        currencyId = "flow",
        address = "A.1654653399040a61.FlowToken",
        blockchain = BlockchainDto.FLOW.name,
    ),
    com.rarible.protocol.currency.dto.CurrencyDto(
        currencyId = "tezos",
        address = "tz1Ke2h7sDdakHJQh8WX4Z372du1KChsksyU",
        blockchain = BlockchainDto.TEZOS.name,
    ),
    com.rarible.protocol.currency.dto.CurrencyDto(
        currencyId = "solana",
        address = "So11111111111111111111111111111111111111112",
        blockchain = BlockchainDto.SOLANA.name,
    ),
    com.rarible.protocol.currency.dto.CurrencyDto(
        currencyId = "immutable-x",
        address = "0x0000000000000000000000000000000000000000",
        blockchain = BlockchainDto.IMMUTABLEX.name,
    ),
)
