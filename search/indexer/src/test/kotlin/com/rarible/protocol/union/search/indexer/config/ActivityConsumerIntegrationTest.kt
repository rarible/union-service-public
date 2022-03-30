package com.rarible.protocol.union.search.indexer.config

import com.rarible.core.daemon.sequential.ConsumerBatchEventHandler
import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.search.core.repository.ActivityEsRepository
import com.rarible.protocol.union.search.indexer.test.IntegrationTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.elasticsearch.repository.config.EnableReactiveElasticsearchRepositories

@IntegrationTest
@EnableReactiveElasticsearchRepositories(basePackages = [
    "com.rarible.protocol.union.search"
])
class ActivityConsumerIntegrationTest {

    @Autowired
    private lateinit var handler: ConsumerBatchEventHandler<ActivityDto>

    @Autowired
    private lateinit var activityEsRepository: ActivityEsRepository

    @Test
    fun `should bring context up`() {
        // TODO fix No qualifying bean ActivityEsRepository
    }
}