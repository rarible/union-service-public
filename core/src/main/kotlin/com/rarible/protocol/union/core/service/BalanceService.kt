package com.rarible.protocol.union.core.service

import com.rarible.protocol.union.core.model.UnionBalance
import com.rarible.protocol.union.core.service.router.BlockchainService

interface BalanceService : BlockchainService {

    suspend fun getBalance(
        currencyId: String,
        owner: String
    ): UnionBalance
}
