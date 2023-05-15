package com.rarible.protocol.union.enrichment.meta.item.customizer

import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.enrichment.meta.item.ItemMetaCustomizer
import com.rarible.protocol.union.enrichment.repository.ItemMetaCustomAttributesRepository
import org.springframework.stereotype.Component

@Component
class ItemMetaAttributeCustomizer(
    private val itemMetaCustomAttributeRepository: ItemMetaCustomAttributesRepository
) : ItemMetaCustomizer {

    override suspend fun customize(id: ItemIdDto, meta: UnionMeta): UnionMeta {
        val customAttributes = itemMetaCustomAttributeRepository.getCustomAttributes(id)
        if (customAttributes.isEmpty()) {
            return meta
        }

        val attributes = meta.attributes.associateByTo(LinkedHashMap()) { it.key }
        // Add or replace existing attribute
        customAttributes.forEach { attributes[it.key] = it }
        return meta.copy(attributes = attributes.values.toList())
    }
}