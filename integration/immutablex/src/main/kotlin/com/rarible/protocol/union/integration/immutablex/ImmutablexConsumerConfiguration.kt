package com.rarible.protocol.union.integration.immutablex

import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionItemEvent
import com.rarible.protocol.union.core.model.UnionOrderEvent
import com.rarible.protocol.union.core.model.UnionOwnershipEvent
import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.integration.immutablex.client.EventsApi
import com.rarible.protocol.union.integration.immutablex.converter.ImmutablexEventConverter
import com.rarible.protocol.union.integration.immutablex.converter.ImmutablexOrderConverter
import com.rarible.protocol.union.integration.immutablex.handlers.ImmutablexActivityEventHandler
import com.rarible.protocol.union.integration.immutablex.handlers.ImmutablexItemEventHandler
import com.rarible.protocol.union.integration.immutablex.handlers.ImmutablexOrderEventHandler
import com.rarible.protocol.union.integration.immutablex.handlers.ImmutablexOwnershipEventHandler
import com.rarible.protocol.union.integration.immutablex.service.ImmutablexOrderService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.data.mongodb.core.MongoTemplate

@ImmutablexConfiguration
@Import(ImmutablexApiConfiguration::class)
class ImmutablexConsumerConfiguration {

    @Bean
    fun immutablexActivityEventHandler(
        handler: IncomingEventHandler<ActivityDto>,
        orderService: ImmutablexOrderService,
    ) = ImmutablexActivityEventHandler(handler, ImmutablexEventConverter(orderService))

    @Bean
    fun immutablexItemEventHandler(handler: IncomingEventHandler<UnionItemEvent>) = ImmutablexItemEventHandler(handler)

    @Bean
    fun immutablexOwnershipEventHandler(handler: IncomingEventHandler<UnionOwnershipEvent>) =
        ImmutablexOwnershipEventHandler(handler)

    @Bean
    fun immutablexOrderEventHandler(
        handler: IncomingEventHandler<UnionOrderEvent>,
        converter: ImmutablexOrderConverter,
    ) = ImmutablexOrderEventHandler(handler, converter)

    @Bean
    fun immutablexScanner(
        eventsApi: EventsApi,
        mongo: MongoTemplate,
        activityHandler: ImmutablexActivityEventHandler,
        itemEventHandler: ImmutablexItemEventHandler,
        ownershipEventHandler: ImmutablexOwnershipEventHandler,
        orderEventHandler: ImmutablexOrderEventHandler,
    ): ImmutablexScanner =
        ImmutablexScanner(eventsApi, mongo, activityHandler, ownershipEventHandler, itemEventHandler, orderEventHandler)

}
