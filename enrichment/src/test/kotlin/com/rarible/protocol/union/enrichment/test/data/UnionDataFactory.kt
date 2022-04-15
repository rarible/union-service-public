package com.rarible.protocol.union.enrichment.test.data

import com.rarible.core.test.data.randomString
import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.core.model.UnionCollection
import com.rarible.protocol.union.core.model.UnionItem
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.core.model.UnionMetaContent
import com.rarible.protocol.union.core.model.UnionMetaContentProperties
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.BurnActivityDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.ItemDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.MetaAttributeDto
import com.rarible.protocol.union.dto.MetaContentDto
import com.rarible.protocol.union.dto.MintActivityDto
import com.rarible.protocol.union.dto.OrderListActivityDto
import com.rarible.protocol.union.dto.OrderMatchSellDto
import com.rarible.protocol.union.dto.OwnershipDto
import com.rarible.protocol.union.dto.OwnershipIdDto
import com.rarible.protocol.union.dto.TransferActivityDto
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.enrichment.converter.EnrichedItemConverter
import com.rarible.protocol.union.enrichment.converter.EnrichedOwnershipConverter
import com.rarible.protocol.union.integration.ethereum.converter.EthActivityConverter
import com.rarible.protocol.union.integration.ethereum.converter.EthAuctionConverter
import com.rarible.protocol.union.integration.ethereum.converter.EthCollectionConverter
import com.rarible.protocol.union.integration.ethereum.converter.EthConverter
import com.rarible.protocol.union.integration.ethereum.converter.EthItemConverter
import com.rarible.protocol.union.integration.ethereum.converter.EthOrderConverter
import com.rarible.protocol.union.integration.ethereum.converter.EthOwnershipConverter
import com.rarible.protocol.union.integration.ethereum.data.randomAddressString
import com.rarible.protocol.union.integration.ethereum.data.randomEthAssetErc1155
import com.rarible.protocol.union.integration.ethereum.data.randomEthAssetErc20
import com.rarible.protocol.union.integration.ethereum.data.randomEthAssetErc721
import com.rarible.protocol.union.integration.ethereum.data.randomEthAuctionDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthCollectionDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemBurnActivity
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemMintActivity
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemTransferActivity
import com.rarible.protocol.union.integration.ethereum.data.randomEthLegacySellOrderDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthNftItemDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthOrderActivityMatch
import com.rarible.protocol.union.integration.ethereum.data.randomEthOrderListActivity
import com.rarible.protocol.union.integration.ethereum.data.randomEthOwnershipDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthOwnershipId
import com.rarible.protocol.union.integration.flow.converter.FlowItemConverter
import com.rarible.protocol.union.integration.solana.converter.SolanaItemConverter
import com.rarible.protocol.union.integration.solana.data.randomSolanaTokenDto
import com.rarible.protocol.union.test.data.randomFlowNftItemDto
import com.rarible.protocol.union.test.mock.CurrencyMock
import kotlinx.coroutines.runBlocking

fun randomUnionAddress(): UnionAddress =
    UnionAddressConverter.convert(
        BlockchainDto.ETHEREUM,
        randomString()
    )

fun randomUnionCollection(id: CollectionIdDto): UnionCollection =
    EthCollectionConverter.convert(
        randomEthCollectionDto(),
        id.blockchain
    ).copy(id = id)

fun randomUnionCollection(): UnionCollection =
    EthCollectionConverter.convert(
        randomEthCollectionDto(),
        BlockchainDto.ETHEREUM
    )

fun randomUnionItem(id: ItemIdDto): UnionItem {
    return when (id.blockchain) {
        BlockchainDto.ETHEREUM, BlockchainDto.POLYGON, BlockchainDto.IMMUTABLEX -> EthItemConverter.convert(
            randomEthNftItemDto(id),
            id.blockchain
        )
        BlockchainDto.FLOW -> FlowItemConverter.convert(
            randomFlowNftItemDto(id),
            id.blockchain
        )
        BlockchainDto.TEZOS -> TODO()
        BlockchainDto.SOLANA -> SolanaItemConverter.convert(
            randomSolanaTokenDto(id),
            BlockchainDto.SOLANA
        )
    }
}

fun randomUnionMeta(): UnionMeta {
    return UnionMeta(
        name = randomString(),
        description = randomString(),
        attributes = listOf(randomUnionMetaAttribute()),
        content = listOf(),
        restrictions = listOf()
    )
}

fun randomUnionMetaAttribute(): MetaAttributeDto {
    return MetaAttributeDto(
        key = randomString(),
        value = randomString(),
        type = randomString(),
        format = randomString()
    )
}

fun randomUnionContent(properties: UnionMetaContentProperties): UnionMetaContent {
    return UnionMetaContent(
        url = randomString(),
        representation = MetaContentDto.Representation.ORIGINAL,
        properties = properties
    )
}

