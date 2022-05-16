package com.rarible.protocol.union.core.util

import com.rarible.core.logging.RaribleMDCContext
import com.rarible.protocol.union.core.model.UnionItem
import com.rarible.protocol.union.core.service.ItemService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ItemIdDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.withContext
import org.slf4j.MDC

object LogUtils {

    @ExperimentalCoroutinesApi
    suspend fun <T> addToMdc(vararg values: Pair<String, String>, block: suspend CoroutineScope.() -> T): T {
        return addToMdc(values.toList(), block)
    }

    @ExperimentalCoroutinesApi
    suspend fun <T> addToMdc(values: List<Pair<String, String>>, block: suspend CoroutineScope.() -> T): T {
        val current = MDC.getCopyOfContextMap()
        val newValues = values.associateBy({ it.first }, { it.second })

        val resultMap = current?.let { newValues + it } ?: newValues

        return withContext(RaribleMDCContext(resultMap), block)
    }

    @ExperimentalCoroutinesApi
    suspend fun <T> addToMdc(
        itemId: ItemIdDto,
        router: BlockchainRouter<ItemService>,
        block: suspend CoroutineScope.() -> T
    ): T {
        val collection = if (itemId.blockchain == BlockchainDto.SOLANA) {
            getSolanaCollection(itemId, router)
        } else {
            getCollection(itemId)
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

    private suspend fun getSolanaCollection(
        itemId: ItemIdDto,
        router: BlockchainRouter<ItemService>
    ): String =
        try {
            router
                .getService(itemId.blockchain)
                .getItemById(itemId.toString())
                .collection.toString()
        } catch (e: NoSuchElementException) {
            ""
        }

    private fun getCollection(itemId: ItemIdDto): String {
        val pair = itemId.value.split(":")
        return if (pair.size > 1) pair[0] else ""
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
}
