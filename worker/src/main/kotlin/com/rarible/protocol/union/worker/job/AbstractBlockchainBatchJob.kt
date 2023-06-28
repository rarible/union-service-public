package com.rarible.protocol.union.worker.job

import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.parser.IdParser

abstract class AbstractBlockchainBatchJob : AbstractBatchJob() {

    override suspend fun handleBatch(continuation: String?, param: String) =
        handleBatch(continuation, IdParser.parseBlockchain(param))

    abstract suspend fun handleBatch(continuation: String?, blockchain: BlockchainDto): String?
}