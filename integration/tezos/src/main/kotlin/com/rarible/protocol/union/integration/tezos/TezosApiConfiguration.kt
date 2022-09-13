package com.rarible.protocol.union.integration.tezos

import com.rarible.protocol.union.core.CoreConfiguration
import com.rarible.protocol.union.core.service.AuctionService
import com.rarible.protocol.union.core.service.OrderService
import com.rarible.protocol.union.core.service.router.OrderProxyService
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.integration.tezos.dipdup.DipDupApiConfiguration
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
    fun tezosItemService(tzktItemService: TzktItemService): TezosItemService {
        return TezosItemService(tzktItemService)
    }

    @Bean
    fun tezosOwnershipService(
        tzktOwnershipService: TzktOwnershipService
    ): TezosOwnershipService {
        return TezosOwnershipService(tzktOwnershipService)
    }

    @Bean
    fun tezosCollectionService(
        tzktCollectionService: TzktCollectionService,
        tezosCollectionRepository: TezosCollectionRepository,
    ): TezosCollectionService {
        return TezosCollectionService(tzktCollectionService, tezosCollectionRepository)
    }

    @Bean
    fun tezosOrderService(
        dipdupOrderService: DipdupOrderService,
        tezosIntegrationProperties: TezosIntegrationProperties
    ): OrderService {
        return OrderProxyService(
            TezosOrderService(dipdupOrderService),
            setOf(PlatformDto.RARIBLE)
        )
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
        dipdupOrderActivityService: DipdupOrderActivityService,
        tzktItemActivityService: TzktItemActivityService
    ): TezosActivityService {
        return TezosActivityService(
            dipdupOrderActivityService,
            tzktItemActivityService
        )
    }
}
