import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomBigDecimal
import com.rarible.core.test.data.randomBigInt
import com.rarible.core.test.data.randomBinary
import com.rarible.core.test.data.randomBoolean
import com.rarible.core.test.data.randomInt
import com.rarible.core.test.data.randomLong
import com.rarible.core.test.data.randomString
import com.rarible.protocol.union.core.model.UnionAsset
import com.rarible.protocol.union.core.model.UnionAssetType
import com.rarible.protocol.union.core.model.UnionEthErc20AssetType
import com.rarible.protocol.union.core.model.UnionEthErc721AssetType
import com.rarible.protocol.union.core.model.UnionOrder
import com.rarible.protocol.union.core.model.UnionOwnership
import com.rarible.protocol.union.core.model.UnionPendingOrder
import com.rarible.protocol.union.core.model.elastic.EsOwnership
import com.rarible.protocol.union.dto.AssetDto
import com.rarible.protocol.union.dto.AssetTypeDto
import com.rarible.protocol.union.dto.AuctionBidDto
import com.rarible.protocol.union.dto.AuctionDataDto
import com.rarible.protocol.union.dto.AuctionDto
import com.rarible.protocol.union.dto.AuctionHistoryDto
import com.rarible.protocol.union.dto.AuctionIdDto
import com.rarible.protocol.union.dto.AuctionStatusDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.ContractAddress
import com.rarible.protocol.union.dto.CreatorDto
import com.rarible.protocol.union.dto.EthErc20AssetTypeDto
import com.rarible.protocol.union.dto.EthErc721AssetTypeDto
import com.rarible.protocol.union.dto.EthLazyItemErc1155Dto
import com.rarible.protocol.union.dto.EthLazyItemErc721Dto
import com.rarible.protocol.union.dto.EthOrderDataRaribleV2DataV1Dto
import com.rarible.protocol.union.dto.EthOrderDataRaribleV2DataV2Dto
import com.rarible.protocol.union.dto.EthOrderFormAssetDto
import com.rarible.protocol.union.dto.EthRaribleV2OrderFormDto
import com.rarible.protocol.union.dto.ItemHistoryDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.OrderDataDto
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.OrderIdDto
import com.rarible.protocol.union.dto.OrderStatusDto
import com.rarible.protocol.union.dto.OwnershipDto
import com.rarible.protocol.union.dto.OwnershipIdDto
import com.rarible.protocol.union.dto.PayoutDto
import com.rarible.protocol.union.dto.PendingOrderDto
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.dto.RaribleAuctionV1DataV1Dto
import com.rarible.protocol.union.dto.RoyaltyDto
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.dto.group
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Instant
import java.time.temporal.ChronoUnit

fun randomContractAddress(
    blockchain: BlockchainDto = BlockchainDto.ETHEREUM,
    value: String = randomAddress().hex()
) = ContractAddress(blockchain, value)

fun randomOwnershipId(
    blockchain: BlockchainDto = BlockchainDto.values().random(),
    itemIdValue: String = "${randomString().lowercase()}:${randomLong()}",
    owner: UnionAddress = randomUnionAddress(blockchain, randomString().lowercase()),
) = OwnershipIdDto(
    blockchain = blockchain,
    itemIdValue = itemIdValue,
    owner = owner,
)

