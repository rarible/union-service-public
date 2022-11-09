package com.rarible.protocol.union.search.indexer.test

import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.randomString
import com.rarible.core.test.data.randomWord
import com.rarible.protocol.union.dto.AssetDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.BlockchainGroupDto
import com.rarible.protocol.union.dto.ContractAddress
import com.rarible.protocol.union.dto.EthErc721AssetTypeDto
import com.rarible.protocol.union.dto.EthEthereumAssetTypeDto
import com.rarible.protocol.union.dto.EthOrderDataRaribleV2DataV1Dto
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.OrderIdDto
import com.rarible.protocol.union.dto.OrderStatusDto
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.dto.UnionAddress
import java.math.BigDecimal
import java.math.BigInteger

fun orderEth() = OrderDto(
    id = OrderIdDto(
        BlockchainDto.ETHEREUM,
        randomWord()
    ),
    fill = BigDecimal.ZERO,
    platform = PlatformDto.RARIBLE,
    status = OrderStatusDto.ACTIVE,
    startedAt = null,
    endedAt = null,
    makeStock = BigDecimal.ONE,
    cancelled = false,
    createdAt = nowMillis(),
    lastUpdatedAt = nowMillis(),
    makePrice = null,
    takePrice = null,
    makePriceUsd = null,
    takePriceUsd = null,
    maker = UnionAddress(BlockchainGroupDto.ETHEREUM, randomString()),
    taker = UnionAddress(BlockchainGroupDto.ETHEREUM, randomString()),
    make = AssetDto(
        type = EthErc721AssetTypeDto(
            contract = ContractAddress(BlockchainDto.ETHEREUM, randomString()),
            tokenId = BigInteger.ZERO
        ),
        value = BigDecimal.ONE
    ),
    take = AssetDto(
        type = EthEthereumAssetTypeDto(BlockchainDto.ETHEREUM),
        value = BigDecimal.ONE
    ),
    salt = randomString(),
    signature = null,
    pending = null,
    data = EthOrderDataRaribleV2DataV1Dto(
        payouts = emptyList(),
        originFees = emptyList()
    )
)
