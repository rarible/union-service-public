package com.rarible.protocol.union.worker.task

import com.rarible.protocol.union.enrichment.custom.collection.CustomCollectionResolutionRequest
import com.rarible.protocol.union.enrichment.custom.collection.CustomCollectionResolver
import com.rarible.protocol.union.enrichment.model.EnrichmentActivity
import com.rarible.protocol.union.enrichment.model.EnrichmentMintActivity
import com.rarible.protocol.union.enrichment.repository.ActivityRepository
import com.rarible.protocol.union.enrichment.test.data.randomEnrichmentMintActivity
import com.rarible.protocol.union.enrichment.test.data.randomUnionCollection
import com.rarible.protocol.union.worker.IntegrationTest
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@IntegrationTest
class ActivityCollectionFixTaskJobIt {

    lateinit var job: ActivityCollectionFixTaskJob

    @Autowired
    lateinit var repository: ActivityRepository

    private val customCollectionResolver: CustomCollectionResolver = mockk()

    @BeforeEach
    fun beforeEach() {
        job = ActivityCollectionFixTaskJob(repository, customCollectionResolver)
    }

    @Test
    fun `execute - ok`() = runBlocking<Unit> {
        val customCollection = randomUnionCollection().id

        val toUpdate = (randomEnrichmentMintActivity() as EnrichmentMintActivity).copy(collection = null)
        val toUpdateCustom = (randomEnrichmentMintActivity() as EnrichmentMintActivity).copy(collection = null)

        val valid = repository.save(randomEnrichmentMintActivity())
        repository.save(toUpdate)
        repository.save(toUpdateCustom)

        coEvery {
            customCollectionResolver.resolve(any<List<CustomCollectionResolutionRequest<EnrichmentActivity>>>(), any())
        } returns mapOf(toUpdateCustom to customCollection)


        job.handle(null, "").collect()

        assertThat(repository.get(toUpdate.id)?.collection).isNotNull()
        assertThat(repository.get(toUpdateCustom.id)?.collection).isEqualTo(customCollection.fullId())
        assertThat(repository.get(valid.id)).isEqualTo(valid)
    }
}