fun randomOwnership(
    id: OwnershipIdDto = randomOwnershipId(),
    blockchain: BlockchainDto = id.blockchain,
    itemId: ItemIdDto? = ItemIdDto(id.blockchain, id.itemIdValue),
    contract: ContractAddress? = ContractAddress(id.blockchain, randomString()),
    collection: CollectionIdDto? = CollectionIdDto(id.blockchain, randomString()),
    tokenId: BigInteger? = randomBigInt(),
    owner: UnionAddress = UnionAddress(id.blockchain.group(), randomString()),
    value: BigInteger = randomBigInt(),
    createdAt: Instant = Instant.ofEpochMilli(randomLong()),
    creators: List<CreatorDto>? = listOf(CreatorDto(randomUnionAddress(id.blockchain), randomInt())),
    lazyValue: BigInteger = randomBigInt(),
    pending: List<ItemHistoryDto> = listOf(),
    auction: AuctionDto? = randomAuction(id = randomAuctionId(id.blockchain)),
    bestSellOrder: OrderDto? = randomOrderDto(id = randomOrderId(id.blockchain)),
) = OwnershipDto(
    id = id,
    blockchain = blockchain,
    itemId = itemId,
    contract = contract,
    collection = collection,
    tokenId = tokenId,
    owner = owner,
    value = value,
    createdAt = createdAt,
    creators = creators,
    lazyValue = lazyValue,
    pending = pending,
    auction = auction,
    bestSellOrder = bestSellOrder,
)

fun randomUnionAddress(
    blockchain: BlockchainDto = BlockchainDto.values().random(),
    value: String = randomAddress().prefixed(),
) = UnionAddress(blockchain.group(), value)

fun randomAuctionId(
    blockchain: BlockchainDto = BlockchainDto.values().random(),
    value: String = randomString(),
) = AuctionIdDto(blockchain, value)

fun randomOrderId(
    blockchain: BlockchainDto = BlockchainDto.values().random(),
    value: String = randomString(),
) = OrderIdDto(blockchain, value)

fun randomItemId(
    blockchain: BlockchainDto = BlockchainDto.values().random(),
    value: String = "${randomString()}:${randomBigInt(6)}",
) = ItemIdDto(blockchain, value)

fun randomInstant(): Instant = nowMillis().minusMillis(randomLong(14400000)).truncatedTo(ChronoUnit.MILLIS)

fun randomAuction(
    id: AuctionIdDto = randomAuctionId(),
    contract: ContractAddress = ContractAddress(id.blockchain, randomString()),
    type: AuctionDto.Type? = AuctionDto.Type.values().random(),
    seller: UnionAddress = randomUnionAddress(id.blockchain),
    sell: AssetDto = AssetDto(randomAssetTypeDto(id.blockchain), randomBigDecimal()),
    buy: AssetTypeDto = randomAssetTypeDto(id.blockchain),
    endTime: Instant? = randomInstant(),
    minimalStep: BigDecimal = randomBigDecimal(),
    minimalPrice: BigDecimal = randomBigDecimal(),
    createdAt: Instant = randomInstant(),
    lastUpdateAt: Instant = randomInstant(),
    buyPrice: BigDecimal? = randomBigDecimal(),
    buyPriceUsd: BigDecimal? = randomBigDecimal(),
    pending: List<AuctionHistoryDto>? = null,
    status: AuctionStatusDto = AuctionStatusDto.values().random(),
    ongoing: Boolean = randomBoolean(),
    hash: String = randomString(),
    auctionId: BigInteger = randomBigInt(),
    lastBid: AuctionBidDto? = null,
    data: AuctionDataDto = RaribleAuctionV1DataV1Dto(emptyList(), emptyList(), duration = randomBigInt()),
) = AuctionDto(
    id = id,
    contract = contract,
    type = type,
    seller = seller,
    sell = sell,
    buy = buy,
    endTime = endTime,
    minimalStep = minimalStep,
    minimalPrice = minimalPrice,
    createdAt = createdAt,
    lastUpdateAt = lastUpdateAt,
    buyPrice = buyPrice,
    buyPriceUsd = buyPriceUsd,
    pending = pending,
    status = status,
    ongoing = ongoing,
    hash = hash,
    auctionId = auctionId,
    lastBid = lastBid,
    data = data,
)

fun randomAssetTypeDto(blockchain: BlockchainDto): AssetTypeDto = EthErc721AssetTypeDto(
    contract = ContractAddress(blockchain, randomAddress().prefixed()),
    tokenId = randomBigInt(),
)

