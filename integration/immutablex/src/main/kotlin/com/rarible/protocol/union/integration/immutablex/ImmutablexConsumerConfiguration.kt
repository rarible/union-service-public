package com.rarible.protocol.union.integration.immutablex

import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionItemEvent
import com.rarible.protocol.union.core.model.UnionOrderEvent
import com.rarible.protocol.union.core.model.UnionOwnershipEvent
import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.integration.immutablex.handlers.ImmutablexActivityEventHandler
import com.rarible.protocol.union.integration.immutablex.handlers.ImmutablexItemEventHandler
import com.rarible.protocol.union.integration.immutablex.handlers.ImmutablexOrderEventHandler
import com.rarible.protocol.union.integration.immutablex.handlers.ImmutablexOwnershipEventHandler
import com.rarible.protocol.union.integration.immutablex.scanner.ImxEventsApi
import com.rarible.protocol.union.integration.immutablex.scanner.ImxScanStateRepository
import com.rarible.protocol.union.integration.immutablex.scanner.ImxScanner
import com.rarible.protocol.union.integration.immutablex.service.ImmutablexActivityService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.data.mongodb.config.EnableMongoAuditing
import org.springframework.data.mongodb.core.ReactiveMongoTemplate

@ImmutablexConfiguration
@Import(ImmutablexApiConfiguration::class)
@EnableMongoAuditing
class ImmutablexConsumerConfiguration {

    @Bean
    fun immutablexActivityEventHandler(
        handler: IncomingEventHandler<ActivityDto>,
        activityService: ImmutablexActivityService
    ) = ImmutablexActivityEventHandler(handler, activityService)

    @Bean
    fun immutablexItemEventHandler(
        handler: IncomingEventHandler<UnionItemEvent>
    ) = ImmutablexItemEventHandler(handler)

    @Bean
    fun immutablexOwnershipEventHandler(
        handler: IncomingEventHandler<UnionOwnershipEvent>
    ) = ImmutablexOwnershipEventHandler(handler)

    @Bean
    fun immutablexOrderEventHandler(
        handler: IncomingEventHandler<UnionOrderEvent>
    ) = ImmutablexOrderEventHandler(handler)

    @Bean
    fun imxScanStateRepository(mongo: ReactiveMongoTemplate): ImxScanStateRepository {
        return ImxScanStateRepository(mongo)
    }

    @Bean
    fun immutablexScanner(
        eventsApi: ImxEventsApi,
        scanStateRepository: ImxScanStateRepository,
        activityHandler: ImmutablexActivityEventHandler,
        itemEventHandler: ImmutablexItemEventHandler,
        ownershipEventHandler: ImmutablexOwnershipEventHandler,
        orderEventHandler: ImmutablexOrderEventHandler,
    ): ImxScanner = ImxScanner(
        eventsApi, scanStateRepository, activityHandler, ownershipEventHandler, itemEventHandler, orderEventHandler
    )

}
