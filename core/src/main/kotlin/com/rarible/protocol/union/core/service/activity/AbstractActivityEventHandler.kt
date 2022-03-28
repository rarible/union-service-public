package com.rarible.service.activity

import com.rarible.core.daemon.sequential.ConsumerEventHandler
import com.rarible.protocol.union.dto.ActivityDto
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import java.time.Instant

abstract class AbstractActivityEventHandler : ConsumerEventHandler<ActivityDto> {
    @Value("\${rarible.common.oldActivityPeriodMillis}")
    private val oldActivityPeriodMillis: Long = 43200000
    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun handle(event: ActivityDto) {
        if (event.reverted == true) {
            logger.info("Ignoring event: ${event.id} because reverted")
            return
        }
        if (event.date.plusMillis(oldActivityPeriodMillis).isBefore(Instant.now())) {
            logger.info("Ignoring event: ${event.id} because too old: ${event.date}")
            return
        }

        handleActivity(event)
    }

    abstract suspend fun handleActivity(event: ActivityDto)
}
