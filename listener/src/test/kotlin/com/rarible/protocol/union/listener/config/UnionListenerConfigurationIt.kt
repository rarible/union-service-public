package com.rarible.protocol.union.listener.config

import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.listener.test.AbstractIntegrationTest
import com.rarible.protocol.union.listener.test.IntegrationTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.BeanFactory
import org.springframework.beans.factory.annotation.Autowired

@IntegrationTest
internal class UnionListenerConfigurationIt : AbstractIntegrationTest() {
    @Autowired
    private lateinit var beanFactory: BeanFactory

    @Autowired
    private lateinit var activeBlockchains: List<BlockchainDto>

    @Test
    fun `check all required consumers exist`() {
        activeBlockchains.forEach {
            beanFactory.getBean("${it.name.lowercase()}UnionBlockchainEventContainer")
        }
    }
}
