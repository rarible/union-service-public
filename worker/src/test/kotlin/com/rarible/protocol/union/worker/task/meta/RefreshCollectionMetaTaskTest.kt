package com.rarible.protocol.union.worker.task.meta

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.rarible.protocol.union.core.task.RefreshCollectionMetaTaskParam
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.enrichment.download.DownloadTaskSource
import com.rarible.protocol.union.enrichment.meta.collection.CollectionMetaPipeline
import com.rarible.protocol.union.enrichment.meta.collection.CollectionMetaService
import com.rarible.protocol.union.enrichment.model.EnrichmentCollectionId
import com.rarible.protocol.union.enrichment.repository.CollectionRepository
import com.rarible.protocol.union.enrichment.test.data.randomCollectionMetaDownloadEntry
import com.rarible.protocol.union.enrichment.test.data.randomEnrichmentCollection
import com.rarible.protocol.union.integration.ethereum.data.randomEthCollectionId
import io.mockk.coEvery
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.SpyK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
internal class RefreshCollectionMetaTaskTest {
    @InjectMockKs
    private lateinit var refreshCollectionMetaTask: RefreshCollectionMetaTask

    @MockK
    private lateinit var collectionRepository: CollectionRepository

    @SpyK
    private var objectMapper: ObjectMapper = jacksonObjectMapper()

    @MockK
    private lateinit var collectionMetaService: CollectionMetaService

    @Test
    fun `full refresh`() = runBlocking<Unit> {
        val from = randomEthCollectionId()
        val blockchain = BlockchainDto.ETHEREUM
        val collectionWithMeta = randomEnrichmentCollection().copy(metaEntry = randomCollectionMetaDownloadEntry())
        val collectionWithoutMeta = randomEnrichmentCollection().copy(metaEntry = null)
        coEvery {
            collectionRepository.findAll(
                fromIdExcluded = EnrichmentCollectionId(from),
                blockchain = blockchain
            )
        } returns flowOf(
            collectionWithMeta,
            collectionWithoutMeta,
        )

        coEvery {
            collectionMetaService.schedule(
                collectionId = collectionWithMeta.id.toDto(),
                pipeline = CollectionMetaPipeline.REFRESH,
                force = true,
                source = DownloadTaskSource.INTERNAL,
                priority = 0
            )
        } returns Unit

        coEvery {
            collectionMetaService.schedule(
                collectionId = collectionWithoutMeta.id.toDto(),
                pipeline = CollectionMetaPipeline.REFRESH,
                force = true,
                source = DownloadTaskSource.INTERNAL,
                priority = 0
            )
        } returns Unit

        val result = refreshCollectionMetaTask.runLongTask(
            from = from.toString(),
            param = objectMapper.writeValueAsString(
                RefreshCollectionMetaTaskParam(
                    blockchain = blockchain,
                    full = true,
                    priority = 0,
                )
            )
        ).toList()

        assertThat(result).containsExactly(collectionWithMeta.id.toString(), collectionWithoutMeta.id.toString())
    }

    @Test
    fun `partial refresh`() = runBlocking<Unit> {
        val from = randomEthCollectionId()
        val blockchain = BlockchainDto.POLYGON
        val collectionWithMeta = randomEnrichmentCollection().copy(metaEntry = randomCollectionMetaDownloadEntry())
        val collectionWithoutMeta = randomEnrichmentCollection().copy(metaEntry = null)
        coEvery {
            collectionRepository.findAll(
                fromIdExcluded = EnrichmentCollectionId(from),
                blockchain = blockchain
            )
        } returns flowOf(
            collectionWithMeta,
            collectionWithoutMeta,
        )

        coEvery {
            collectionMetaService.schedule(
                collectionId = collectionWithoutMeta.id.toDto(),
                pipeline = CollectionMetaPipeline.REFRESH,
                force = true,
                source = DownloadTaskSource.INTERNAL,
                priority = 1
            )
        } returns Unit

        val result = refreshCollectionMetaTask.runLongTask(
            from = from.toString(),
            param = objectMapper.writeValueAsString(
                RefreshCollectionMetaTaskParam(
                    blockchain = blockchain,
                    full = false,
                    priority = 1,
                )
            )
        ).toList()

        assertThat(result).containsExactly(collectionWithMeta.id.toString(), collectionWithoutMeta.id.toString())
    }
}
