package com.rarible.protocol.union.core

import com.rarible.protocol.currency.api.client.CurrencyApiClientFactory
import com.rarible.protocol.currency.api.client.CurrencyControllerApi
import com.rarible.protocol.union.core.service.ActivityService
import com.rarible.protocol.union.core.service.AuctionService
import com.rarible.protocol.union.core.service.CollectionService
import com.rarible.protocol.union.core.service.ItemService
import com.rarible.protocol.union.core.service.OrderService
import com.rarible.protocol.union.core.service.OwnershipService
import com.rarible.protocol.union.core.service.SignatureService
import com.rarible.protocol.union.core.service.dummy.DummyActivityService
import com.rarible.protocol.union.core.service.dummy.DummyAuctionService
import com.rarible.protocol.union.core.service.dummy.DummyCollectionService
import com.rarible.protocol.union.core.service.dummy.DummyItemService
import com.rarible.protocol.union.core.service.dummy.DummyOrderService
import com.rarible.protocol.union.core.service.dummy.DummyOwnershipService
import com.rarible.protocol.union.core.service.dummy.DummySignatureService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.core.service.router.BlockchainService
import com.rarible.protocol.union.dto.BlockchainDto
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.core.convert.MappingMongoConverter
import org.springframework.scheduling.annotation.EnableScheduling

@Configuration
@EnableScheduling
@ComponentScan(basePackageClasses = [CoreConfiguration::class])
class CoreConfiguration(
    val enabledBlockchains: List<BlockchainDto>
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val allBlockchains = BlockchainDto.values().toSet()

    @Autowired
    fun setMapKeyDotReplacement(mappingMongoConverter: MappingMongoConverter) {
        mappingMongoConverter.setMapKeyDotReplacement("__DOT__");
    }

    @Bean
    fun itemServiceRouter(services: List<ItemService>): BlockchainRouter<ItemService> {
        val result = ArrayList(services)
        val disabled = getDisabledBlockchains(services)
        disabled.forEach {
            result.add(DummyItemService(it))
            logger.info("ItemService for blockchain {} disabled or not implemented, replaced by dummy", it.name)
        }
        return BlockchainRouter(result, enabledBlockchains)
    }

    @Bean
    fun ownershipServiceRouter(services: List<OwnershipService>): BlockchainRouter<OwnershipService> {
        val result = ArrayList(services)
        val disabled = getDisabledBlockchains(services)
        disabled.forEach {
            result.add(DummyOwnershipService(it))
            logger.info("OwnershipService for blockchain {} disabled or not implemented, replaced by dummy", it.name)
        }
        return BlockchainRouter(result, enabledBlockchains)
    }

    @Bean
    fun collectionServiceRouter(services: List<CollectionService>): BlockchainRouter<CollectionService> {
        val result = ArrayList(services)
        val disabled = getDisabledBlockchains(services)
        disabled.forEach {
            result.add(DummyCollectionService(it))
            logger.info("CollectionService for blockchain {} disabled or not implemented, replaced by dummy", it.name)
        }
        return BlockchainRouter(services, enabledBlockchains)
    }

    @Bean
    fun orderServiceRouter(services: List<OrderService>): BlockchainRouter<OrderService> {
        val result = ArrayList(services)
        val disabled = getDisabledBlockchains(services)
        disabled.forEach {
            result.add(DummyOrderService(it))
            logger.info("OrderService for blockchain {} disabled or not implemented, replaced by dummy", it.name)
        }
        return BlockchainRouter(services, enabledBlockchains)
    }

    @Bean
    fun orderAuctionRouter(services: List<AuctionService>): BlockchainRouter<AuctionService> {
        val result = ArrayList(services)
        val disabled = getDisabledBlockchains(services)
        disabled.forEach {
            result.add(DummyAuctionService(it))
            logger.info("AuctionService for blockchain {} disabled or not implemented, replaced by dummy", it.name)
        }
        return BlockchainRouter(services, enabledBlockchains)
    }

    @Bean
    fun activityServiceRouter(services: List<ActivityService>): BlockchainRouter<ActivityService> {
        val result = ArrayList(services)
        val disabled = getDisabledBlockchains(services)
        disabled.forEach {
            result.add(DummyActivityService(it))
            logger.info("ActivityService for blockchain {} disabled or not implemented, replaced by dummy", it.name)
        }
        return BlockchainRouter(services, enabledBlockchains)
    }

    @Bean
    fun signatureServiceRouter(services: List<SignatureService>): BlockchainRouter<SignatureService> {
        val result = ArrayList(services)
        val disabled = getDisabledBlockchains(services)
        disabled.forEach {
            result.add(DummySignatureService(it))
            logger.info("SignatureService for blockchain {} disabled or not implemented, replaced by dummy", it.name)
        }
        return BlockchainRouter(services, enabledBlockchains)
    }

    @Bean
    fun currencyApi(factory: CurrencyApiClientFactory): CurrencyControllerApi {
        return factory.createCurrencyApiClient()
    }

    private fun <T : BlockchainService> getDisabledBlockchains(services: List<T>): List<BlockchainDto> {
        services.groupBy { it.blockchain }.forEach { e ->
            if (e.value.size > 1) {
                throw IllegalArgumentException(
                    "There are several implementations of service " +
                            "for blockchain ${e.key}: ${e.value.map { it.javaClass.name }}, should be only one" +
                            "implementation"
                )
            }
        }

        val disabledBlockchains = this.allBlockchains.toMutableSet()
        services.forEach { disabledBlockchains.remove(it.blockchain) }
        return disabledBlockchains.toList()
    }
}