fun randomUnionOwnership() = EthOwnershipConverter.convert(
    randomEthOwnershipDto(randomEthOwnershipId()),
    BlockchainDto.ETHEREUM
)

fun randomUnionOwnership(itemId: ItemIdDto) = EthOwnershipConverter.convert(
    randomEthOwnershipDto(itemId),
    itemId.blockchain
)

fun randomUnionOwnership(ownershipId: OwnershipIdDto) = EthOwnershipConverter.convert(
    randomEthOwnershipDto(ownershipId),
    ownershipId.blockchain
)

fun randomUnionSellOrderDto() = runBlocking {
    mockedEthOrderConverter.convert(
        randomEthLegacySellOrderDto()
            .copy(takePrice = null, takePriceUsd = null),
        BlockchainDto.ETHEREUM
    )
}

fun randomUnionSellOrderDto(itemId: ItemIdDto) = runBlocking {
    mockedEthOrderConverter.convert(
        randomEthLegacySellOrderDto(itemId)
            .copy(takePrice = null, takePriceUsd = null),
        itemId.blockchain
    )
}

fun randomUnionSellOrderDto(itemId: ItemIdDto, owner: String) = runBlocking {
    mockedEthOrderConverter.convert(
        randomEthLegacySellOrderDto(itemId, EthConverter.convertToAddress(owner))
            .copy(takePrice = null, takePriceUsd = null),
        itemId.blockchain
    )
}

fun randomUnionBidOrderDto() = runBlocking {
    mockedEthOrderConverter.convert(
        randomEthLegacySellOrderDto()
            .copy(make = randomEthAssetErc20(), take = randomEthAssetErc721())
            .copy(makePrice = null, makePriceUsd = null),
        BlockchainDto.ETHEREUM
    )
}

fun randomUnionBidOrderDto(itemId: ItemIdDto) = runBlocking {
    mockedEthOrderConverter.convert(
        randomEthLegacySellOrderDto(itemId)
            .copy(make = randomEthAssetErc20(), take = randomEthAssetErc721())
            .copy(makePrice = null, makePriceUsd = null),
        itemId.blockchain
    )
}

fun randomUnionAuctionDto(itemId: ItemIdDto) = randomUnionAuctionDto(
    itemId.toOwnership(randomAddressString())
)

fun randomUnionAuctionDto(ownershipId: OwnershipIdDto) = runBlocking {
    mockedEthAuctionConverter.convert(
        randomEthAuctionDto(ownershipId.getItemId()),
        ownershipId.blockchain
    ).copy(seller = ownershipId.owner)
}

fun randomUnionActivityMint(itemId: ItemIdDto) = runBlocking {
    val mint = mockedEthActivityConverter.convert(
        randomEthItemMintActivity(), itemId.blockchain
    ) as MintActivityDto

    mint.copy(itemId = itemId)
}

fun randomUnionActivityOrderList(blockchain: BlockchainDto) = runBlocking {
    val list = mockedEthActivityConverter.convert(
        randomEthOrderListActivity(), blockchain
    ) as OrderListActivityDto

    list
}

fun randomUnionActivityTransfer(itemId: ItemIdDto) = runBlocking {
    val mint = mockedEthActivityConverter.convert(
        randomEthItemTransferActivity(), itemId.blockchain
    ) as TransferActivityDto

    mint.copy(itemId = itemId)
}

fun randomUnionActivityBurn(itemId: ItemIdDto) = runBlocking {
    val mint = mockedEthActivityConverter.convert(
        randomEthItemBurnActivity(), itemId.blockchain
    ) as BurnActivityDto

    mint.copy(itemId = itemId)
}

fun randomUnionActivitySale(itemId: ItemIdDto) = runBlocking {
    val swapDto = randomEthOrderActivityMatch()
    val dto = swapDto.copy(left = swapDto.left.copy(asset = randomEthAssetErc1155(itemId)))

    mockedEthActivityConverter.convert(
        dto, itemId.blockchain
    ) as OrderMatchSellDto
}

fun randomItemDto(itemId: ItemIdDto): ItemDto {
    return EnrichedItemConverter.convert(randomUnionItem(itemId))
}

fun randomOwnershipDto(ownershipId: OwnershipIdDto): OwnershipDto {
    return EnrichedOwnershipConverter.convert(randomUnionOwnership(ownershipId))
}

private val mockedEthOrderConverter = EthOrderConverter(CurrencyMock.currencyServiceMock)
private val mockedEthAuctionConverter = EthAuctionConverter(CurrencyMock.currencyServiceMock)
private val mockedEthActivityConverter = EthActivityConverter(
    CurrencyMock.currencyServiceMock, mockedEthAuctionConverter
)
