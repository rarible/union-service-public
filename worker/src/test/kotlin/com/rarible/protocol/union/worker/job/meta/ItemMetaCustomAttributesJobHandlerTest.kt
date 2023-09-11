package com.rarible.protocol.union.worker.job.meta

import com.rarible.core.test.data.randomString
import com.rarible.protocol.union.enrichment.meta.item.ItemMetaPipeline
import com.rarible.protocol.union.enrichment.meta.item.ItemMetaService
import com.rarible.protocol.union.enrichment.model.ItemMetaCustomAttributes
import com.rarible.protocol.union.enrichment.repository.ItemMetaCustomAttributesRepository
import com.rarible.protocol.union.enrichment.test.data.randomUnionMetaAttribute
import com.rarible.protocol.union.worker.IntegrationTest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import randomItemId

@IntegrationTest
class ItemMetaCustomAttributesJobHandlerTest {

    @Autowired
    lateinit var repository: ItemMetaCustomAttributesRepository

    private val itemMetaService: ItemMetaService =
        mockk { coEvery { schedule(any(), any(), any(), any(), any()) } returns Unit }

    private val provider: MetaCustomAttributesProvider = mockk { every { name } returns "test" }
    private lateinit var handler: ItemMetaCustomAttributesJobHandler

    @BeforeEach
    fun beforeEach() {
        handler = ItemMetaCustomAttributesJobHandler(
            repository = repository,
            providers = listOf(provider),
            itemMetaService = itemMetaService
        )
    }

    @Test
    fun `update attributes - ok, inserted and notified`() = runBlocking<Unit> {
        val itemId = randomItemId()
        val extraAttribute = randomUnionMetaAttribute()
        coEvery { provider.getCustomAttributes() } returns listOf(MetaCustomAttributes(itemId, listOf(extraAttribute)))

        handler.handle()

        val updated = repository.get(itemId)

        assertThat(updated?.attributes).isEqualTo(listOf(extraAttribute))
        coVerify(exactly = 1) { itemMetaService.schedule(itemId, ItemMetaPipeline.REFRESH, true, any(), any()) }
    }

    @Test
    fun `update attributes - ok, updated and notified`() = runBlocking<Unit> {
        val itemId = randomItemId()
        val extraAttribute = randomUnionMetaAttribute()
        val current = extraAttribute.copy(value = randomString())
        repository.save(ItemMetaCustomAttributes(itemId.fullId(), listOf(current)))
        coEvery { provider.getCustomAttributes() } returns listOf(MetaCustomAttributes(itemId, listOf(extraAttribute)))

        handler.handle()

        val updated = repository.get(itemId)

        assertThat(updated?.attributes).isEqualTo(listOf(extraAttribute))
        coVerify(exactly = 1) { itemMetaService.schedule(itemId, ItemMetaPipeline.REFRESH, true, any(), any()) }
    }

    @Test
    fun `update attributes - ok, skipped`() = runBlocking<Unit> {
        val itemId = randomItemId()
        val extraAttribute = randomUnionMetaAttribute()
        repository.save(ItemMetaCustomAttributes(itemId.fullId(), listOf(extraAttribute)))

        coEvery { provider.getCustomAttributes() } returns listOf(MetaCustomAttributes(itemId, listOf(extraAttribute)))

        handler.handle()

        val updated = repository.get(itemId)

        assertThat(updated?.attributes).isEqualTo(listOf(extraAttribute))
        coVerify(exactly = 0) { itemMetaService.schedule(itemId, ItemMetaPipeline.REFRESH, true, any(), any()) }
    }
}
