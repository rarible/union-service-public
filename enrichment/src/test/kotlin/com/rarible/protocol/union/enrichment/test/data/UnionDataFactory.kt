package com.rarible.protocol.union.enrichment.test.data

import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomBigInt
import com.rarible.core.test.data.randomBoolean
import com.rarible.core.test.data.randomDouble
import com.rarible.core.test.data.randomInt
import com.rarible.core.test.data.randomLong
import com.rarible.core.test.data.randomString
import com.rarible.protocol.dto.OrderRaribleV2DataV1Dto
import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.core.model.MetaSource
import com.rarible.protocol.union.core.model.UnionBurnActivity
import com.rarible.protocol.union.core.model.UnionCollection
import com.rarible.protocol.union.core.model.UnionCollectionMeta
import com.rarible.protocol.union.core.model.UnionImageProperties
import com.rarible.protocol.union.core.model.UnionItem
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.core.model.UnionMetaAttribute
import com.rarible.protocol.union.core.model.UnionMetaContent
import com.rarible.protocol.union.core.model.UnionMetaContentProperties
import com.rarible.protocol.union.core.model.UnionMintActivity
import com.rarible.protocol.union.core.model.UnionOrderBidActivity
import com.rarible.protocol.union.core.model.UnionOrderListActivity
import com.rarible.protocol.union.core.model.UnionOrderMatchSell
import com.rarible.protocol.union.core.model.UnionTransferActivity
import com.rarible.protocol.union.core.model.elastic.EsActivity
import com.rarible.protocol.union.core.model.elastic.EsActivityLite
import com.rarible.protocol.union.core.model.elastic.EsCollection
import com.rarible.protocol.union.core.model.elastic.EsCollectionLite
import com.rarible.protocol.union.core.model.elastic.EsItem
import com.rarible.protocol.union.core.model.elastic.EsItemLite
import com.rarible.protocol.union.core.model.elastic.EsTrait
import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.BlockchainGroupDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.EthSudoSwapAmmDataV1Dto
import com.rarible.protocol.union.dto.ItemDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.MetaContentDto
import com.rarible.protocol.union.dto.OwnershipDto
import com.rarible.protocol.union.dto.OwnershipIdDto
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.dto.SudoSwapCurveTypeDto
import com.rarible.protocol.union.dto.SudoSwapPoolTypeDto
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.enrichment.converter.ItemDtoConverter
import com.rarible.protocol.union.enrichment.converter.OrderDtoConverter
import com.rarible.protocol.union.enrichment.converter.OwnershipDtoConverter
import com.rarible.protocol.union.enrichment.download.DownloadEntry
import com.rarible.protocol.union.enrichment.download.DownloadStatus
import com.rarible.protocol.union.enrichment.download.DownloadTask
import com.rarible.protocol.union.enrichment.download.DownloadTaskSource
import com.rarible.protocol.union.enrichment.meta.simplehash.SimpleHashItem
import com.rarible.protocol.union.integration.ethereum.converter.EthActivityConverter
import com.rarible.protocol.union.integration.ethereum.converter.EthAuctionConverter
import com.rarible.protocol.union.integration.ethereum.converter.EthCollectionConverter
import com.rarible.protocol.union.integration.ethereum.converter.EthConverter
import com.rarible.protocol.union.integration.ethereum.converter.EthItemConverter
import com.rarible.protocol.union.integration.ethereum.converter.EthOrderConverter
import com.rarible.protocol.union.integration.ethereum.converter.EthOwnershipConverter
import com.rarible.protocol.union.integration.ethereum.data.randomAddressString
import com.rarible.protocol.union.integration.ethereum.data.randomEthAddress
import com.rarible.protocol.union.integration.ethereum.data.randomEthAssetErc1155
import com.rarible.protocol.union.integration.ethereum.data.randomEthAssetErc20
import com.rarible.protocol.union.integration.ethereum.data.randomEthAssetErc721
import com.rarible.protocol.union.integration.ethereum.data.randomEthAuctionDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthCollectionDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemBurnActivity
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemMintActivity
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemTransferActivity
import com.rarible.protocol.union.integration.ethereum.data.randomEthNftItemDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthOrderActivityMatch
import com.rarible.protocol.union.integration.ethereum.data.randomEthOrderBidActivity
import com.rarible.protocol.union.integration.ethereum.data.randomEthOrderListActivity
import com.rarible.protocol.union.integration.ethereum.data.randomEthOwnershipDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthOwnershipId
import com.rarible.protocol.union.integration.ethereum.data.randomEthPartDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthSellOrderDto
import com.rarible.protocol.union.integration.flow.converter.FlowItemConverter
import com.rarible.protocol.union.integration.flow.data.randomFlowNftItemDto
import com.rarible.protocol.union.integration.solana.converter.SolanaItemConverter
import com.rarible.protocol.union.integration.solana.data.randomSolanaTokenDto
import com.rarible.protocol.union.test.mock.CurrencyMock
import kotlinx.coroutines.runBlocking
import java.time.Instant
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

