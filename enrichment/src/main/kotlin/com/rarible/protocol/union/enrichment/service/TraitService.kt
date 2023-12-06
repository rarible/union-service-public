package com.rarible.protocol.union.enrichment.service

import com.rarible.protocol.union.enrichment.repository.TraitRepository
import kotlinx.coroutines.flow.count
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.atomic.AtomicInteger
import kotlin.system.measureTimeMillis

@Service
class TraitService(
    val traitRepository: TraitRepository
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun deleteWithZeroItemsCount() {
        logger.info("Deleting traits with zero items count")
        val deleted = AtomicInteger(0)
        val time = measureTimeMillis {
            deleted.set(traitRepository.deleteWithZeroItemsCount().count())
            // TODO delete from index as well after implementing PT-4121
        }
        logger.info("Deleted traits with zero items count: ${deleted.get()} time: ${time}ms")
    }
}
