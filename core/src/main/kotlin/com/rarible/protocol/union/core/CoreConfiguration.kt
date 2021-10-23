package com.rarible.protocol.union.core

import com.rarible.protocol.currency.api.client.CurrencyApiClientFactory
import com.rarible.protocol.currency.api.client.CurrencyControllerApi
import com.rarible.protocol.union.core.service.ActivityService
import com.rarible.protocol.union.core.service.CollectionService
import com.rarible.protocol.union.core.service.ItemService
import com.rarible.protocol.union.core.service.OrderService
import com.rarible.protocol.union.core.service.OwnershipService
import com.rarible.protocol.union.core.service.SignatureService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.core.service.router.BlockchainService
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling

@Configuration
@EnableScheduling
@ComponentScan(basePackageClasses = [CoreConfiguration::class])
@EnableConfigurationProperties(value = [IntegrationProperties::class])
class CoreConfiguration(
    integrationProperties: IntegrationProperties
) {

    val enabledBlockchains = integrationProperties.integrations.keys

    //-------------------- Routers ---------------------//
    @Bean
    fun itemServiceRouter(services: List<ItemService>): BlockchainRouter<ItemService> {
        return BlockchainRouter(filter(services))
    }

    @Bean
    fun ownershipServiceRouter(services: List<OwnershipService>): BlockchainRouter<OwnershipService> {
        return BlockchainRouter(filter(services))
    }

    @Bean
    fun collectionServiceRouter(services: List<CollectionService>): BlockchainRouter<CollectionService> {
        return BlockchainRouter(filter(services))
    }

    @Bean
    fun orderServiceRouter(services: List<OrderService>): BlockchainRouter<OrderService> {
        return BlockchainRouter(filter(services))
    }

    @Bean
    fun activityServiceRouter(services: List<ActivityService>): BlockchainRouter<ActivityService> {
        return BlockchainRouter(filter(services))
    }

    @Bean
    fun signatureServiceRouter(services: List<SignatureService>): BlockchainRouter<SignatureService> {
        return BlockchainRouter(filter(services))
    }

    private fun <T : BlockchainService> filter(services: List<T>): List<T> {
        return services.filter { enabledBlockchains.contains(it.blockchain) }
    }

    //-------------------- CURRENCY ---------------------//
    @Bean
    fun currencyApi(factory: CurrencyApiClientFactory): CurrencyControllerApi {
        return factory.createCurrencyApiClient()
    }

}