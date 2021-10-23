package com.rarible.protocol.union.core.tezos

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
import com.rarible.protocol.union.core.IntegrationProperties
import com.rarible.protocol.union.core.service.ActivityService
import com.rarible.protocol.union.core.service.CollectionService
import com.rarible.protocol.union.core.service.ItemService
import com.rarible.protocol.union.core.service.OrderService
import com.rarible.protocol.union.core.service.OwnershipService
import com.rarible.protocol.union.core.service.SignatureService
import com.rarible.protocol.union.core.service.router.OrderProxyService
import com.rarible.protocol.union.core.tezos.converter.TezosActivityConverter
import com.rarible.protocol.union.core.tezos.converter.TezosOrderConverter
import com.rarible.protocol.union.core.tezos.service.TezosActivityService
import com.rarible.protocol.union.core.tezos.service.TezosCollectionService
import com.rarible.protocol.union.core.tezos.service.TezosItemService
import com.rarible.protocol.union.core.tezos.service.TezosOrderService
import com.rarible.protocol.union.core.tezos.service.TezosOwnershipService
import com.rarible.protocol.union.core.tezos.service.TezosSignatureService
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.PlatformDto
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.net.URI

@Configuration
class TezosConfiguration(
    private val integrations: IntegrationProperties
) {

    @Bean
    fun tezosApiServiceUriProvider(): TezosApiServiceUriProvider {
        return FixedTezosApiServiceUriProvider(URI(integrations.get(BlockchainDto.TEZOS).client!!.url!!))
    }

    //--------------------- TEZOS API ---------------------//
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

    //--------------------- TEZOS SERVICES --------------------//
    @Bean
    fun tezosItemService(tezosItemApi: NftItemControllerApi): ItemService {
        return TezosItemService(BlockchainDto.TEZOS, tezosItemApi)
    }

    @Bean
    fun tezosOwnershipService(tezosOwnershipApi: NftOwnershipControllerApi): OwnershipService {
        return TezosOwnershipService(BlockchainDto.TEZOS, tezosOwnershipApi)
    }

    @Bean
    fun tezosCollectionService(tezosCollectionApi: NftCollectionControllerApi): CollectionService {
        return TezosCollectionService(BlockchainDto.TEZOS, tezosCollectionApi)
    }

    @Bean
    fun tezosOrderService(tezosOrderApi: OrderControllerApi, tezosOrderConverter: TezosOrderConverter): OrderService {
        return OrderProxyService(
            TezosOrderService(BlockchainDto.TEZOS, tezosOrderApi, tezosOrderConverter),
            setOf(PlatformDto.RARIBLE)
        )
    }

    @Bean
    fun tezosActivityService(
        tezosActivityItemApi: NftActivityControllerApi,
        tezosActivityOrderApi: OrderActivityControllerApi,
        tezosActivityConverter: TezosActivityConverter
    ): ActivityService {
        return TezosActivityService(
            BlockchainDto.TEZOS,
            tezosActivityItemApi,
            tezosActivityOrderApi,
            tezosActivityConverter
        )
    }

    @Bean
    fun tezosSignatureService(
        tezosOrderApi: OrderControllerApi,
        signatureControllerApi: OrderSignatureControllerApi
    ): SignatureService {
        return TezosSignatureService(BlockchainDto.TEZOS, signatureControllerApi)
    }
}