package com.rarible.protocol.union.integration.solana.service

import com.rarible.core.apm.CaptureSpan
import com.rarible.protocol.solana.api.client.BalanceControllerApi
import com.rarible.protocol.union.core.model.UnionOwnership
import com.rarible.protocol.union.core.service.OwnershipService
import com.rarible.protocol.union.core.service.router.AbstractBlockchainService
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.continuation.page.Page
import com.rarible.protocol.union.integration.solana.converter.SolanaOwnershipConverter
import kotlinx.coroutines.reactive.awaitFirst

@CaptureSpan(type = "blockchain")
open class SolanaOwnershipService(
    private val balanceApi: BalanceControllerApi
) : AbstractBlockchainService(BlockchainDto.SOLANA), OwnershipService {

    override suspend fun getOwnershipById(ownershipId: String): UnionOwnership {
        val balance = balanceApi.getBalanceByAccount(ownershipId).awaitFirst()
        return SolanaOwnershipConverter.convert(balance, blockchain)
    }

    // TODO add continuation
    override suspend fun getOwnershipsByItem(itemId: String, continuation: String?, size: Int): Page<UnionOwnership> {
        val balancesDto = balanceApi.getBalanceByMint(
            itemId,
            continuation,
            size
        ).awaitFirst()
        val ownerships = balancesDto.balances.map { balance ->
            SolanaOwnershipConverter.convert(balance, blockchain)
        }

        return Page(0, balancesDto.continuation, ownerships)
    }

    override suspend fun getOwnershipsByOwner(address: String, continuation: String?, size: Int): Page<UnionOwnership> {
        val balancesDto = balanceApi.getBalanceByOwner(
            address,
            continuation,
            size
        ).awaitFirst()
        val ownerships = balancesDto.balances.map { balance ->
            SolanaOwnershipConverter.convert(balance, blockchain)
        }

        return Page(0, balancesDto.continuation, ownerships)
    }
}
