package com.rarible.protocol.union.worker.job.collection

import com.rarible.protocol.union.core.event.KafkaEventFactory
import com.rarible.protocol.union.core.model.UnionItem
import com.rarible.protocol.union.core.model.UnionItemChangeEvent
import com.rarible.protocol.union.core.model.offchainEventMark
import com.rarible.protocol.union.core.producer.UnionInternalBlockchainEventProducer
import org.springframework.stereotype.Component

@Component
class CustomCollectionJob(
    private val eventProducer: UnionInternalBlockchainEventProducer,
    private val ruleHandlerProvider: CustomCollectionItemFetcherProvider
) {

    private val batchSize = 50

    /**
     * @param name identifier of rule set in the configuration
     * @param continuation last state of the migration (format depends on the rule type)
     */
    suspend fun migrate(name: String, continuation: String?): String? {
        val handlers = ruleHandlerProvider.get(name)
        val state = continuation?.let { CustomCollectionJobState(it) }
        var currentRule = state?.rule ?: 0
        var currentState = state?.state
        while (currentRule < handlers.size) {
            val handler = handlers[currentRule]
            val next = handler.next(currentState, batchSize)
            if (next.state != null) {
                notify(next.items)
                return CustomCollectionJobState(currentRule, next.state).toString()
            }
            currentRule++
            currentState = null
        }
        return null
    }

    // Here we need just to send events, collections will be replaced in listener
    suspend fun notify(items: List<UnionItem>) {
        // TODO ideally, there should be check - if collection already substituted, but it is possible
        // only if we have items in Union
        items.forEach {
            val itemId = it.id
            val eventTimeMarks = offchainEventMark("enrichment-in")
            val message = KafkaEventFactory.internalItemEvent(UnionItemChangeEvent(itemId, eventTimeMarks))
            eventProducer.getProducer(itemId.blockchain).send(message)
        }
    }

}