fun randomAssetTypeErc20Dto(blockchain: BlockchainDto): EthErc20AssetTypeDto = EthErc20AssetTypeDto(
    contract = ContractAddress(blockchain, randomString()),
)

fun randomUnionAssetType(blockchain: BlockchainDto): UnionAssetType = UnionEthErc721AssetType(
    contract = ContractAddress(blockchain, randomString()),
    tokenId = randomBigInt(),
)

fun randomUnionAssetTypeErc20(blockchain: BlockchainDto): UnionAssetType = UnionEthErc20AssetType(
    contract = ContractAddress(blockchain, randomString()),
)

fun randomUnionOrder(
    id: OrderIdDto = randomOrderId(),
    fill: BigDecimal = randomBigDecimal(),
    platform: PlatformDto = PlatformDto.values().random(),
    status: UnionOrder.Status = UnionOrder.Status.values().random(),
    startedAt: Instant? = null,
    endedAt: Instant? = null,
    makeStock: BigDecimal = randomBigDecimal(),
    cancelled: Boolean = randomBoolean(),
    createdAt: Instant = randomInstant(),
    lastUpdatedAt: Instant = randomInstant(),
    makePrice: BigDecimal? = randomBigDecimal(),
    takePrice: BigDecimal? = randomBigDecimal(),
    makePriceUsd: BigDecimal? = null,
    takePriceUsd: BigDecimal? = null,
    maker: UnionAddress = randomUnionAddress(id.blockchain),
    taker: UnionAddress? = null,
    make: UnionAsset = UnionAsset(randomUnionAssetType(id.blockchain), randomBigDecimal()),
    take: UnionAsset = UnionAsset(randomUnionAssetType(id.blockchain), randomBigDecimal()),
    salt: String = randomString(),
    signature: String? = null,
    pending: List<UnionPendingOrder>? = listOf(),
    data: OrderDataDto = randomOrderData(),
) = UnionOrder(
    id = id,
    fill = fill,
    platform = platform,
    status = status,
    startedAt = startedAt,
    endedAt = endedAt,
    makeStock = makeStock,
    cancelled = cancelled,
    createdAt = createdAt,
    lastUpdatedAt = lastUpdatedAt,
    makePrice = makePrice,
    takePrice = takePrice,
    makePriceUsd = makePriceUsd,
    takePriceUsd = takePriceUsd,
    maker = maker,
    taker = taker,
    make = make,
    take = take,
    salt = salt,
    signature = signature,
    pending = pending,
    data = data,
)

fun randomOrderDto(
    id: OrderIdDto = randomOrderId(),
    fill: BigDecimal = randomBigDecimal(),
    platform: PlatformDto = PlatformDto.values().random(),
    status: OrderStatusDto = OrderStatusDto.values().random(),
    startedAt: Instant? = null,
    endedAt: Instant? = null,
    makeStock: BigDecimal = randomBigDecimal(),
    cancelled: Boolean = randomBoolean(),
    createdAt: Instant = randomInstant(),
    lastUpdatedAt: Instant = randomInstant(),
    makePrice: BigDecimal? = randomBigDecimal(),
    takePrice: BigDecimal? = randomBigDecimal(),
    makePriceUsd: BigDecimal? = null,
    takePriceUsd: BigDecimal? = null,
    maker: UnionAddress = randomUnionAddress(id.blockchain),
    taker: UnionAddress? = null,
    make: AssetDto = AssetDto(randomAssetTypeDto(id.blockchain), randomBigDecimal()),
    take: AssetDto = AssetDto(randomAssetTypeDto(id.blockchain), randomBigDecimal()),
    salt: String = randomString(),
    signature: String? = null,
    pending: List<PendingOrderDto>? = listOf(),
    data: OrderDataDto = randomOrderData(),
) = OrderDto(
    id = id,
    fill = fill,
    platform = platform,
    status = status,
    startedAt = startedAt,
    endedAt = endedAt,
    makeStock = makeStock,
    cancelled = cancelled,
    createdAt = createdAt,
    lastUpdatedAt = lastUpdatedAt,
    makePrice = makePrice,
    takePrice = takePrice,
    makePriceUsd = makePriceUsd,
    takePriceUsd = takePriceUsd,
    maker = maker,
    taker = taker,
    make = make,
    take = take,
    salt = salt,
    signature = signature,
    pending = pending,
    data = data,
)

