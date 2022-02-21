package com.rarible.protocol.union.integration.solana.service

import com.rarible.protocol.union.core.model.UnionOwnership
import com.rarible.protocol.union.core.service.OwnershipService
import com.rarible.protocol.union.core.service.router.AbstractBlockchainService
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.continuation.page.Page
import com.rarible.protocol.union.integration.solana.converter.SolanaBalanceConverter
import com.rarible.solana.protocol.api.client.BalanceControllerApi
import kotlinx.coroutines.reactive.awaitFirst


open class SolanaOwnershipService(
    private val balanceApi: BalanceControllerApi
) : AbstractBlockchainService(BlockchainDto.SOLANA), OwnershipService {
    override suspend fun getOwnershipById(ownershipId: String): UnionOwnership {
        val balance = balanceApi.getBalanceByAccount(ownershipId).awaitFirst()

        return SolanaBalanceConverter.convert(balance)
    }

    // TODO add continuation
    override suspend fun getOwnershipsByItem(itemId: String, continuation: String?, size: Int): Page<UnionOwnership> {
        val balancesDto = balanceApi.getBalanceByMint(itemId).awaitFirst()
        val ownerships = balancesDto.balances.map { balance -> SolanaBalanceConverter.convert(balance) }

        return Page(balancesDto.total, null, ownerships)
    }

    override suspend fun getOwnershipsByOwner(address: String, continuation: String?, size: Int): Page<UnionOwnership> {
        val balancesDto = balanceApi.getBalanceByOwner(address).awaitFirst()
        val ownerships = balancesDto.balances.map { balance -> SolanaBalanceConverter.convert(balance) }

        return Page(balancesDto.total, null, ownerships)
    }
}