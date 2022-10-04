package com.rarible.protocol.union.api.controller

import com.rarible.protocol.union.api.controller.test.AbstractIntegrationTest
import com.rarible.protocol.union.api.controller.test.IntegrationTest
import com.rarible.protocol.union.dto.ItemsDto
import com.rarible.protocol.union.enrichment.repository.ItemRepository
import com.rarible.protocol.union.enrichment.test.data.randomShortItem
import com.rarible.protocol.union.integration.ethereum.data.randomEthNftItemDto
import io.mockk.every
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import reactor.kotlin.core.publisher.toFlux
import java.time.Instant

@FlowPreview
@IntegrationTest
internal class ItemReconciliationControllerFt : AbstractIntegrationTest() {
    @Autowired
    private lateinit var itemRepository: ItemRepository

    @Test
    fun getItems() {
        val item1 = runBlocking {
            itemRepository.save(randomShortItem().copy(lastUpdatedAt = Instant.ofEpochMilli(2000)))
        }
        val item2 = runBlocking {
            itemRepository.save(randomShortItem().copy(lastUpdatedAt = Instant.ofEpochMilli(3000)))
        }

        val item1Dto = randomEthNftItemDto(item1.id.toDto())
        val item2Dto = randomEthNftItemDto(item2.id.toDto())

        every {
            testEthereumItemApi.getNftItemsByIds(match {
                it.ids.contains(item1Dto.id) && it.ids.contains(item2Dto.id) && it.ids.size == 2
            })
        } returns listOf(item1Dto, item2Dto).toFlux()

        val result = testRestTemplate.getForObject(
            "$baseUri/reconciliation/items?lastUpdatedFrom={from}&lastUpdatedTo={to}",
            ItemsDto::class.java,
            Instant.ofEpochMilli(1000),
            Instant.ofEpochMilli(4000)
        )!!

        assertThat(result.items.map { it.id.fullId() }).containsExactly(
            item1.id.toDto().fullId(),
            item2.id.toDto().fullId()
        )
    }
}
