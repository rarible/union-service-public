package com.rarible.protocol.union.enrichment.service

import com.rarible.protocol.union.enrichment.repository.TraitRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class TraitService(
    val traitRepository: TraitRepository
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun deleteWithZeroItemsCount() {
        logger.info("Deleting traits with zero items count")
        traitRepository.findWithZeroItemsCount().collect {
            //TODO delete from index as well after implementing PT-4121
            traitRepository.delete(it)
        }
    }

}
