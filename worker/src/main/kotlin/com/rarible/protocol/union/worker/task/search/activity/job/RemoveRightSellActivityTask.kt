package com.rarible.protocol.union.worker.task.search.activity.job

import com.rarible.core.logging.Logger
import com.rarible.core.task.TaskHandler
import com.rarible.protocol.order.api.client.OrderActivityControllerApi
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.enrichment.repository.search.EsActivityRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

@Component
class RemoveRightSellActivityTask(
    @Qualifier("ethereum.activity.api.order") private val ethereumOrderActivityApi: OrderActivityControllerApi,
    @Qualifier("polygon.activity.api.order") private val polygonOrderActivityApi: OrderActivityControllerApi,
    private val esActivityRepository: EsActivityRepository,
) : TaskHandler<String> {

    override val type: String
        get() = REMOVE_RIGHT_SELL_ACTIVITY_TASK

    override fun runLongTask(from: String?, param: String): Flow<String> = flow {
        logger.info("Start REMOVE_RIGHT_SELL_ACTIVITY_TASK")

        val size = 200
        var continuation = from
        var blockchainDto = if (param.isNullOrBlank()) BlockchainDto.ETHEREUM else BlockchainDto.valueOf(param)
        do {
            val orderSellRightActivities = getClient(blockchainDto).getOrderSellRightActivities(continuation, size)
                .awaitSingle()

            val ids = orderSellRightActivities.items.map { it }
            logger.info("Remove activities $ids")
            esActivityRepository.deleteAll(orderSellRightActivities.items.map { it })
            continuation = orderSellRightActivities.continuation
            if (continuation != null) {
                emit(continuation)
            }
        } while (continuation.isNullOrEmpty().not())

        logger.info("Finish REMOVE_RIGHT_SELL_ACTIVITY_TASK")
    }

    private fun getClient(blockchainDto: BlockchainDto): OrderActivityControllerApi =
        if (blockchainDto == BlockchainDto.POLYGON) {
            polygonOrderActivityApi
        } else {
            ethereumOrderActivityApi
        }
    companion object {
        const val REMOVE_RIGHT_SELL_ACTIVITY_TASK = "REMOVE_RIGHT_SELL_ACTIVITY_TASK"
        val logger by Logger()
    }
}