val CUSTOM_COLLECTION = CollectionIdDto(BlockchainDto.ETHEREUM, "0x7777777777777777777777777777777777777777")
val CUSTOM_COLLECTION_INCLUDE = CollectionIdDto(BlockchainDto.ETHEREUM, "0x8888888888888888888888888888888888888888")

fun randomUnionAddress(): UnionAddress =
    UnionAddressConverter.convert(
        BlockchainDto.ETHEREUM,
        randomAddressString().lowercase()
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

fun randomUnionItem(id: ItemIdDto = randomEthItemId()): UnionItem {
    return when (id.blockchain) {
        BlockchainDto.ETHEREUM,
        BlockchainDto.POLYGON,
        BlockchainDto.IMMUTABLEX,
        BlockchainDto.MANTLE,
        BlockchainDto.ARBITRUM,
        BlockchainDto.CHILIZ,
        BlockchainDto.ZKSYNC,
        BlockchainDto.ZKEVM,
        BlockchainDto.ASTAR,
        BlockchainDto.BASE,
        BlockchainDto.LIGHTLINK -> EthItemConverter.convert(
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

fun randomUnionMeta(
    content: List<UnionMetaContent> = emptyList(),
    attributes: List<UnionMetaAttribute> = listOf(randomUnionMetaAttribute()),
    source: MetaSource? = MetaSource.ORIGINAL,
    contributors: List<MetaSource> = listOf()
): UnionMeta {
    return UnionMeta(
        name = randomString(),
        description = randomString(),
        language = randomString(2),
        genres = listOf(randomString(), randomString()),
        tags = listOf(randomString(), randomString()),
        createdAt = nowMillis(),
        rights = randomString(),
        rightsUri = randomString(),
        externalUri = randomString(),
        originalMetaUri = randomString(),
        attributes = attributes,
        content = content,
        source = source,
        contributors = contributors
    )
}

fun randomUnionCollectionMeta(): UnionCollectionMeta {
    return UnionCollectionMeta(
        name = randomString(),
        description = randomString(),
        language = randomString(2),
        genres = listOf(randomString(), randomString()),
        tags = listOf(randomString(), randomString()),
        createdAt = nowMillis(),
        rights = randomString(),
        rightsUri = randomString(),
        externalUri = randomString(),
        originalMetaUri = randomString(),
        content = listOf(),
        feeRecipient = randomUnionAddress(),
        sellerFeeBasisPoints = randomInt(10000)
    )
}

fun randomUnionMetaAttribute(): UnionMetaAttribute {
    return UnionMetaAttribute(
        key = randomString(),
        value = randomString(),
        type = randomString(),
        format = randomString()
    )
}

fun randomUnionContent(properties: UnionMetaContentProperties? = null): UnionMetaContent {
    return UnionMetaContent(
        url = "http://localhost:8080/image/${randomString()}",
        fileName = "${randomString()}.png}",
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

fun randomUnionSellOrder(
    itemId: ItemIdDto = randomEthItemId(),
    owner: String = randomAddressString(),
    origins: List<String> = emptyList()
) = runBlocking {
    val originFees = origins.map { randomEthPartDto(EthConverter.convertToAddress(it)) }
    val data = OrderRaribleV2DataV1Dto(originFees = originFees, payouts = emptyList())
    mockedEthOrderConverter.convert(
        randomEthSellOrderDto(itemId, EthConverter.convertToAddress(owner), data)
            .copy(takePrice = null, takePriceUsd = null),
        itemId.blockchain
    )
}

fun randomSellOrderDto(
    itemId: ItemIdDto = randomEthItemId(),
    owner: String = randomAddressString(),
    origins: List<String> = emptyList()
) = randomUnionSellOrder(itemId, owner, origins).let { OrderDtoConverter.convert(it) }

fun randomUnionBidOrder(
    itemId: ItemIdDto = randomEthItemId(),
    owner: String = randomAddressString(),
    origins: List<String> = emptyList()
) = runBlocking {
    val originFees = origins.map { randomEthPartDto(EthConverter.convertToAddress(it)) }
    val data = OrderRaribleV2DataV1Dto(originFees = originFees, payouts = emptyList())
    mockedEthOrderConverter.convert(
        randomEthSellOrderDto(itemId, EthConverter.convertToAddress(owner), data)
            .copy(make = randomEthAssetErc20(), take = randomEthAssetErc721())
            .copy(makePrice = null, makePriceUsd = null),
        itemId.blockchain
    )
}

fun randomBidOrderDto(
    itemId: ItemIdDto = randomEthItemId(),
    owner: String = randomAddressString(),
    origins: List<String> = emptyList()
) = randomUnionBidOrder(itemId, owner, origins).let { OrderDtoConverter.convert(it) }

fun randomUnionAuctionDto(itemId: ItemIdDto) = randomUnionAuctionDto(
    itemId.toOwnership(randomAddressString())
)

fun randomUnionAuctionDto(ownershipId: OwnershipIdDto) = runBlocking {
    mockedEthAuctionConverter.convert(
        randomEthAuctionDto(ownershipId.getItemId()),
        ownershipId.blockchain
    ).copy(seller = ownershipId.owner)
}

fun randomUnionActivityMint(
    itemId: ItemIdDto = randomEthItemId(),
    owner: UnionAddress = randomUnionAddress()
) = runBlocking {
    val mint = mockedEthActivityConverter.convert(
        randomEthItemMintActivity(), itemId.blockchain
    ) as UnionMintActivity

    mint.copy(itemId = itemId, owner = owner)
}

fun randomUnionActivityOrderList(
    blockchain: BlockchainDto = BlockchainDto.ETHEREUM
) = runBlocking {
    val list = mockedEthActivityConverter.convert(
        randomEthOrderListActivity(), blockchain
    ) as UnionOrderListActivity

    list
}

fun randomUnionActivityOrderBid(blockchain: BlockchainDto) = runBlocking {
    val list = mockedEthActivityConverter.convert(
        randomEthOrderBidActivity(), blockchain
    ) as UnionOrderBidActivity

    list
}

fun randomUnionActivityTransfer(
    itemId: ItemIdDto = randomEthItemId()
) = runBlocking {
    val mint = mockedEthActivityConverter.convert(
        randomEthItemTransferActivity(), itemId.blockchain
    ) as UnionTransferActivity

    mint.copy(itemId = itemId)
}

fun randomUnionActivityBurn(
    itemId: ItemIdDto = randomEthItemId()
) = runBlocking {
    val mint = mockedEthActivityConverter.convert(
        randomEthItemBurnActivity(), itemId.blockchain
    ) as UnionBurnActivity

    mint.copy(itemId = itemId)
}

fun randomUnionActivitySale(
    itemId: ItemIdDto = randomEthItemId()
) = runBlocking {
    val swapDto = randomEthOrderActivityMatch()
    val dto = swapDto.copy(left = swapDto.left.copy(asset = randomEthAssetErc1155(itemId)))

    mockedEthActivityConverter.convert(
        dto, itemId.blockchain
    ) as UnionOrderMatchSell
}

fun randomItemDto(itemId: ItemIdDto): ItemDto {
    return ItemDtoConverter.convert(randomUnionItem(itemId))
}

fun randomOwnershipDto(ownershipId: OwnershipIdDto): OwnershipDto {
    return OwnershipDtoConverter.convert(randomUnionOwnership(ownershipId))
}

fun randomEsActivity() = EsActivity(
    activityId = randomString(),
    date = nowMillis().truncatedTo(ChronoUnit.MILLIS),
    blockNumber = randomLong(),
    logIndex = randomInt(),
    blockchain = BlockchainDto.values().random(),
    type = ActivityTypeDto.values().random(),
    userFrom = randomString(),
    userTo = randomString(),
    collection = randomString(),
    item = randomString(),
)

fun randomEsCollection() = EsCollection(
    collectionId = randomString(),
    date = nowMillis(),
    blockchain = BlockchainDto.values().random(),
    name = randomString(),
    symbol = randomString(),
    owner = randomString(),
    meta = EsCollection.CollectionMeta(
        name = randomString(),
    ),
)

fun randomEsItem(): EsItem {
    val itemId = ItemIdDto(BlockchainDto.ETHEREUM, randomAddress().prefixed(), randomBigInt())
    return EsItem(
        id = itemId.value,
        itemId = itemId.fullId(),
        blockchain = itemId.blockchain,
        collection = CollectionIdDto(BlockchainDto.ETHEREUM, randomAddress().prefixed()).fullId(),
        name = randomString(),
        description = randomString(),
        traits = listOf(EsTrait(randomString(), randomInt().toString()), EsTrait(randomString(), randomString())),
        creators = listOf(randomString()),
        mintedAt = nowMillis(),
        lastUpdatedAt = nowMillis(),
        bestSellAmount = randomDouble(),
        bestSellCurrency = randomString(),
        bestSellMarketplace = randomMarketplace().name,
        bestBidAmount = randomDouble(),
        bestBidCurrency = randomString(),
        bestBidMarketplace = randomMarketplace().name,
    )
}

fun randomEsItemLite(): EsItemLite {
    val itemId = ItemIdDto(BlockchainDto.ETHEREUM, randomAddress().prefixed(), randomBigInt())
    return EsItemLite(
        id = itemId.value,
        itemId = itemId.fullId(),
        blockchain = itemId.blockchain,
    )
}

val EsActivity.info: EsActivityLite
    get() = EsActivityLite(activityId, blockchain, type, date, blockNumber, logIndex, salt)

val EsCollection.info: EsCollectionLite
    get() = EsCollectionLite(collectionId, date, salt)

private val mockedEthOrderConverter = EthOrderConverter(CurrencyMock.currencyServiceMock)
private val mockedEthAuctionConverter = EthAuctionConverter(CurrencyMock.currencyServiceMock)
private val mockedEthActivityConverter = EthActivityConverter(
    mockedEthAuctionConverter
)

fun randomItemMetaDownloadEntry(
    id: String = randomEthItemId().fullId(),
    status: DownloadStatus = DownloadStatus.SUCCESS,
    data: UnionMeta? = randomUnionMeta(),
    downloads: Int = 1,
    fails: Int = 1,
    retries: Int = 0,
    scheduledAt: Instant? = nowMillis().minusSeconds(60),
    updatedAt: Instant? = nowMillis().minusSeconds(20),
    succeedAt: Instant? = nowMillis().minusSeconds(20),
    retriedAt: Instant? = nowMillis().minusSeconds(30),
    failedAt: Instant? = nowMillis().minusSeconds(40),
    errorMessage: String? = "Error: ${randomString()}"
): DownloadEntry<UnionMeta> {
    return DownloadEntry(
        id = id,
        status = status,
        data = data,
        downloads = downloads,
        fails = fails,
        retries = retries,
        scheduledAt = scheduledAt,
        updatedAt = updatedAt,
        succeedAt = succeedAt,
        failedAt = failedAt,
        retriedAt = retriedAt,
        errorMessage = errorMessage
    )
}

fun randomCollectionMetaDownloadEntry(
    id: String = randomEthItemId().fullId(),
    status: DownloadStatus = DownloadStatus.SUCCESS,
    data: UnionCollectionMeta? = randomUnionCollectionMeta(),
    downloads: Int = 1,
    fails: Int = 1,
    retries: Int = 0,
    scheduledAt: Instant? = nowMillis().minusSeconds(60),
    updatedAt: Instant? = nowMillis().minusSeconds(20),
    succeedAt: Instant? = nowMillis().minusSeconds(20),
    retriedAt: Instant? = nowMillis().minusSeconds(30),
    failedAt: Instant? = nowMillis().minusSeconds(40),
    errorMessage: String? = "Error: ${randomString()}"
): DownloadEntry<UnionCollectionMeta> {
    return DownloadEntry(
        id = id,
        status = status,
        data = data,
        downloads = downloads,
        fails = fails,
        retries = retries,
        scheduledAt = scheduledAt,
        updatedAt = updatedAt,
        succeedAt = succeedAt,
        failedAt = failedAt,
        retriedAt = retriedAt,
        errorMessage = errorMessage
    )
}

fun randomSudoSwapAmmDataV1Dto(): EthSudoSwapAmmDataV1Dto {
    return EthSudoSwapAmmDataV1Dto(
        poolAddress = UnionAddress(BlockchainGroupDto.ETHEREUM, randomEthAddress()),
        bondingCurve = UnionAddress(BlockchainGroupDto.ETHEREUM, randomEthAddress()),
        curveType = SudoSwapCurveTypeDto.LINEAR,
        assetRecipient = UnionAddress(BlockchainGroupDto.ETHEREUM, randomEthAddress()),
        poolType = SudoSwapPoolTypeDto.TRADE,
        delta = randomBigInt(),
        fee = randomBigInt(),
        feeDecimal = randomInt()
    )
}

fun randomMarketplace(): PlatformDto {
    return PlatformDto.values().random()
}

fun randomUnionImageProperties(): UnionImageProperties {
    return UnionImageProperties(
        mimeType = randomString(),
        size = randomLong(),
        available = randomBoolean(),
        width = randomInt(),
        height = randomInt()
    )
}

fun randomSimpleHashItem(
    nftId: String = randomAddressString(),
    tokenId: String? = randomBigInt().toString(),
    name: String? = randomString(),
    description: String? = randomString(),
    previews: SimpleHashItem.Preview? = randomSimpleHashItemPreview(),
    imageProperties: SimpleHashItem.ImageProperties? = randomSimpleHashImageProperties(),
    extraMetadata: SimpleHashItem.ExtraMetadata? = randomSimpleHashImageExtraMetadata(),
    collection: SimpleHashItem.Collection? = randomSimpleHashItemCollection(),
    createdDate: LocalDateTime? = LocalDateTime.now(),
    externalUrl: String? = "http://localhost:8080/sh/external/${randomString()}"
): SimpleHashItem {
    return SimpleHashItem(
        nftId = nftId,
        tokenId = tokenId,
        name = name,
        description = description,
        previews = previews,
        imageProperties = imageProperties,
        extraMetadata = extraMetadata,
        collection = collection,
        createdDate = createdDate,
        externalUrl = externalUrl
    )
}

fun randomSimpleHashItemPreview(
    imageSmallUrl: String? = "http://localhost:8080/sh/small/${randomString()}",
    imageMediumUrl: String? = "http://localhost:8080/sh/medium/${randomString()}",
    imageLargeUrl: String? = "http://localhost:8080/sh/large/${randomString()}",
    imageOpengraphUrl: String? = "http://localhost:8080/sh/opengraph/${randomString()}",
): SimpleHashItem.Preview {
    return SimpleHashItem.Preview(
        imageLargeUrl = imageLargeUrl,
        imageMediumUrl = imageMediumUrl,
        imageOpengraphUrl = imageOpengraphUrl,
        imageSmallUrl = imageSmallUrl
    )
}

fun randomSimpleHashImageProperties(
    width: Int? = randomInt(2000),
    height: Int? = randomInt(1000),
    size: Long? = randomLong(10000000),
    mimeType: String? = "image/png"
): SimpleHashItem.ImageProperties {
    return SimpleHashItem.ImageProperties(
        width = width,
        height = height,
        size = size,
        mimeType = mimeType
    )
}

fun randomSimpleHashImageExtraMetadata(
    imageOriginalUrl: String? = "http://localhost:8080/sh/original/${randomString()}",
    attributes: List<SimpleHashItem.Attribute> = emptyList(),
    features: Map<String, String>? = emptyMap(),
    projectId: String? = null,
    collectionName: String? = randomString(),
    metadataOriginalUrl: String? = "http://localhost:8080/sh/meta/${randomString()}",
): SimpleHashItem.ExtraMetadata {
    return SimpleHashItem.ExtraMetadata(
        imageOriginalUrl = imageOriginalUrl,
        attributes = attributes,
        features = features,
        projectId = projectId,
        collectionName = collectionName,
        metadataOriginalUrl = metadataOriginalUrl
    )
}

fun randomSimpleHashItemCollection(
    name: String? = "SH Collection ${randomString()}",
    description: String? = "SH description ${randomString()}",
    imageUrl: String? = "http://localhost:8080/sh/collection/original/${randomString()}",
    bannerImageUrl: String? = "http://localhost:8080/sh/collection/banner/${randomString()}",
): SimpleHashItem.Collection {
    return SimpleHashItem.Collection(
        name = name,
        description = description,
        imageUrl = imageUrl,
        bannerImageUrl = bannerImageUrl
    )
}

fun randomDownloadTask(
    id: String = randomEthItemId().fullId(),
    type: String = "item",
    pipeline: String = randomString(),
    force: Boolean = true,
    source: DownloadTaskSource = DownloadTaskSource.EXTERNAL,
    priority: Int = 0,
    scheduledAt: Instant = nowMillis(),
    startedAt: Instant? = null,
    inProgress: Boolean = false
): DownloadTask {
    return DownloadTask(
        id = id,
        type = type,
        pipeline = pipeline,
        force = force,
        source = source,
        priority = priority,
        scheduledAt = scheduledAt,
        startedAt = startedAt,
        inProgress = inProgress,
    )
}
