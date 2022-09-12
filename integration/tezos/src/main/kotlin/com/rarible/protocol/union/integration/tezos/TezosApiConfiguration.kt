package com.rarible.protocol.union.integration.tezos

import com.rarible.protocol.tezos.api.client.FixedTezosApiServiceUriProvider
import com.rarible.protocol.tezos.api.client.NftActivityControllerApi
import com.rarible.protocol.tezos.api.client.NftCollectionControllerApi
import com.rarible.protocol.tezos.api.client.NftItemControllerApi
import com.rarible.protocol.tezos.api.client.NftOwnershipControllerApi
import com.rarible.protocol.tezos.api.client.OrderActivityControllerApi
import com.rarible.protocol.tezos.api.client.OrderControllerApi
import com.rarible.protocol.tezos.api.client.OrderSignatureControllerApi
import com.rarible.protocol.tezos.api.client.TezosApiClientFactory
import com.rarible.protocol.tezos.api.client.TezosApiServiceUriProvider
import com.rarible.protocol.union.core.CoreConfiguration
import com.rarible.protocol.union.core.service.AuctionService
import com.rarible.protocol.union.core.service.OrderService
import com.rarible.protocol.union.core.service.router.OrderProxyService
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.integration.tezos.converter.TezosActivityConverter
import com.rarible.protocol.union.integration.tezos.converter.TezosOrderConverter
import com.rarible.protocol.union.integration.tezos.dipdup.DipDupApiConfiguration
import com.rarible.protocol.union.integration.tezos.dipdup.DipDupDummyApiConfiguration
import com.rarible.protocol.union.integration.tezos.dipdup.service.DipdupOrderActivityService
import com.rarible.protocol.union.integration.tezos.dipdup.service.DipdupOrderService
import com.rarible.protocol.union.integration.tezos.dipdup.service.TzktCollectionService
import com.rarible.protocol.union.integration.tezos.dipdup.service.TzktItemActivityService
import com.rarible.protocol.union.integration.tezos.dipdup.service.TzktItemService
import com.rarible.protocol.union.integration.tezos.dipdup.service.TzktOwnershipService
import com.rarible.protocol.union.integration.tezos.dipdup.service.TzktSignatureService
import com.rarible.protocol.union.integration.tezos.entity.TezosCollectionRepository
import com.rarible.protocol.union.integration.tezos.service.TezosActivityService
import com.rarible.protocol.union.integration.tezos.service.TezosAuctionService
import com.rarible.protocol.union.integration.tezos.service.TezosCollectionService
import com.rarible.protocol.union.integration.tezos.service.TezosItemService
import com.rarible.protocol.union.integration.tezos.service.TezosOrderService
import com.rarible.protocol.union.integration.tezos.service.TezosOwnershipService
import com.rarible.protocol.union.integration.tezos.service.TezosSignatureService
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Import
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import java.net.URI


@TezosConfiguration
@Import(value = [CoreConfiguration::class, DipDupApiConfiguration::class, DipDupDummyApiConfiguration::class])
@ComponentScan(basePackageClasses = [TezosOrderConverter::class])
@EnableConfigurationProperties(value = [TezosIntegrationProperties::class])
class TezosApiConfiguration(
    private val properties: TezosIntegrationProperties
) {

    @Bean
    fun tezosBlockchain(): BlockchainDto {
        return BlockchainDto.TEZOS
    }

    @Bean
    fun tezosFixedApiServiceUriProvider(): TezosApiServiceUriProvider {
        return FixedTezosApiServiceUriProvider(URI(properties.client!!.url!!))
    }

    @Bean
    fun tezosItemApi(factory: TezosApiClientFactory): NftItemControllerApi =
        factory.createNftItemApiClient()

    @Bean
    fun tezosOwnershipApi(factory: TezosApiClientFactory): NftOwnershipControllerApi =
        factory.createNftOwnershipApiClient()

    @Bean
    fun tezosCollectionApi(factory: TezosApiClientFactory): NftCollectionControllerApi =
        factory.createNftCollectionApiClient()

    @Bean
    fun tezosOrderApi(factory: TezosApiClientFactory): OrderControllerApi =
        factory.createOrderApiClient()

    @Bean
    fun tezosSignatureApi(factory: TezosApiClientFactory): OrderSignatureControllerApi =
        factory.createOrderSignatureApiClient()

    @Bean
    fun tezosNftActivityApi(factory: TezosApiClientFactory): OrderActivityControllerApi =
        factory.createOrderActivityApiClient()

    @Bean
    fun tezosOrderActivityApi(factory: TezosApiClientFactory): NftActivityControllerApi =
        factory.createNftActivityApiClient()

    //-------------------- Services --------------------//

    @Bean
    fun tezosItemService(controllerApi: NftItemControllerApi, tzktItemService: TzktItemService): TezosItemService {
        return TezosItemService(controllerApi, tzktItemService)
    }

    @Bean
    fun tezosOwnershipService(
        controllerApi: NftOwnershipControllerApi,
        tzktOwnershipService: TzktOwnershipService
    ): TezosOwnershipService {
        return TezosOwnershipService(controllerApi, tzktOwnershipService)
    }

    @Bean
    fun tezosCollectionService(
        controllerApi: NftCollectionControllerApi,
        tzktCollectionService: TzktCollectionService,
        tezosCollectionRepository: TezosCollectionRepository,
    ): TezosCollectionService {
        return TezosCollectionService(controllerApi, tzktCollectionService, tezosCollectionRepository)
    }

    @Bean
    fun tezosOrderService(
        controllerApi: OrderControllerApi,
        converter: TezosOrderConverter,
        dipdupOrderService: DipdupOrderService,
        tezosIntegrationProperties: TezosIntegrationProperties
    ): OrderService {
        return OrderProxyService(
            TezosOrderService(controllerApi, converter, dipdupOrderService, tezosIntegrationProperties),
            setOf(PlatformDto.RARIBLE)
        )
    }

    @Bean
    fun tezosAuctionService(): AuctionService {
        return TezosAuctionService(BlockchainDto.TEZOS)
    }

    @Bean
    fun tezosSignatureService(
        controllerApi: OrderSignatureControllerApi,
        tzktSignatureService: TzktSignatureService
    ): TezosSignatureService {
        return TezosSignatureService(controllerApi, tzktSignatureService)
    }

    @Bean
    fun tezosActivityService(
        itemActivityApi: NftActivityControllerApi,
        orderActivityApi: OrderActivityControllerApi,
        converter: TezosActivityConverter,
        dipdupOrderActivityService: DipdupOrderActivityService,
        tzktItemActivityService: TzktItemActivityService
    ): TezosActivityService {
        return TezosActivityService(
            itemActivityApi,
            orderActivityApi,
            converter,
            dipdupOrderActivityService,
            tzktItemActivityService
        )
    }

    @Bean
    fun tezosCollectionRepository(mongoTemplate: ReactiveMongoOperations): TezosCollectionRepository {
        return TezosCollectionRepository(mongoTemplate)
    }
}
