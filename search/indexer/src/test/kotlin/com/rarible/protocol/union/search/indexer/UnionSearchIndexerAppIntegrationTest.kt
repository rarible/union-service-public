package com.rarible.protocol.union.search.indexer

import com.rarible.core.daemon.sequential.ConsumerWorker
import com.rarible.core.daemon.sequential.ConsumerWorkerHolder
import com.rarible.protocol.union.search.core.config.SearchConfiguration
import com.rarible.protocol.union.search.core.repository.ActivityEsRepository
import com.rarible.protocol.union.search.indexer.config.UnionSearchIndexerConfig
import com.rarible.protocol.union.search.indexer.handler.ActivityEventHandler
import com.rarible.protocol.union.search.indexer.test.IntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.context.annotation.ComponentScan
import org.springframework.test.context.ContextConfiguration
//import com.rarible.protocol.union.search.core.repository.ActivityEsRepository


@IntegrationTest
@EnableAutoConfiguration
@ContextConfiguration(classes = [SearchConfiguration::class])

class UnionSearchIndexerAppIntegrationTest {

    @Autowired
    protected lateinit var activityEsRepository: ActivityEsRepository

    //@Autowired
    //private lateinit var activityEventHandler: ActivityEventHandler

    @Test
    fun `should bring context up`() {
        //assertThat(consumerWorkers).hasSize(1)
    }

}
