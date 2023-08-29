package com.rarible.protocol.union.api.controller.internal

import com.rarible.protocol.union.api.controller.test.AbstractIntegrationTest
import com.rarible.protocol.union.api.controller.test.IntegrationTest
import com.rarible.protocol.union.core.converter.EsItemConverter.toEsItem
import com.rarible.protocol.union.core.es.ElasticsearchTestBootstrapper
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.enrichment.converter.ShortItemConverter
import com.rarible.protocol.union.enrichment.converter.ShortOrderConverter
import com.rarible.protocol.union.enrichment.meta.item.ItemMetaPipeline
import com.rarible.protocol.union.enrichment.repository.search.EsItemRepository
import com.rarible.protocol.union.enrichment.service.EnrichmentItemService
import com.rarible.protocol.union.integration.ethereum.converter.EthItemConverter
import com.rarible.protocol.union.integration.ethereum.converter.EthOrderConverter
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import com.rarible.protocol.union.integration.ethereum.data.randomEthNftItemDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthV2OrderDto
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.client.RestTemplate

@IntegrationTest
internal class AdminControllerFt : AbstractIntegrationTest() {
    @Autowired
    lateinit var restTemplate: RestTemplate

    @Autowired
    lateinit var ethOrderConverter: EthOrderConverter

    @Autowired
    lateinit var enrichmentItemService: EnrichmentItemService

    @Autowired
    lateinit var esItemRepository: EsItemRepository

    @Autowired
    lateinit var elasticsearchTestBootstrapper: ElasticsearchTestBootstrapper

    @BeforeEach
    fun setUp() {
        elasticsearchTestBootstrapper.bootstrap()
    }

    @Test
    fun `get cheapest order for collection`() = runBlocking<Unit> {
        val ethItemId = randomEthItemId()
        val ethItem = randomEthNftItemDto(ethItemId)
        val ethUnionItem = EthItemConverter.convert(ethItem, ethItemId.blockchain)
        val ethOrder = randomEthV2OrderDto(ethItemId)
        val ethUnionOrder = ethOrderConverter.convert(ethOrder, ethItemId.blockchain)
        val ethShortItem = ShortItemConverter.convert(ethUnionItem)
            .copy(bestSellOrder = ShortOrderConverter.convert(ethUnionOrder))
        enrichmentItemService.save(ethShortItem)
        val itemDto = enrichmentItemService.enrichItem(
            shortItem = ethShortItem,
            item = ethUnionItem,
            orders = mapOf(ethUnionOrder.id to ethUnionOrder),
            metaPipeline = ItemMetaPipeline.API
        )
        esItemRepository.bulk(entitiesToSave = listOf(itemDto.toEsItem()))
        ethereumOrderControllerApiMock.mockGetById(ethOrder)

        val result = restTemplate.getForObject(
            "$baseUri/admin/collections/{collectionId}/cheapestOrder",
            OrderDto::class.java,
            ethUnionItem.collection!!.fullId()
        )

        assertThat(result.id).isEqualTo(ethUnionOrder.id)
    }
}
