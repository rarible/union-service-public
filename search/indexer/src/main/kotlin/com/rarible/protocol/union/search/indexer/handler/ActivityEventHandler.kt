package com.rarible.protocol.union.search.indexer.handler

import com.rarible.core.daemon.sequential.ConsumerEventHandler
import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.search.core.converter.ElasticActivityConverter
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ActivityEventHandler(
    private val converter: ElasticActivityConverter
): ConsumerEventHandler<ActivityDto> {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun handle(event: ActivityDto) {
        logger.debug("Handling ActivityDto id = ${event.id}")
        val activity = converter.convert(event)
        // TODO save to ElasticSearch
        logger.debug("Handled ActivityDto id = ${event.id}")
    }
}
