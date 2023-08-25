package com.rarible.protocol.union.enrichment.test.data

import com.rarible.core.test.data.randomBigInt
import com.rarible.core.test.data.randomString
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.OrderIdDto
import com.rarible.protocol.union.dto.OwnershipIdDto
import com.rarible.protocol.union.enrichment.converter.EnrichmentActivityConverter
import com.rarible.protocol.union.enrichment.converter.EnrichmentCollectionConverter
import com.rarible.protocol.union.enrichment.converter.ShortItemConverter
import com.rarible.protocol.union.enrichment.converter.ShortOrderConverter
import com.rarible.protocol.union.enrichment.converter.ShortOwnershipConverter
import com.rarible.protocol.union.enrichment.model.ShortOrder
import com.rarible.protocol.union.integration.ethereum.data.randomEthCollectionId
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import java.math.BigInteger

fun randomShortItem() = ShortItemConverter.convert(randomUnionItem(randomEthItemId()))
fun randomShortItem(id: ItemIdDto) = ShortItemConverter.convert(randomUnionItem(id))

fun randomEnrichmentCollection(id: CollectionIdDto = randomEthCollectionId()) = EnrichmentCollectionConverter.convert(
    collection = randomUnionCollection(id)
)

fun randomEnrichmentMintActivity(itemId: ItemIdDto = randomEthItemId()) = EnrichmentActivityConverter.convert(
    source = randomUnionActivityMint(itemId)
)

fun randomShortOwnership() = ShortOwnershipConverter.convert(randomUnionOwnership())
fun randomShortOwnership(id: ItemIdDto) = ShortOwnershipConverter.convert(randomUnionOwnership(id))
fun randomShortOwnership(id: OwnershipIdDto) = ShortOwnershipConverter.convert(randomUnionOwnership(id))

fun randomShortSellOrder() = ShortOrderConverter.convert(randomUnionSellOrder())
fun randomShortBidOrder() = ShortOrderConverter.convert(randomUnionBidOrder())

fun randomShortOrder(
    id: String = randomString(),
    makeStock: BigInteger = randomBigInt(),
): ShortOrder = ShortOrder(
    blockchain = BlockchainDto.ETHEREUM,
    id = id,
    platform = "RARIBLE",
    makeStock = makeStock,
    dtoId = OrderIdDto(BlockchainDto.ETHEREUM, id),
    makePrice = null,
    takePrice = null
)
