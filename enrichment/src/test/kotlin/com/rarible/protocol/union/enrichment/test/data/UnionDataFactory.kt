package com.rarible.protocol.union.enrichment.test.data

import com.rarible.protocol.union.core.model.UnionItem
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.OwnershipIdDto
import com.rarible.protocol.union.integration.ethereum.converter.EthCollectionConverter
import com.rarible.protocol.union.integration.ethereum.converter.EthConverter
import com.rarible.protocol.union.integration.ethereum.converter.EthItemConverter
import com.rarible.protocol.union.integration.ethereum.converter.EthOrderConverter
import com.rarible.protocol.union.integration.ethereum.converter.EthOwnershipConverter
import com.rarible.protocol.union.integration.ethereum.data.randomEthAssetErc20
import com.rarible.protocol.union.integration.ethereum.data.randomEthAssetErc721
import com.rarible.protocol.union.integration.ethereum.data.randomEthCollectionDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthLegacyOrderDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthNftItemDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthOwnershipDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthOwnershipId
import com.rarible.protocol.union.integration.flow.converter.FlowItemConverter
import com.rarible.protocol.union.test.data.randomFlowNftItemDto
import com.rarible.protocol.union.test.mock.CurrencyMock
import kotlinx.coroutines.runBlocking

fun randomUnionCollection(): CollectionDto =
    EthCollectionConverter.convert(
        randomEthCollectionDto(),
        BlockchainDto.ETHEREUM
    )

fun randomUnionItem(id: ItemIdDto): UnionItem {
    return when (id.blockchain) {
        BlockchainDto.ETHEREUM, BlockchainDto.POLYGON -> EthItemConverter.convert(
            randomEthNftItemDto(id),
            id.blockchain
        )
        BlockchainDto.FLOW -> FlowItemConverter.convert(
            randomFlowNftItemDto(id),
            id.blockchain
        )
        BlockchainDto.TEZOS -> TODO()
    }
}

fun randomUnionOwnershipDto() = EthOwnershipConverter.convert(
    randomEthOwnershipDto(randomEthOwnershipId()),
    BlockchainDto.ETHEREUM
)

fun randomUnionOwnershipDto(itemId: ItemIdDto) = EthOwnershipConverter.convert(
    randomEthOwnershipDto(itemId),
    itemId.blockchain
)

fun randomUnionOwnershipDto(ownershipId: OwnershipIdDto) = EthOwnershipConverter.convert(
    randomEthOwnershipDto(ownershipId),
    ownershipId.blockchain
)

fun randomUnionSellOrderDto() = runBlocking {
    mockedEthOrderConverter.convert(
        randomEthLegacyOrderDto()
            .copy(takePrice = null, takePriceUsd = null),
        BlockchainDto.ETHEREUM
    )
}

fun randomUnionSellOrderDto(itemId: ItemIdDto) = runBlocking {
    mockedEthOrderConverter.convert(
        randomEthLegacyOrderDto(itemId)
            .copy(takePrice = null, takePriceUsd = null),
        itemId.blockchain
    )
}

fun randomUnionSellOrderDto(itemId: ItemIdDto, owner: String) = runBlocking {
    mockedEthOrderConverter.convert(
        randomEthLegacyOrderDto(itemId, EthConverter.convertToAddress(owner))
            .copy(takePrice = null, takePriceUsd = null),
        itemId.blockchain
    )
}

fun randomUnionBidOrderDto() = runBlocking {
    mockedEthOrderConverter.convert(
        randomEthLegacyOrderDto()
            .copy(make = randomEthAssetErc721(), take = randomEthAssetErc20())
            .copy(makePrice = null, makePriceUsd = null),
        BlockchainDto.ETHEREUM
    )
}

fun randomUnionBidOrderDto(itemId: ItemIdDto) = runBlocking {
    mockedEthOrderConverter.convert(
        randomEthLegacyOrderDto(itemId)
            .copy(make = randomEthAssetErc721(), take = randomEthAssetErc20())
            .copy(makePrice = null, makePriceUsd = null),
        itemId.blockchain
    )
}

fun randomUnionBidOrderDto(itemId: ItemIdDto, owner: String) = runBlocking {
    mockedEthOrderConverter.convert(
        randomEthLegacyOrderDto(itemId, EthConverter.convertToAddress(owner))
            .copy(make = randomEthAssetErc721(), take = randomEthAssetErc20())
            .copy(makePrice = null, makePriceUsd = null),
        itemId.blockchain
    )
}

private val mockedEthOrderConverter = EthOrderConverter(CurrencyMock.currencyServiceMock)
