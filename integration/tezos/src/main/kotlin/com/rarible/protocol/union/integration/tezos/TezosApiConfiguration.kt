package com.rarible.protocol.union.integration.tezos

import com.fasterxml.jackson.databind.ObjectMapper
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
import com.rarible.protocol.union.integration.tezos.dipdup.PGIntegrationProperties
import com.rarible.protocol.union.integration.tezos.dipdup.service.DipdupOrderActivityService
import com.rarible.protocol.union.integration.tezos.dipdup.service.DipdupOrderService
import com.rarible.protocol.union.integration.tezos.dipdup.service.TzktCollectionService
import com.rarible.protocol.union.integration.tezos.dipdup.service.TzktItemService
import com.rarible.protocol.union.integration.tezos.dipdup.service.TzktOwnershipService
import com.rarible.protocol.union.integration.tezos.service.TezosActivityService
import com.rarible.protocol.union.integration.tezos.service.TezosAuctionService
import com.rarible.protocol.union.integration.tezos.service.TezosCollectionService
import com.rarible.protocol.union.integration.tezos.service.TezosItemService
import com.rarible.protocol.union.integration.tezos.service.TezosOrderService
import com.rarible.protocol.union.integration.tezos.service.TezosOwnershipService
import com.rarible.protocol.union.integration.tezos.service.TezosPgActivityService
import com.rarible.protocol.union.integration.tezos.service.TezosPgCollectionService
import com.rarible.protocol.union.integration.tezos.service.TezosSignatureService
import io.r2dbc.spi.ConnectionFactories
import io.r2dbc.spi.ConnectionFactory
import io.r2dbc.spi.ConnectionFactoryOptions
import io.r2dbc.spi.ConnectionFactoryOptions.DATABASE
import io.r2dbc.spi.ConnectionFactoryOptions.DRIVER
import io.r2dbc.spi.ConnectionFactoryOptions.HOST
import io.r2dbc.spi.ConnectionFactoryOptions.PASSWORD
import io.r2dbc.spi.ConnectionFactoryOptions.PORT
import io.r2dbc.spi.ConnectionFactoryOptions.PROTOCOL
import io.r2dbc.spi.ConnectionFactoryOptions.USER
import io.r2dbc.spi.Option
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Import
import java.net.URI


@TezosConfiguration
@Import(value = [CoreConfiguration::class, DipDupApiConfiguration::class, DipDupDummyApiConfiguration::class])
@ComponentScan(basePackageClasses = [TezosOrderConverter::class])
@EnableConfigurationProperties(value = [TezosIntegrationProperties::class, PGIntegrationProperties::class])
class TezosApiConfiguration(
    private val properties: TezosIntegrationProperties,
    private val pgProperties: PGIntegrationProperties
) {

    @Bean
    fun tezosBlockchain(): BlockchainDto {
        return BlockchainDto.TEZOS
    }

    @Bean
    fun connectionFactory(): ConnectionFactory {
        return ConnectionFactories.get(
            ConnectionFactoryOptions.builder()
                .option(DRIVER, "pool")
                .option(PROTOCOL, "postgresql")
                .option(HOST, pgProperties.host)
                .option(PORT, pgProperties.port)
                .option(USER, pgProperties.user)
                .option(PASSWORD, pgProperties.password)
                .option(DATABASE, pgProperties.database)
                .option(Option.valueOf("initialSize"), pgProperties.poolSize.toString())
                .option(Option.valueOf("maxSize"), pgProperties.poolSize.toString())
                .build()
        )
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
    fun tezosOwnershipService(controllerApi: NftOwnershipControllerApi, tzktOwnershipService: TzktOwnershipService): TezosOwnershipService {
        return TezosOwnershipService(controllerApi, tzktOwnershipService)
    }

    @Bean
    fun tezosCollectionService(
        controllerApi: NftCollectionControllerApi,
        tezosPgCollectionService: TezosPgCollectionService,
        tzktCollectionService: TzktCollectionService
    ): TezosCollectionService {
        return TezosCollectionService(controllerApi, tezosPgCollectionService, tzktCollectionService)
    }

    @Bean
    fun tezosOrderService(
        controllerApi: OrderControllerApi,
        converter: TezosOrderConverter,
        dipdupOrderService: DipdupOrderService
    ): OrderService {
        return OrderProxyService(
            TezosOrderService(controllerApi, converter, dipdupOrderService),
            setOf(PlatformDto.RARIBLE)
        )
    }

    @Bean
    fun tezosAuctionService(): AuctionService {
        return TezosAuctionService(BlockchainDto.TEZOS)
    }

    @Bean
    fun tezosSignatureService(controllerApi: OrderSignatureControllerApi): TezosSignatureService {
        return TezosSignatureService(controllerApi)
    }

    @Bean
    fun tezosPgActivityService(connectionFactory: ConnectionFactory): TezosPgActivityService {
        return TezosPgActivityService(connectionFactory)
    }

    @Bean
    fun tezosPgCollectionService(mapper: ObjectMapper, connectionFactory: ConnectionFactory): TezosPgCollectionService {
        return TezosPgCollectionService(mapper, connectionFactory)
    }

    @Bean
    fun tezosActivityService(
        itemActivityApi: NftActivityControllerApi,
        orderActivityApi: OrderActivityControllerApi,
        converter: TezosActivityConverter,
        pgActivityService: TezosPgActivityService,
        dipdupOrderActivityService: DipdupOrderActivityService
    ): TezosActivityService {
        return TezosActivityService(itemActivityApi, orderActivityApi, converter, pgActivityService, dipdupOrderActivityService)
    }
}
