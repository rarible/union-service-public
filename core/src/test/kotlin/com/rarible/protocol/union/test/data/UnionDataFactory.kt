package com.rarible.protocol.union.test.data

import com.rarible.protocol.union.core.ethereum.converter.EthConverter
import com.rarible.protocol.union.core.ethereum.converter.EthItemConverter
import com.rarible.protocol.union.core.ethereum.converter.EthOrderConverter
import com.rarible.protocol.union.core.ethereum.converter.EthOwnershipConverter
import com.rarible.protocol.union.core.flow.converter.FlowItemConverter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.OwnershipIdDto
import com.rarible.protocol.union.dto.UnionItemDto
import com.rarible.protocol.union.test.mock.CurrencyMock
import kotlinx.coroutines.runBlocking

fun randomUnionItem(id: ItemIdDto): UnionItemDto {
    return when (id.blockchain) {
        BlockchainDto.ETHEREUM, BlockchainDto.POLYGON -> EthItemConverter.convert(
            randomEthNftItemDto(id),
            id.blockchain
        )
        BlockchainDto.FLOW -> FlowItemConverter.convert(
            randomFlowNftItemDto(id),
            id.blockchain
        )
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

fun randomUnionOrderDto() = runBlocking {
    mockedEthOrderConverter.convert(
        randomEthLegacyOrderDto(),
        BlockchainDto.ETHEREUM
    )
}

fun randomUnionOrderDto(itemId: ItemIdDto) = runBlocking {
    mockedEthOrderConverter.convert(
        randomEthLegacyOrderDto(itemId),
        itemId.blockchain
    )
}

fun randomUnionOrderDto(itemId: ItemIdDto, owner: String) = runBlocking {
    mockedEthOrderConverter.convert(
        randomEthLegacyOrderDto(itemId, EthConverter.convertToAddress(owner)),
        itemId.blockchain
    )
}


private val mockedEthOrderConverter = EthOrderConverter(CurrencyMock.currencyServiceMock)
