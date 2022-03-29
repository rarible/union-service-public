package com.rarible.protocol.union.search.indexer.config

import com.rarible.core.daemon.sequential.ConsumerBatchEventHandler
import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.search.core.config.SearchConfiguration
import com.rarible.protocol.union.search.indexer.test.IntegrationTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.context.junit4.SpringRunner

@IntegrationTest
class ActivityConsumerIntegrationTest {

    @Autowired
    private lateinit var handler: ConsumerBatchEventHandler<ActivityDto>

    @Test
    fun `should bring context up`() {
        // TODO fix No qualifying bean ActivityEsRepository
    }
}