fun randomOrderData(): OrderDataDto = EthOrderDataRaribleV2DataV1Dto(
    listOf(PayoutDto(randomUnionAddress(), randomInt())),
    listOf(PayoutDto(randomUnionAddress(), randomInt()))
)

fun randomEsOwnership(
    id: OwnershipIdDto = randomOwnershipId(),
) = EsOwnership(
    ownershipId = id.fullId(),
    originalOwnershipId = id.fullId(),
    blockchain = id.blockchain,
    itemId = ItemIdDto(id.blockchain, id.itemIdValue).fullId(),
    collection = CollectionIdDto(id.blockchain, randomString()).fullId(),
    owner = id.owner.fullId(),
    date = randomInstant(),
    auctionId = randomString(),
    auctionOwnershipId = OwnershipIdDto(
        id.blockchain,
        id.itemIdValue,
        randomUnionAddress(id.blockchain, randomString())
    ).fullId(),
)

fun convertUnionOwnershipToEsOwnership(source: UnionOwnership): EsOwnership {
    return EsOwnership(
        ownershipId = source.id.fullId(),
        originalOwnershipId = null,
        blockchain = source.id.blockchain,
        itemId = source.id.getItemId().fullId(),
        collection = source.collection?.fullId(),
        owner = source.id.owner.fullId(),
        date = source.createdAt,
        auctionId = null,
        auctionOwnershipId = null,
    )
}

fun randomEthRaribleV2OrderFormDto(
    blockchain: BlockchainDto = BlockchainDto.ETHEREUM,
    maker: UnionAddress = randomUnionAddress(BlockchainDto.ETHEREUM),
    taker: UnionAddress = randomUnionAddress(BlockchainDto.ETHEREUM),
    make: EthOrderFormAssetDto = EthOrderFormAssetDto(randomAssetTypeDto(BlockchainDto.ETHEREUM), randomBigInt()),
    take: EthOrderFormAssetDto = EthOrderFormAssetDto(randomAssetTypeDto(BlockchainDto.ETHEREUM), randomBigInt()),
    startedAt: Instant? = nowMillis(),
    endedAt: Instant = nowMillis(),
    salt: BigInteger = randomBigInt(),
    signature: String = randomBinary().prefixed(),
) = EthRaribleV2OrderFormDto(
    blockchain = blockchain,
    maker = maker,
    taker = taker,
    make = make,
    take = take,
    startedAt = startedAt,
    endedAt = endedAt,
    salt = salt,
    signature = signature,
    data = EthOrderDataRaribleV2DataV2Dto(emptyList(), emptyList(), false),
)

fun randomEthLazyItemErc721Dto(
    itemId: ItemIdDto = randomItemId(),
    uri: String = randomString(),
    creators: List<CreatorDto> = emptyList(),
    royalties: List<RoyaltyDto> = emptyList(),
    signatures: List<String> = emptyList()
) = EthLazyItemErc721Dto(
    id = itemId,
    uri = uri,
    creators = creators,
    royalties = royalties,
    signatures = signatures
)

fun randomEthLazyItemErc1155Dto(
    itemId: ItemIdDto = randomItemId(),
    uri: String = randomString(),
    creators: List<CreatorDto> = emptyList(),
    royalties: List<RoyaltyDto> = emptyList(),
    signatures: List<String> = emptyList(),
    supply: BigInteger = randomBigInt()
) = EthLazyItemErc1155Dto(
    id = itemId,
    uri = uri,
    creators = creators,
    royalties = royalties,
    signatures = signatures,
    supply = supply,
)
