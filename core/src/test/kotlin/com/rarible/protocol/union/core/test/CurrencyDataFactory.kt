package com.rarible.protocol.union.core.test

fun nativeTestCurrencies() = listOf(
    com.rarible.protocol.currency.dto.CurrencyDto(
        currencyId = "ethereum",
        address = "0x0000000000000000000000000000000000000000",
        blockchain = com.rarible.protocol.currency.dto.BlockchainDto.ETHEREUM,
    ),
    com.rarible.protocol.currency.dto.CurrencyDto(
        currencyId = "matic-network",
        address = "0x0000000000000000000000000000000000000000",
        blockchain = com.rarible.protocol.currency.dto.BlockchainDto.POLYGON,
    ),
    com.rarible.protocol.currency.dto.CurrencyDto(
        currencyId = "mantle",
        address = "0x0000000000000000000000000000000000000000",
        blockchain = com.rarible.protocol.currency.dto.BlockchainDto.MANTLE,
    ),
    com.rarible.protocol.currency.dto.CurrencyDto(
        currencyId = "flow",
        address = "A.1654653399040a61.FlowToken",
        blockchain = com.rarible.protocol.currency.dto.BlockchainDto.FLOW,
    ),
    com.rarible.protocol.currency.dto.CurrencyDto(
        currencyId = "tezos",
        address = "tz1Ke2h7sDdakHJQh8WX4Z372du1KChsksyU",
        blockchain = com.rarible.protocol.currency.dto.BlockchainDto.TEZOS,
    ),
    com.rarible.protocol.currency.dto.CurrencyDto(
        currencyId = "solana",
        address = "So11111111111111111111111111111111111111112",
        blockchain = com.rarible.protocol.currency.dto.BlockchainDto.SOLANA,
    ),
    com.rarible.protocol.currency.dto.CurrencyDto(
        currencyId = "immutable-x",
        address = "0x0000000000000000000000000000000000000000",
        blockchain = com.rarible.protocol.currency.dto.BlockchainDto.IMMUTABLEX,
    ),
)
