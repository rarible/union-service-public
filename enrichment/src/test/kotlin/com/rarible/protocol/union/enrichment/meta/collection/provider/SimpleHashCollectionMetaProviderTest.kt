package com.rarible.protocol.union.enrichment.meta.collection.provider

import com.rarible.core.test.data.randomAddress
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.enrichment.download.ProviderDownloadException
import com.rarible.protocol.union.enrichment.service.SimpleHashService
import com.rarible.protocol.union.enrichment.test.data.randomUnionCollectionMeta
import com.rarible.protocol.union.enrichment.test.data.randomUnionContent
import com.rarible.protocol.union.integration.ethereum.data.randomEthCollectionId
import io.mockk.coEvery
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
internal class SimpleHashCollectionMetaProviderTest {
    @InjectMockKs
    private lateinit var simpleHashCollectionMetaProvider: SimpleHashCollectionMetaProvider

    @MockK
    private lateinit var simpleHashService: SimpleHashService

    @Test
    fun `original meta exists - skip`() = runBlocking<Unit> {
        val original = randomUnionCollectionMeta()

        val result = simpleHashCollectionMetaProvider.fetch(
            blockchain = BlockchainDto.ETHEREUM,
            id = randomAddress().toString(),
            original = original,
        )

        assertThat(result).isEqualTo(original)
    }

    @Test
    fun `blockchain not supported - skip`() = runBlocking<Unit> {
        coEvery { simpleHashService.isSupportedCollection(BlockchainDto.ETHEREUM) } returns false

        val result = simpleHashCollectionMetaProvider.fetch(
            blockchain = BlockchainDto.ETHEREUM,
            id = randomAddress().toString(),
            original = null,
        )

        assertThat(result).isEqualTo(null)
    }

    @Test
    fun `meta found - ok`() = runBlocking<Unit> {
        coEvery { simpleHashService.isSupportedCollection(BlockchainDto.ETHEREUM) } returns true
        val collectionId = randomEthCollectionId()
        val meta = randomUnionCollectionMeta().copy(content = listOf(randomUnionContent()))
        coEvery { simpleHashService.fetch(collectionId) } returns meta

        val result = simpleHashCollectionMetaProvider.fetch(
            blockchain = collectionId.blockchain,
            id = collectionId.value,
            original = null,
        )

        assertThat(result).isEqualTo(meta)
    }

    @Test
    fun `meta content empty - fail`() = runBlocking<Unit> {
        coEvery { simpleHashService.isSupportedCollection(BlockchainDto.ETHEREUM) } returns true
        val collectionId = randomEthCollectionId()
        val meta = randomUnionCollectionMeta()
        coEvery { simpleHashService.fetch(collectionId) } returns meta

        assertThatExceptionOfType(ProviderDownloadException::class.java).isThrownBy {
            runBlocking {
                simpleHashCollectionMetaProvider.fetch(
                    blockchain = collectionId.blockchain,
                    id = collectionId.value,
                    original = null,
                )
            }
        }.withMessageContaining("SIMPLE_HASH")
    }

    @Test
    fun `meta not found - fail`() = runBlocking<Unit> {
        coEvery { simpleHashService.isSupportedCollection(BlockchainDto.ETHEREUM) } returns true
        val collectionId = randomEthCollectionId()
        coEvery { simpleHashService.fetch(collectionId) } returns null

        assertThatExceptionOfType(ProviderDownloadException::class.java).isThrownBy {
            runBlocking {
                simpleHashCollectionMetaProvider.fetch(
                    blockchain = collectionId.blockchain,
                    id = collectionId.value,
                    original = null,
                )
            }
        }.withMessageContaining("SIMPLE_HASH")
    }
}
