package com.rarible.protocol.union.enrichment.test.data

import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.OwnershipIdDto
import com.rarible.protocol.union.enrichment.converter.ShortItemConverter
import com.rarible.protocol.union.enrichment.converter.ShortOwnershipConverter
import com.rarible.protocol.union.test.data.randomEthItemId
import com.rarible.protocol.union.test.data.randomUnionItem
import com.rarible.protocol.union.test.data.randomUnionOwnershipDto

fun randomShortItem() = ShortItemConverter.convert(randomUnionItem(randomEthItemId()))
fun randomShortItem(id: ItemIdDto) = ShortItemConverter.convert(randomUnionItem(id))

fun randomShortOwnership() = ShortOwnershipConverter.convert(randomUnionOwnershipDto())
fun randomShortOwnership(id: ItemIdDto) = ShortOwnershipConverter.convert(randomUnionOwnershipDto(id))
fun randomShortOwnership(id: OwnershipIdDto) = ShortOwnershipConverter.convert(randomUnionOwnershipDto(id))