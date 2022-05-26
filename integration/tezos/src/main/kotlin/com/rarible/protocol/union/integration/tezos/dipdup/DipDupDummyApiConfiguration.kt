package com.rarible.protocol.union.integration.tezos.dipdup

import com.rarible.protocol.union.integration.tezos.dipdup.service.DipdupOrderActivityService
import com.rarible.protocol.union.integration.tezos.dipdup.service.DipdupOrderService
import com.rarible.protocol.union.integration.tezos.dipdup.service.TzktCollectionService
import com.rarible.protocol.union.integration.tezos.dipdup.service.TzktItemActivityService
import com.rarible.protocol.union.integration.tezos.dipdup.service.TzktItemService
import com.rarible.protocol.union.integration.tezos.dipdup.service.TzktOwnershipService
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnProperty(name = ["integration.tezos.dipdup.enabled"], havingValue = "false")
class DipDupDummyApiConfiguration {

    @Bean
    fun dipdupDummyOrderService() = object: DipdupOrderService {}

    @Bean
    fun dipdupDummyOrderActivityService() = object: DipdupOrderActivityService {}

    @Bean
    fun tzktDummyCollectionService() = object: TzktCollectionService {}

    @Bean
    fun tzktDummyItemService() = object: TzktItemService {}

    @Bean
    fun tzktDummyOwnershipService() = object : TzktOwnershipService {}

    @Bean
    fun tzktItemActivityService() = object: TzktItemActivityService {}

}
