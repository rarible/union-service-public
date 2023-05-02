package com.rarible.protocol.union.enrichment.test.data

import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.OwnershipIdDto
import com.rarible.protocol.union.enrichment.converter.EnrichmentActivityConverter
import com.rarible.protocol.union.enrichment.converter.EnrichmentCollectionConverter
import com.rarible.protocol.union.enrichment.converter.ShortItemConverter
import com.rarible.protocol.union.enrichment.converter.ShortOwnershipConverter
import com.rarible.protocol.union.integration.ethereum.data.randomEthCollectionId
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId

fun randomShortItem() = ShortItemConverter.convert(randomUnionItem(randomEthItemId()))
fun randomShortItem(id: ItemIdDto) = ShortItemConverter.convert(randomUnionItem(id))

fun randomEnrichmentCollection(id: CollectionIdDto = randomEthCollectionId()) = EnrichmentCollectionConverter.convert(
    collection = randomUnionCollection(id)
)

fun randomEnrichmentMintActivity() = EnrichmentActivityConverter.convert(
    source = randomUnionActivityMint(randomEthItemId())
)

fun randomShortOwnership() = ShortOwnershipConverter.convert(randomUnionOwnership())
fun randomShortOwnership(id: ItemIdDto) = ShortOwnershipConverter.convert(randomUnionOwnership(id))
fun randomShortOwnership(id: OwnershipIdDto) = ShortOwnershipConverter.convert(randomUnionOwnership(id))
