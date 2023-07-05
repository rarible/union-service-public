package com.rarible.protocol.union.enrichment.meta.provider

import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.core.model.download.MetaProviderType
import com.rarible.protocol.union.core.model.download.ProviderDownloadException
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.enrichment.service.SimpleHashService
import com.rarible.protocol.union.enrichment.test.data.randomUnionMeta
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import io.mockk.coEvery
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
internal class SimpleHashItemProviderTest {
    @InjectMockKs
    private lateinit var simpleHashItemProvider: SimpleHashItemProvider

    @MockK
    private lateinit var simpleHashService: SimpleHashService

    @Test
    fun `should throw exception if no content`() = runBlocking<Unit> {
        val itemId = randomEthItemId()
        coEvery { simpleHashService.isSupported(BlockchainDto.ETHEREUM) } returns true
        coEvery { simpleHashService.fetch(itemId) } returns randomUnionMeta(content = emptyList())

        try {
            simpleHashItemProvider.fetch(itemId, null)
            fail("Shouldn't be here")
        } catch (e: ProviderDownloadException) {
            assertThat(e.provider).isEqualTo(MetaProviderType.SIMPLE_HASH)
        }
    }

    @Test
    fun `should throw exception if no meta`() = runBlocking<Unit> {
        coEvery { simpleHashService.isSupported(BlockchainDto.ETHEREUM) } returns true
        val itemId = randomEthItemId()
        coEvery { simpleHashService.fetch(itemId) } returns null

        try {
            simpleHashItemProvider.fetch(itemId, null)
            fail("Shouldn't be here")
        } catch (e: ProviderDownloadException) {
            assertThat(e.provider).isEqualTo(MetaProviderType.SIMPLE_HASH)
        }
    }
}