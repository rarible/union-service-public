package com.rarible.protocol.union.integration.tezos.dipdup

import com.rarible.protocol.union.integration.tezos.dipdup.service.DipdupOrderService
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnProperty(name = ["integration.tezos.dipdup.enabled"], havingValue = "false")
class DipDupDummyApiConfiguration {

    @Bean
    fun dipdupDummyOrderService(): DipdupOrderService {
        return object: DipdupOrderService {
            override fun enabled() = false
        }
    }

}
