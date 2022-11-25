package com.rarible.protocol.union.worker.job

import com.rarible.protocol.union.dto.BlockchainDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

abstract class AbstractReconciliationJob {

    fun reconcile(continuation: String?, blockchain: BlockchainDto): Flow<String> {
        return flow {
            var next = continuation
            do {
                next = reconcileBatch(next, blockchain)
                if (next != null) {
                    emit(next)
                }
            } while (next != null)
        }
    }

    abstract suspend fun reconcileBatch(continuation: String?, blockchain: BlockchainDto): String?

}