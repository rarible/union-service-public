package com.rarible.protocol.union.enrichment.converter

import com.rarible.protocol.union.core.model.UnionTraitEvent
import com.rarible.protocol.union.dto.ExtendedTraitPropertyDto
import com.rarible.protocol.union.dto.TraitDto
import com.rarible.protocol.union.dto.TraitEntryDto
import com.rarible.protocol.union.dto.TraitsDto
import com.rarible.protocol.union.enrichment.model.Trait
import java.math.RoundingMode
import com.rarible.protocol.union.core.model.trait.Trait as TraitEntity

object TraitConverter {

    fun toEvent(trait: Trait): UnionTraitEvent {
        return UnionTraitEvent(
            id = trait.id,
            collectionId = trait.collectionId.toDto(),
            key = trait.key,
            value = trait.value,
            itemsCount = trait.itemsCount,
            listedItemsCount = trait.listedItemsCount,
            version = trait.version
        )
    }

    fun convert(traits: List<TraitEntity>): TraitsDto {
        return TraitsDto(
            traits = traits.map { trait ->
                TraitDto(
                    key = TraitEntryDto(
                        value = trait.key.value,
                        count = trait.key.count
                    ),
                    values = trait.values.map { value ->
                        TraitEntryDto(
                            value = value.value,
                            count = value.count
                        )
                    }
                )
            }
        )
    }

    fun convertWithRarity(traits: List<TraitEntity>, itemCount: Long): List<ExtendedTraitPropertyDto> {
        return traits.map { trait ->
            trait.values.map { value ->
                ExtendedTraitPropertyDto(
                    key = trait.key.value,
                    value = value.value,
                    rarity = value.count.toBigDecimal().multiply(ONE_HUNDRED_PERCENT).divide(itemCount.toBigDecimal(), 7, RoundingMode.HALF_UP)
                )
            }
        }.flatten()
    }

    private val ONE_HUNDRED_PERCENT = 100.toBigDecimal()
}
