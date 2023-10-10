package com.rarible.protocol.union.integration.ethereum.service

import com.rarible.protocol.erc20.api.client.BalanceControllerApi
import com.rarible.protocol.union.core.model.UnionBalance
import com.rarible.protocol.union.core.service.BalanceService
import com.rarible.protocol.union.core.service.router.AbstractBlockchainService
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.integration.ethereum.converter.EthBalanceConverter
import kotlinx.coroutines.reactor.awaitSingle

open class EthBalanceService(
    blockchain: BlockchainDto,
    private val balanceControllerApi: BalanceControllerApi
) : AbstractBlockchainService(blockchain), BalanceService {

    override suspend fun getBalance(currencyId: String, owner: String): UnionBalance {
        return if (currencyId == EthBalanceConverter.NATIVE_CURRENCY_CONTRACT) {
            val ethBalance = balanceControllerApi.getEthBalance(owner).awaitSingle()
            EthBalanceConverter.convert(ethBalance, blockchain)
        } else {
            val ethBalance = balanceControllerApi.getErc20Balance(currencyId, owner).awaitSingle()
            EthBalanceConverter.convert(ethBalance, blockchain)
        }
    }
}
