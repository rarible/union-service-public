package com.rarible.protocol.union.core.util

import com.rarible.core.logging.RaribleMDCContext
import com.rarible.protocol.union.core.model.UnionItem
import com.rarible.protocol.union.core.service.ItemService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.CollectionDto
import com.rarible.protocol.union.dto.CollectionEventDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.CollectionMetaDto
import com.rarible.protocol.union.dto.CollectionUpdateEventDto
import com.rarible.protocol.union.dto.ItemDeleteEventDto
import com.rarible.protocol.union.dto.ItemDto
import com.rarible.protocol.union.dto.ItemEventDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.ItemUpdateEventDto
import com.rarible.protocol.union.dto.MetaAttributeDto
import com.rarible.protocol.union.dto.MetaDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.slf4j.MDC

object LogUtils {

    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun <T> addToMdc(vararg values: Pair<String, String>, block: suspend CoroutineScope.() -> T): T {
        return addToMdc(values.toList(), block)
    }

    suspend fun <T> addToMdc(values: List<Pair<String, String>>, block: suspend CoroutineScope.() -> T): T {
        val current = MDC.getCopyOfContextMap()
        val newValues = values.associateBy({ it.first }, { it.second })

        val resultMap = current?.let { newValues + it } ?: newValues

        return withContext(RaribleMDCContext(resultMap), block)
    }

    suspend fun <T> addToMdc(
        itemId: ItemIdDto,
        router: BlockchainRouter<ItemService>,
        block: suspend CoroutineScope.() -> T
    ): T {
        return addToMdc(itemId, collectionId = null, router, block)
    }

    suspend fun <T> addToMdc(
        itemId: ItemIdDto,
        collectionId: CollectionIdDto?,
        router: BlockchainRouter<ItemService>,
        block: suspend CoroutineScope.() -> T
    ): T {
        val collection = collectionId?.fullId() ?: try {
            router.getService(itemId.blockchain).getItemCollectionId(itemId.value) ?: ""
        } catch (e: Exception) {
            logger.info("Unable to get collection for Item {}: {}", itemId, e.message)
            // should never happen
            ""
        }

        return addToMdc(
            values = listOf(
                "blockchain" to itemId.blockchain.name,
                "itemId" to itemId.value,
                "collection" to collection
            ),
            block = block
        )
    }

    suspend fun <T> addToMdc(
        collectionId: CollectionIdDto,
        block: suspend CoroutineScope.() -> T
    ): T {
        return addToMdc(
            values = listOf(
                "blockchain" to collectionId.blockchain.name,
                "collection" to collectionId.value
            ),
            block = block
        )
    }

    @ExperimentalCoroutinesApi
    suspend fun <T> addToMdc(item: UnionItem, block: suspend CoroutineScope.() -> T): T {
        return addToMdc(
            listOf(
                "blockchain" to item.id.blockchain.name,
                "itemId" to item.id.value,
                "collection" to (item.collection?.value ?: "")
            ), block
        )
    }

    fun ItemEventDto.log(): String {
        return when (this) {
            is ItemUpdateEventDto -> copy(item = item.trim())
            is ItemDeleteEventDto -> this
        }.toString()
    }

    fun CollectionEventDto.log(): String {
        return when (this) {
            is CollectionUpdateEventDto -> copy(collection = collection.trim())
        }.toString()
    }

    private fun CollectionDto.trim(): CollectionDto {
        return this.copy(meta = meta?.trim())
    }

    private fun ItemDto.trim(): ItemDto {
        return this.copy(meta = meta?.trim())
    }

    private fun MetaDto.trim(): MetaDto {
        return this.copy(
            name = trimToLength(name, 1000, "...")!!,
            description = trimToLength(description, 1000, "..."),
            attributes = attributes.take(100).map { it.trim() }
        )
    }

    private fun CollectionMetaDto.trim(): CollectionMetaDto {
        return this.copy(
            name = trimToLength(name, 1000, "...")!!,
            description = trimToLength(description, 1000, "...")
        )
    }

    private fun MetaAttributeDto.trim(): MetaAttributeDto {
        return this.copy(
            key = trimToLength(key, 100, "...")!!,
            value = trimToLength(value, 100)
        )
    }
}
