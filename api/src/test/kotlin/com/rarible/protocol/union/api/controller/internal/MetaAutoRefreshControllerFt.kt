package com.rarible.protocol.union.api.controller.internal

import com.rarible.protocol.union.api.controller.test.AbstractIntegrationTest
import com.rarible.protocol.union.api.controller.test.IntegrationTest
import com.rarible.protocol.union.enrichment.repository.CollectionRepository
import com.rarible.protocol.union.enrichment.repository.MetaAutoRefreshStateRepository
import com.rarible.protocol.union.enrichment.test.data.randomEnrichmentCollection
import com.rarible.protocol.union.integration.ethereum.data.randomEthCollectionId
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.postForObject
import java.time.Instant

@IntegrationTest
internal class MetaAutoRefreshControllerFt : AbstractIntegrationTest() {
    @Autowired
    private lateinit var metaAutoRefreshStateRepository: MetaAutoRefreshStateRepository

    @Autowired
    private lateinit var collectionRepository: CollectionRepository

    private val restTemplate: RestTemplate = RestTemplate()

    @Test
    fun crud() = runBlocking<Unit> {
        val collection = collectionRepository.save(
            randomEnrichmentCollection()
        )

        restTemplate.postForObject<Unit?>("$baseUri/maintenance/items/meta/autoRefresh/${collection.id}")

        val result1 = metaAutoRefreshStateRepository.loadToCheckCreated(Instant.now().minusSeconds(30)).toList()

        assertThat(result1.map { it.id }).containsExactly(collection.id.toString())

        restTemplate.delete("$baseUri/maintenance/items/meta/autoRefresh/${collection.id}")

        val result2 = metaAutoRefreshStateRepository.loadToCheckCreated(Instant.now().minusSeconds(30)).toList()
        assertThat(result2).isEmpty()

        try {
            restTemplate.postForObject<Unit>("$baseUri/maintenance/items/meta/autoRefresh/${randomEthCollectionId()}")
            fail("should throw exception")
        } catch (e: HttpClientErrorException) {
            assertThat(e.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }
    }
}