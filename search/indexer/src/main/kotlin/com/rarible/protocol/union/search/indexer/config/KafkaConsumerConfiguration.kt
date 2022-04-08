package com.rarible.protocol.union.search.indexer.config

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.daemon.RetryProperties
import com.rarible.core.daemon.sequential.ConsumerBatchEventHandler
import com.rarible.core.daemon.sequential.ConsumerBatchWorker
import com.rarible.core.daemon.sequential.ConsumerWorkerHolder
import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.dto.OrderEventDto
import com.rarible.protocol.union.subscriber.UnionEventsConsumerFactory
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration

@Configuration
class KafkaConsumerConfiguration(
    applicationEnvironmentInfo: ApplicationEnvironmentInfo,
    private val kafkaProperties: KafkaProperties,
    private val meterRegistry: MeterRegistry,
) {
    companion object {
        const val ACTIVITY = "activity"
        const val ORDER = "order"
    }

    private val env = applicationEnvironmentInfo.name
    private val host = applicationEnvironmentInfo.host

    private val consumerFactory = UnionEventsConsumerFactory(kafkaProperties.brokerReplicaSet, host, env)

    @Bean
    fun activityWorker(
        handler: ConsumerBatchEventHandler<ActivityDto>
    ): ConsumerWorkerHolder<ActivityDto> {
        val workers = (1..kafkaProperties.workerCount).map { index ->
            val consumer = consumerFactory.createActivityConsumer(consumerGroup(ACTIVITY))
            ConsumerBatchWorker(
                consumer = consumer,
                eventHandler = handler,
                workerName = worker(ACTIVITY, index),
                properties = kafkaProperties.daemon,
                retryProperties = RetryProperties(attempts = Integer.MAX_VALUE, delay = Duration.ofMillis(1000)),
                meterRegistry = meterRegistry,
            )
        }
        return ConsumerWorkerHolder(workers)
    }

    @Bean
    fun orderWorker(
        handler: ConsumerBatchEventHandler<OrderEventDto>
    ): ConsumerWorkerHolder<OrderEventDto> {
        val workers = (1..kafkaProperties.workerCount).map { index ->
            val consumer = consumerFactory.createOrderConsumer(consumerGroup(ORDER))
            ConsumerBatchWorker(
                consumer = consumer,
                eventHandler = handler,
                workerName = worker(ORDER, index),
                properties = kafkaProperties.daemon,
                retryProperties = RetryProperties(attempts = Integer.MAX_VALUE, delay = Duration.ofMillis(1000)),
                meterRegistry = meterRegistry,
            )
        }
        return ConsumerWorkerHolder(workers)
    }

    private fun consumerGroup(suffix: String): String {
        return "$env.protocol.union.search.$suffix"
    }

    private fun worker(suffix: String, index: Int): String {
        return "$env.$suffix.worker-$index"
    }
}
