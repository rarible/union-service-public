package com.rarible.protocol.union.integration.immutablex

import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionActivityDto
import com.rarible.protocol.union.core.model.UnionCollectionEvent
import com.rarible.protocol.union.core.model.UnionItemEvent
import com.rarible.protocol.union.core.model.UnionItemMetaEvent
import com.rarible.protocol.union.core.model.UnionOrderEvent
import com.rarible.protocol.union.core.model.UnionOwnershipEvent
import com.rarible.protocol.union.integration.immutablex.converter.ImxActivityConverter
import com.rarible.protocol.union.integration.immutablex.handlers.ImxActivityEventHandler
import com.rarible.protocol.union.integration.immutablex.handlers.ImxCollectionEventHandler
import com.rarible.protocol.union.integration.immutablex.handlers.ImxItemEventHandler
import com.rarible.protocol.union.integration.immutablex.handlers.ImxOrderEventHandler
import com.rarible.protocol.union.integration.immutablex.repository.ImxCollectionCreatorRepository
import com.rarible.protocol.union.integration.immutablex.repository.ImxItemMetaRepository
import com.rarible.protocol.union.integration.immutablex.repository.ImxScanStateRepository
import com.rarible.protocol.union.integration.immutablex.scanner.ImxEventsApi
import com.rarible.protocol.union.integration.immutablex.scanner.ImxScanMetrics
import com.rarible.protocol.union.integration.immutablex.scanner.ImxScanner
import com.rarible.protocol.union.integration.immutablex.service.ImxActivityService
import com.rarible.protocol.union.integration.immutablex.service.ImxItemService
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.beans.factory.annotation.Value
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
        activityHandler: IncomingEventHandler<UnionActivityDto>,
        itemHandler: IncomingEventHandler<UnionItemEvent>,
        ownershipHandler: IncomingEventHandler<UnionOwnershipEvent>,
        itemService: ImxItemService,
        activityService: ImxActivityService,
        imxScanMetrics: ImxScanMetrics,
        converter: ImxActivityConverter
    ) = ImxActivityEventHandler(
        activityHandler,
        itemHandler,
        ownershipHandler,
        itemService,
        activityService,
        imxScanMetrics,
        converter
    )

    @Bean
    fun immutablexItemEventHandler(
        itemMetaHandler: IncomingEventHandler<UnionItemMetaEvent>,
        itemHandler: IncomingEventHandler<UnionItemEvent>,
        itemService: ImxItemService,
        itemMetaRepository: ImxItemMetaRepository
    ) = ImxItemEventHandler(itemMetaHandler, itemHandler, itemService, itemMetaRepository)

    @Bean
    fun imxCollectionEventHandler(
        collectionEventHandler: IncomingEventHandler<UnionCollectionEvent>,
        collectionCreatorRepository: ImxCollectionCreatorRepository
    ) = ImxCollectionEventHandler(collectionEventHandler, collectionCreatorRepository)

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
        collectionEventHandler: ImxCollectionEventHandler,
        @Value("\${integration.immutablex.scanner.job.offset.activity:3000}")
        activityDelay: Long
    ): ImxScanner = ImxScanner(
        eventsApi,
        scanStateRepository,
        imxScanMetrics,
        activityHandler,
        itemEventHandler,
        orderEventHandler,
        collectionEventHandler,
        activityDelay
    )

}
