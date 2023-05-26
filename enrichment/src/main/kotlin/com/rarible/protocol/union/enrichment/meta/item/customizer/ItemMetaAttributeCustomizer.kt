package com.rarible.protocol.union.enrichment.meta.item.customizer

import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.enrichment.meta.WrappedMeta
import com.rarible.protocol.union.enrichment.meta.item.ItemMetaCustomizer
import com.rarible.protocol.union.enrichment.repository.ItemMetaCustomAttributesRepository
import org.slf4j.LoggerFactory
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

@Component
@Order(10)
class ItemMetaAttributeCustomizer(
    private val itemMetaCustomAttributeRepository: ItemMetaCustomAttributesRepository
) : ItemMetaCustomizer {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun customize(id: ItemIdDto, wrappedMeta: WrappedMeta<UnionMeta>): WrappedMeta<UnionMeta> {
        val customAttributes = itemMetaCustomAttributeRepository.getCustomAttributes(id)
        if (customAttributes.isEmpty()) {
            return wrappedMeta
        }

        logger.info(
            "Customizing meta for Item {} with {} (attributes={})",
            id.fullId(),
            javaClass.simpleName,
            customAttributes
        )

        val attributes = wrappedMeta.data.attributes.associateByTo(LinkedHashMap()) { it.key }
        // Add or replace existing attribute
        customAttributes.forEach { attributes[it.key] = it }
        return wrappedMeta.copy(
            data = wrappedMeta.data.copy(attributes = attributes.values.toList())
        )
    }
}
