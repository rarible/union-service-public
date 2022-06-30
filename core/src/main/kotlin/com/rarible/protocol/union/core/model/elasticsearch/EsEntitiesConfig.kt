package com.rarible.protocol.union.core.model.elasticsearch

import com.rarible.core.logging.Logger
import com.rarible.protocol.union.core.model.EsActivity
import com.rarible.protocol.union.core.model.EsCollection
import com.rarible.protocol.union.core.model.EsItem
import com.rarible.protocol.union.core.model.EsOrder
import com.rarible.protocol.union.core.model.EsOwnership

object EsEntitiesConfig {

    /**
     * For test purposes only.
     */
    fun createEsEntities(): List<EntityDefinition> = listOf(
        EsActivity.ENTITY_DEFINITION,
        EsCollection.ENTITY_DEFINITION,
        EsOrder.ENTITY_DEFINITION,
        EsOwnership.ENTITY_DEFINITION,
        EsItem.ENTITY_DEFINITION
    )

    fun prodEsEntities(): List<EntityDefinition> = listOf(
        EsActivity.ENTITY_DEFINITION,
        EsCollection.ENTITY_DEFINITION,
        EsOwnership.ENTITY_DEFINITION,
    )

    fun loadMapping(entity: EsEntity): String {
        return loadMapping(entity.name.lowercase())
    }

    fun loadSettings(entity: EsEntity): String {
        return loadMapping("${entity.entityName.lowercase()}_settings")
    }

    fun loadMapping(mapping: String): String =
        try {
            EsEntitiesConfig::class.java.getResource("/mappings/$mapping.json")!!.readText()
        } catch (e: Exception) {
            logger.error("Not found mapping for index $mapping")
            throw e
        }

    val INDEX_SETTINGS = loadMapping("settings")

    private val logger by Logger()
}
