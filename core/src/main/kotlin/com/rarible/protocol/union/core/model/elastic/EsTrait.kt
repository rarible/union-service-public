package com.rarible.protocol.union.core.model.elastic

import com.rarible.protocol.union.core.model.elastic.EsEntitiesConfig.loadMapping
import com.rarible.protocol.union.core.model.elastic.EsEntitiesConfig.loadSettings
import com.rarible.protocol.union.dto.BlockchainDto
import org.springframework.data.annotation.Id

data class EsTrait(
    @Id
    val id: String,
    val blockchain: BlockchainDto,
    val collectionId: String,
    val key: String,
    val value: String?, // TODO: Should it be nullable?
    val itemsCount: Long,
    val listedItemsCount: Long,
    val version: Long,
) {
    companion object {
        const val VERSION: Int = 1

        val ENTITY_DEFINITION = EsEntity.TRAIT.let {
            EntityDefinition(
                entity = it,
                mapping = loadMapping(it),
                versionData = VERSION,
                settings = loadSettings(it)
            )
        }
    }
}
