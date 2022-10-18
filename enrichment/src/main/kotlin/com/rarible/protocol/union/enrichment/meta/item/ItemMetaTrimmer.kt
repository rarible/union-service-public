package com.rarible.protocol.union.enrichment.meta.item

import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.core.util.trimToLength
import com.rarible.protocol.union.dto.MetaAttributeDto
import com.rarible.protocol.union.enrichment.configuration.ItemMetaTrimmingProperties
import org.springframework.stereotype.Component

// Introduced because of items like SOLANA:A7jz7XRxEruiWfJGw2rMNTXi1huNuhAoHAswgzx1dXWV
@Component
class ItemMetaTrimmer(
    private val properties: ItemMetaTrimmingProperties
) {

    private val suffix = properties.suffix

    fun trim(meta: UnionMeta?): UnionMeta? {
        meta ?: return null
        return meta.copy(
            name = trimToLength(meta.name, properties.nameLength, suffix)!!,
            description = trimToLength(meta.description, properties.descriptionLength, suffix),
            attributes = meta.attributes.take(properties.attributesSize).map { trim(it) }
        )
    }

    private fun trim(attribute: MetaAttributeDto): MetaAttributeDto {
        return attribute.copy(
            key = trimToLength(attribute.key, properties.attributeNameLength, suffix)!!,
            value = trimToLength(attribute.value, properties.attributeValueLength, suffix)
        )
    }

}