package com.rarible.protocol.union.enrichment.meta.item

import com.rarible.protocol.union.core.model.UnionCollectionMeta
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.core.model.UnionMetaAttribute
import com.rarible.protocol.union.core.util.trimToLength
import com.rarible.protocol.union.enrichment.configuration.MetaTrimmingProperties
import org.springframework.stereotype.Component

// Introduced because of items like SOLANA:A7jz7XRxEruiWfJGw2rMNTXi1huNuhAoHAswgzx1dXWV
@Component
class MetaTrimmer(
    private val properties: MetaTrimmingProperties
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

    fun trim(meta: UnionCollectionMeta?): UnionCollectionMeta? {
        meta ?: return null
        return meta.copy(
            name = trimToLength(meta.name, properties.nameLength, suffix)!!,
            description = trimToLength(meta.description, properties.descriptionLength, suffix),
        )
    }

    private fun trim(attribute: UnionMetaAttribute): UnionMetaAttribute {
        return attribute.copy(
            key = trimToLength(attribute.key, properties.attributeNameLength, suffix)!!,
            value = trimToLength(attribute.value, properties.attributeValueLength, suffix)
        )
    }
}
