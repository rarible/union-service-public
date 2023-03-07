package com.rarible.protocol.union.worker.task

import com.rarible.protocol.union.core.model.UnionImageProperties
import com.rarible.protocol.union.core.model.UnionItemUpdateEvent
import com.rarible.protocol.union.core.model.UnionMetaContent
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.MetaContentDto
import com.rarible.protocol.union.enrichment.meta.embedded.EmbeddedContentUrlProvider
import com.rarible.protocol.union.enrichment.model.ShortItemId
import com.rarible.protocol.union.enrichment.repository.ItemRepository
import com.rarible.protocol.union.enrichment.service.EnrichmentItemEventService
import com.rarible.protocol.union.enrichment.service.EnrichmentItemService
import com.rarible.protocol.union.enrichment.test.data.randomItemMetaDownloadEntry
import com.rarible.protocol.union.enrichment.test.data.randomShortItem
import com.rarible.protocol.union.enrichment.test.data.randomUnionItem
import com.rarible.protocol.union.enrichment.test.data.randomUnionMeta
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import scalether.domain.Address

@ExtendWith(MockKExtension::class)
internal class FixEmbeddedContentAvailableTaskTest {
    @InjectMockKs
    private lateinit var fixEmbeddedContentAvailableTask: FixEmbeddedContentAvailableTask

    @MockK
    private lateinit var itemRepository: ItemRepository

    @MockK
    private lateinit var itemService: EnrichmentItemService

    @MockK
    private lateinit var embeddedContentUrlProvider: EmbeddedContentUrlProvider

    @MockK
    private lateinit var enrichmentItemEventService: EnrichmentItemEventService

    @Test
    fun runTask() = runBlocking<Unit> {
        val itemWithoutMeta = randomShortItem().copy(metaEntry = null)
        val itemWithoutMetaData = randomShortItem().copy(metaEntry = randomItemMetaDownloadEntry(data = null))
        val itemWithEmptyContent =
            randomShortItem().copy(
                metaEntry = randomItemMetaDownloadEntry(
                    data = randomUnionMeta().copy(
                        content = emptyList()
                    )
                )
            )
        val itemWithoutEmbedded =
            randomShortItem().copy(
                metaEntry = randomItemMetaDownloadEntry(
                    data = randomUnionMeta().copy(
                        content = listOf(
                            UnionMetaContent(
                                url = "test",
                                representation = MetaContentDto.Representation.ORIGINAL,
                                properties = UnionImageProperties()
                            )
                        )
                    )
                )
            )
        val itemWithoutProperties =
            randomShortItem().copy(
                metaEntry = randomItemMetaDownloadEntry(
                    data = randomUnionMeta().copy(
                        content = listOf(
                            UnionMetaContent(
                                url = "${EmbeddedContentUrlProvider.EMBEDDED_SCHEMA}test",
                                representation = MetaContentDto.Representation.ORIGINAL,
                                properties = null
                            )
                        )
                    )
                )
            )
        val itemWithAvailableContent =
            randomShortItem().copy(
                metaEntry = randomItemMetaDownloadEntry(
                    data = randomUnionMeta().copy(
                        content = listOf(
                            UnionMetaContent(
                                url = "${EmbeddedContentUrlProvider.EMBEDDED_SCHEMA}test",
                                representation = MetaContentDto.Representation.ORIGINAL,
                                properties = UnionImageProperties(available = true)
                            )
                        )
                    )
                )
            )
        val unavailableMeta = randomUnionMeta()
        val unavailableEntry = randomItemMetaDownloadEntry(
            data = unavailableMeta.copy(
                content = listOf(
                    UnionMetaContent(
                        url = "test",
                        representation = MetaContentDto.Representation.ORIGINAL,
                        properties = UnionImageProperties(available = false)
                    ),
                    UnionMetaContent(
                        url = "${EmbeddedContentUrlProvider.EMBEDDED_SCHEMA}test",
                        representation = MetaContentDto.Representation.ORIGINAL,
                        properties = UnionImageProperties(available = false)
                    )
                )
            )
        )
        val itemWithUnavailableContent = randomShortItem().copy(metaEntry = unavailableEntry)
        val itemWithUnavailableContentForUpdate = randomShortItem().copy(metaEntry = unavailableEntry)
        val fromId = ShortItemId(BlockchainDto.ETHEREUM, "${Address.ONE()}:1")
        coEvery { itemRepository.findAll(fromId) }.returns(
            listOf(
                itemWithoutMeta,
                itemWithoutMetaData,
                itemWithEmptyContent,
                itemWithoutEmbedded,
                itemWithoutProperties,
                itemWithAvailableContent,
                itemWithUnavailableContent
            ).asFlow()
        )
        val unionItem = randomUnionItem(itemWithUnavailableContent.id.toDto())
        coEvery { itemService.fetch(itemWithUnavailableContent.id) }.returns(unionItem)
        every { embeddedContentUrlProvider.isEmbeddedContentUrl("test") }.returns(false)
        every {
            embeddedContentUrlProvider.isEmbeddedContentUrl("${EmbeddedContentUrlProvider.EMBEDDED_SCHEMA}test")
        }.returns(true)
        coEvery { itemRepository.get(itemWithUnavailableContent.id) }.returns(itemWithUnavailableContentForUpdate)
        coEvery { itemRepository.get(itemWithoutEmbedded.id) }.returns(itemWithoutEmbedded)
        coEvery { itemRepository.get(itemWithoutProperties.id) }.returns(itemWithoutProperties)
        coEvery { itemRepository.get(itemWithAvailableContent.id) }.returns(itemWithAvailableContent)
        coEvery { itemRepository.get(itemWithEmptyContent.id) }.returns(itemWithEmptyContent)
        coEvery { itemRepository.get(itemWithoutMetaData.id) }.returns(itemWithAvailableContent)
        coEvery { itemRepository.get(itemWithoutMeta.id) }.returns(itemWithAvailableContent)
        val expectedItem = itemWithUnavailableContentForUpdate.copy(
            metaEntry = unavailableEntry.copy(
                data = unavailableMeta.copy(
                    content = listOf(
                        UnionMetaContent(
                            url = "test",
                            representation = MetaContentDto.Representation.ORIGINAL,
                            properties = UnionImageProperties(available = false)
                        ),
                        UnionMetaContent(
                            url = "${EmbeddedContentUrlProvider.EMBEDDED_SCHEMA}test",
                            representation = MetaContentDto.Representation.ORIGINAL,
                            properties = UnionImageProperties(available = true)
                        )
                    )
                )
            )
        )
        coEvery { itemRepository.save(expectedItem) }.returns(randomShortItem())
        val expectedEvent = UnionItemUpdateEvent(
            item = unionItem,
            eventTimeMarks = null
        )
        coEvery { enrichmentItemEventService.onItemUpdated(expectedEvent) }.returns(Unit)

        assertThat(
            fixEmbeddedContentAvailableTask.runLongTask(from = fromId.toString(), param = "").toList()
        ).containsExactly(itemWithoutMeta.id.toString())

        coVerify { itemRepository.save(expectedItem) }
        coVerify { enrichmentItemEventService.onItemUpdated(expectedEvent) }
        confirmVerified(enrichmentItemEventService)
    }
}