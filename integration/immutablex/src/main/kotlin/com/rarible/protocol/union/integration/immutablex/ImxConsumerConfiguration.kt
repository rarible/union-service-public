package com.rarible.protocol.union.integration.immutablex

import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionCollectionEvent
import com.rarible.protocol.union.core.model.UnionItemEvent
import com.rarible.protocol.union.core.model.UnionItemMetaEvent
import com.rarible.protocol.union.core.model.UnionOrderEvent
import com.rarible.protocol.union.core.model.UnionOwnershipEvent
import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.integration.immutablex.handlers.ImxActivityEventHandler
import com.rarible.protocol.union.integration.immutablex.handlers.ImxCollectionEventHandler
import com.rarible.protocol.union.integration.immutablex.handlers.ImxItemEventHandler
import com.rarible.protocol.union.integration.immutablex.handlers.ImxOrderEventHandler
import com.rarible.protocol.union.integration.immutablex.scanner.ImxEventsApi
import com.rarible.protocol.union.integration.immutablex.scanner.ImxScanMetrics
import com.rarible.protocol.union.integration.immutablex.scanner.ImxScanStateRepository
import com.rarible.protocol.union.integration.immutablex.scanner.ImxScanner
import com.rarible.protocol.union.integration.immutablex.service.ImxActivityService
import com.rarible.protocol.union.integration.immutablex.service.ImxItemService
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.data.mongodb.config.EnableMongoAuditing
import org.springframework.data.mongodb.core.ReactiveMongoTemplate

@ImxConfiguration
@Import(ImxApiConfiguration::class)
@EnableMongoAuditing
class ImxConsumerConfiguration {

    @Bean
    fun imxScanMetrics(
        meterRegistry: MeterRegistry
    ) = ImxScanMetrics(meterRegistry)

    @Bean
    fun immutablexActivityEventHandler(
        activityHandler: IncomingEventHandler<ActivityDto>,
        itemHandler: IncomingEventHandler<UnionItemEvent>,
        ownershipHandler: IncomingEventHandler<UnionOwnershipEvent>,
        itemService: ImxItemService,
        activityService: ImxActivityService,
        imxScanMetrics: ImxScanMetrics
    ) = ImxActivityEventHandler(
        activityHandler,
        itemHandler,
        ownershipHandler,
        itemService,
        activityService,
        imxScanMetrics
    )

    @Bean
    fun immutablexItemEventHandler(
        itemMetaHandler: IncomingEventHandler<UnionItemMetaEvent>,
        itemService: ImxItemService
    ) = ImxItemEventHandler(itemMetaHandler, itemService)

    @Bean
    fun imxCollectionEventHandler(
        collectionEventHandler: IncomingEventHandler<UnionCollectionEvent>
    ) = ImxCollectionEventHandler(collectionEventHandler)

    @Bean
    fun immutablexOrderEventHandler(
        handler: IncomingEventHandler<UnionOrderEvent>,
        imxScanMetrics: ImxScanMetrics
    ) = ImxOrderEventHandler(handler, imxScanMetrics)

    @Bean
    fun imxScanStateRepository(mongo: ReactiveMongoTemplate): ImxScanStateRepository {
        return ImxScanStateRepository(mongo)
    }

    @Bean
    fun immutablexScanner(
        eventsApi: ImxEventsApi,
        scanStateRepository: ImxScanStateRepository,
        imxScanMetrics: ImxScanMetrics,
        activityHandler: ImxActivityEventHandler,
        itemEventHandler: ImxItemEventHandler,
        orderEventHandler: ImxOrderEventHandler,
        collectionEventHandler: ImxCollectionEventHandler
    ): ImxScanner = ImxScanner(
        eventsApi,
        scanStateRepository,
        imxScanMetrics,
        activityHandler,
        itemEventHandler,
        orderEventHandler,
        collectionEventHandler
    )

}
