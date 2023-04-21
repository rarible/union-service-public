package com.rarible.protocol.union.integration.tezos

import com.rarible.dipdup.client.OrderActivityClient
import com.rarible.dipdup.client.TokenActivityClient
import com.rarible.protocol.union.core.CoreConfiguration
import com.rarible.protocol.union.core.service.AuctionService
import com.rarible.protocol.union.core.service.OrderService
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.integration.tezos.dipdup.DipDupApiConfiguration
import com.rarible.protocol.union.integration.tezos.dipdup.DipDupIntegrationProperties
import com.rarible.protocol.union.integration.tezos.dipdup.converter.DipDupActivityConverter
import com.rarible.protocol.union.integration.tezos.dipdup.service.DipDupCollectionService
import com.rarible.protocol.union.integration.tezos.dipdup.service.DipDupItemService
import com.rarible.protocol.union.integration.tezos.dipdup.service.DipDupOwnershipService
import com.rarible.protocol.union.integration.tezos.dipdup.service.DipDupRoyaltyService
import com.rarible.protocol.union.integration.tezos.dipdup.service.DipDupTokenActivityService
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
import org.springframework.context.annotation.Import
import org.springframework.data.mongodb.core.ReactiveMongoOperations

@TezosConfiguration
@Import(value = [CoreConfiguration::class, DipDupApiConfiguration::class])
@EnableConfigurationProperties(value = [TezosIntegrationProperties::class])
class TezosApiConfiguration {

    @Bean
    fun tezosBlockchain(): BlockchainDto {
        return BlockchainDto.TEZOS
    }

    @Bean
    fun tezosCollectionRepository(mongoTemplate: ReactiveMongoOperations): TezosCollectionRepository {
        return TezosCollectionRepository(mongoTemplate)
    }

    //-------------------- Services --------------------//

    @Bean
    fun tezosItemService(
        tzktItemService: TzktItemService,
        dipdupItemService: DipDupItemService,
        dipDupRoyaltyService: DipDupRoyaltyService,
        properties: DipDupIntegrationProperties
    ): TezosItemService {
        return TezosItemService(tzktItemService, dipdupItemService, dipDupRoyaltyService, properties)
    }

    @Bean
    fun tezosOwnershipService(
        tzktOwnershipService: TzktOwnershipService,
        dipDupOwnershipService: DipDupOwnershipService,
        properties: DipDupIntegrationProperties
    ): TezosOwnershipService {
        return TezosOwnershipService(tzktOwnershipService, dipDupOwnershipService, properties)
    }

    @Bean
    fun tezosCollectionService(
        tzktCollectionService: TzktCollectionService,
        dipdupCollectionService: DipDupCollectionService,
        tezosCollectionRepository: TezosCollectionRepository,
        properties: DipDupIntegrationProperties
    ): TezosCollectionService {
        return TezosCollectionService(tzktCollectionService, dipdupCollectionService, tezosCollectionRepository, properties)
    }

    @Bean
    fun tezosOrderService(
        dipdupOrderService: DipdupOrderService,
        tezosIntegrationProperties: TezosIntegrationProperties
    ): OrderService {
        return TezosOrderService(dipdupOrderService)
    }

    @Bean
    fun tezosAuctionService(): AuctionService {
        return TezosAuctionService(BlockchainDto.TEZOS)
    }

    @Bean
    fun tezosSignatureService(
        tzktSignatureService: TzktSignatureService
    ): TezosSignatureService {
        return TezosSignatureService(tzktSignatureService)
    }

    @Bean
    fun tezosActivityService(
        orderActivityClient: OrderActivityClient,
        dipdupOrderActivityService: DipdupOrderActivityService,
        tokenActivityClient: TokenActivityClient,
        dipdupTokenActivityService: DipDupTokenActivityService,
        dipDupActivityConverter: DipDupActivityConverter,
        tzktItemActivityService: TzktItemActivityService,
        properties: DipDupIntegrationProperties
    ): TezosActivityService {
        return TezosActivityService(
            orderActivityClient,
            dipdupOrderActivityService,
            tokenActivityClient,
            dipdupTokenActivityService,
            tzktItemActivityService,
            dipDupActivityConverter,
            properties
        )
    }
}
