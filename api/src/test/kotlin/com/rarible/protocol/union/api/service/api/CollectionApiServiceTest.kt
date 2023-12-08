package com.rarible.protocol.union.api.service.api

import com.rarible.protocol.union.core.producer.UnionInternalCollectionEventProducer
import com.rarible.protocol.union.enrichment.repository.CollectionRepository
import com.rarible.protocol.union.enrichment.test.data.randomEnrichmentCollection
import io.mockk.coEvery
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
internal class CollectionApiServiceTest {
    @InjectMockKs
    private lateinit var collectionApiService: CollectionApiService

    @MockK
    private lateinit var collectionRepository: CollectionRepository

    @MockK
    private lateinit var unionInternalCollectionEventProducer: UnionInternalCollectionEventProducer

    @Test
    fun `update has traits changed`() = runBlocking<Unit> {
        val collection = randomEnrichmentCollection().copy(hasTraits = false)

        coEvery { collectionRepository.get(collection.id) } returns collection
        coEvery { collectionRepository.save(collection.copy(hasTraits = true)) } returns collection
        coEvery { unionInternalCollectionEventProducer.sendChangeEvent(collection.id.toDto()) } returns Unit

        val result = collectionApiService.updateHasTraits(id = collection.id, hasTraits = true)

        assertThat(result).isTrue()
    }

    @Test
    fun `update has traits not changed`() = runBlocking<Unit> {
        val collection = randomEnrichmentCollection().copy(hasTraits = true)

        coEvery { collectionRepository.get(collection.id) } returns collection

        val result = collectionApiService.updateHasTraits(id = collection.id, hasTraits = true)

        assertThat(result).isFalse()
    }

    @Test
    fun `filter has traits`() = runBlocking<Unit> {
        val collectionWithTraits = randomEnrichmentCollection().copy(hasTraits = true)
        val collectionWithoutTraits = randomEnrichmentCollection().copy(hasTraits = false)

        coEvery {
            collectionRepository.getAll(
                listOf(
                    collectionWithTraits.id,
                    collectionWithoutTraits.id
                )
            )
        } returns listOf(
            collectionWithTraits,
            collectionWithoutTraits
        )

        val result = collectionApiService.filterHasTraits(
            listOf(
                collectionWithTraits.id.toString(),
                collectionWithoutTraits.id.toString()
            )
        )

        assertThat(result).containsExactly(collectionWithTraits.id.toString())
    }
}
