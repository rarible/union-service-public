package com.rarible.protocol.union.core.service.dummy

import com.rarible.protocol.union.core.exception.UnionNotFoundException
import com.rarible.protocol.union.core.model.UnionBalance
import com.rarible.protocol.union.core.service.BalanceService
import com.rarible.protocol.union.core.service.router.AbstractBlockchainService
import com.rarible.protocol.union.dto.BlockchainDto

class DummyBalanceService(
    blockchain: BlockchainDto
) : AbstractBlockchainService(blockchain), BalanceService {

    override suspend fun getBalance(currencyId: String, owner: String): UnionBalance {
        throw UnionNotFoundException(
            "Balance [$currencyId:$owner] not found," +
                " operation is not supported for ${blockchain.name}"
        )
    }
}
