package com.rarible.protocol.union.enrichment.repository

import com.rarible.protocol.union.core.model.UnionMetaAttribute
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.enrichment.model.ItemMetaCustomAttributes
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.find
import org.springframework.data.mongodb.core.findById
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.inValues
import org.springframework.stereotype.Component

@Component
class ItemMetaCustomAttributesRepository(
    private val template: ReactiveMongoTemplate
) {

    suspend fun getCustomAttributes(id: ItemIdDto): List<UnionMetaAttribute> {
        return get(id)?.attributes ?: emptyList()
    }

    suspend fun save(attributes: ItemMetaCustomAttributes): ItemMetaCustomAttributes {
        return template.save(attributes).awaitFirst()
    }

    suspend fun get(itemId: ItemIdDto): ItemMetaCustomAttributes? {
        return template.findById<ItemMetaCustomAttributes>(itemId.fullId()).awaitFirstOrNull()
    }

    suspend fun getAll(ids: Collection<ItemIdDto>): List<ItemMetaCustomAttributes> {
        val criteria = Criteria("_id").inValues(ids.map { it.fullId() })
        return template.find<ItemMetaCustomAttributes>(Query(criteria)).collectList().awaitFirst()
    }
}
