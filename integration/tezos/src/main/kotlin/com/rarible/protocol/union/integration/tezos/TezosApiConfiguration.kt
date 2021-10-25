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
import com.rarible.protocol.union.core.service.OrderService
import com.rarible.protocol.union.core.service.router.OrderProxyService
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.integration.tezos.converter.TezosActivityConverter
import com.rarible.protocol.union.integration.tezos.converter.TezosOrderConverter
import com.rarible.protocol.union.integration.tezos.service.TezosActivityService
import com.rarible.protocol.union.integration.tezos.service.TezosCollectionService
import com.rarible.protocol.union.integration.tezos.service.TezosItemService
import com.rarible.protocol.union.integration.tezos.service.TezosOrderService
import com.rarible.protocol.union.integration.tezos.service.TezosOwnershipService
import com.rarible.protocol.union.integration.tezos.service.TezosSignatureService
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Import
import java.net.URI

@TezosConfiguration
@Import(CoreConfiguration::class)
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
    fun tezosItemService(controllerApi: NftItemControllerApi): TezosItemService {
        return TezosItemService(controllerApi)
    }

    @Bean
    fun tezosOwnershipService(controllerApi: NftOwnershipControllerApi): TezosOwnershipService {
        return TezosOwnershipService(controllerApi)
    }

    @Bean
    fun tezosCollectionService(controllerApi: NftCollectionControllerApi): TezosCollectionService {
        return TezosCollectionService(controllerApi)
    }

    @Bean
    fun tezosOrderService(
        controllerApi: OrderControllerApi,
        converter: TezosOrderConverter
    ): OrderService {
        return OrderProxyService(
            TezosOrderService(controllerApi, converter),
            setOf(PlatformDto.RARIBLE)
        )
    }

    @Bean
    fun tezosSignatureService(controllerApi: OrderSignatureControllerApi): TezosSignatureService {
        return TezosSignatureService(controllerApi)
    }

    @Bean
    fun tezosActivityService(
        itemActivityApi: NftActivityControllerApi,
        orderActivityApi: OrderActivityControllerApi,
        converter: TezosActivityConverter
    ): TezosActivityService {
        return TezosActivityService(itemActivityApi, orderActivityApi, converter)
    }
}