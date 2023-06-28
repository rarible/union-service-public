package com.rarible.protocol.union.listener.repository

import com.rarible.protocol.union.enrichment.model.MetaAutoRefreshState
import com.rarible.protocol.union.enrichment.model.MetaAutoRefreshStatus
import com.rarible.protocol.union.enrichment.repository.MetaAutoRefreshStateRepository
import com.rarible.protocol.union.integration.ethereum.data.randomEthCollectionId
import com.rarible.protocol.union.listener.test.AbstractIntegrationTest
import com.rarible.protocol.union.listener.test.IntegrationTest
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant
import java.time.temporal.ChronoUnit

@IntegrationTest
class MetaAutoRefreshStateRepositoryIt : AbstractIntegrationTest() {
    @Autowired
    private lateinit var metaAutoRefreshStateRepository: MetaAutoRefreshStateRepository

    @Test
    fun crud() = runBlocking<Unit> {
        val collectionId1 = randomEthCollectionId().fullId()
        val collectionId2 = randomEthCollectionId().fullId()
        val collectionId3 = randomEthCollectionId().fullId()
        val collectionId4 = randomEthCollectionId().fullId()
        metaAutoRefreshStateRepository.save(
            MetaAutoRefreshState(
                id = collectionId1,
                status = MetaAutoRefreshStatus.CREATED,
                createdAt = Instant.now()
            )
        )
        metaAutoRefreshStateRepository.save(
            MetaAutoRefreshState(
                id = collectionId2,
                status = MetaAutoRefreshStatus.REFRESHED,
                createdAt = Instant.now(),
                lastRefreshedAt = Instant.now()
            )
        )
        metaAutoRefreshStateRepository.save(
            MetaAutoRefreshState(
                id = collectionId3,
                status = MetaAutoRefreshStatus.CREATED,
                createdAt = Instant.now().minus(31, ChronoUnit.DAYS)
            )
        )
        metaAutoRefreshStateRepository.save(
            MetaAutoRefreshState(
                id = collectionId4,
                status = MetaAutoRefreshStatus.REFRESHED,
                createdAt = Instant.now().minus(20, ChronoUnit.DAYS),
                lastRefreshedAt = Instant.now().minus(15, ChronoUnit.DAYS),
            )
        )

        val result1 = metaAutoRefreshStateRepository.loadToCheckCreated(Instant.now().minus(30, ChronoUnit.DAYS)).toList()
        assertThat(result1.map { it.id }).containsExactlyInAnyOrder(collectionId1)

        val result2 = metaAutoRefreshStateRepository.loadToCheckRefreshed(
            createFromDate = Instant.now().minus(30, ChronoUnit.DAYS),
            refreshedFromDate = Instant.now().minus(14, ChronoUnit.DAYS)
        ).toList()
        assertThat(result2.map { it.id }).containsExactlyInAnyOrder(collectionId2)

        metaAutoRefreshStateRepository.delete(collectionId1)

        val result3 = metaAutoRefreshStateRepository.loadToCheckCreated(Instant.now().minus(30, ChronoUnit.DAYS)).toList()
        assertThat(result3).isEmpty()
    }
}