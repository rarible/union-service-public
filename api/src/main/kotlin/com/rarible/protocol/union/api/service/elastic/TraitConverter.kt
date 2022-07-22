package com.rarible.protocol.union.api.service.elastic

import com.rarible.protocol.union.core.model.trait.ExtendedTraitProperty
import com.rarible.protocol.union.core.model.trait.Trait
import com.rarible.protocol.union.core.model.trait.TraitEntry
import com.rarible.protocol.union.core.model.trait.TraitProperty
import com.rarible.protocol.union.dto.ExtendedTraitPropertyDto
import com.rarible.protocol.union.dto.TraitDto
import com.rarible.protocol.union.dto.TraitEntryDto
import com.rarible.protocol.union.dto.TraitPropertyDto

fun Trait.toApiDto(): TraitDto =
    TraitDto(
        key = key.toApiDto(),
        values = values.map { it.toApiDto() }
    )

private fun TraitEntry.toApiDto(): TraitEntryDto = TraitEntryDto(count = count, value = value)

fun ExtendedTraitProperty.toApiDto(): ExtendedTraitPropertyDto =
    ExtendedTraitPropertyDto(key = key, value = value, rarity = rarity)

fun TraitPropertyDto.toInner(): TraitProperty = TraitProperty(key = key, value = value)
