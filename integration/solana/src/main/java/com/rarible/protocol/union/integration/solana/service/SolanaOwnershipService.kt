package com.rarible.protocol.union.integration.solana.service

import com.rarible.core.apm.CaptureSpan
import com.rarible.protocol.solana.api.client.BalanceControllerApi
import com.rarible.protocol.union.core.model.UnionOwnership
import com.rarible.protocol.union.core.service.OwnershipService
import com.rarible.protocol.union.core.service.router.AbstractBlockchainService
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.continuation.page.Page
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.integration.solana.converter.SolanaOwnershipConverter
import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.WebClientResponseException

@CaptureSpan(type = "blockchain")
open class SolanaOwnershipService(
    private val balanceApi: BalanceControllerApi
) : AbstractBlockchainService(BlockchainDto.SOLANA), OwnershipService {

    override suspend fun getOwnershipById(ownershipId: String): UnionOwnership {
        val pair = IdParser.split(ownershipId, 2)
        val mint = pair[0]
        val owner = pair[1]

        val balancesDto = balanceApi.getBalancesByMintAndOwner(mint, owner).awaitFirst()
        val associatedTokenAccountBalance = balancesDto.balances.find { it.isAssociatedTokenAccount == true }
            ?: throw createBalanceNotFoundException("No associated token account balance found for $ownershipId")
        return SolanaOwnershipConverter.convert(associatedTokenAccountBalance, blockchain)
    }

    override suspend fun getOwnershipsByItem(itemId: String, continuation: String?, size: Int): Page<UnionOwnership> {
        val balancesDto = balanceApi.getBalanceByMint(
            itemId,
            continuation,
            size
        ).awaitFirst()
        val balances = balancesDto.balances.filter { it.isAssociatedTokenAccount == true }
        val ownerships = balances.map { balance ->
            SolanaOwnershipConverter.convert(balance, blockchain)
        }

        return Page(
            total = balancesDto.balances.size.toLong(),
            continuation = balancesDto.continuation,
            entities = ownerships
        )
    }

    override suspend fun getOwnershipsByOwner(address: String, continuation: String?, size: Int): Page<UnionOwnership> {
        val balancesDto = balanceApi.getBalanceByOwner(
            address,
            continuation,
            size
        ).awaitFirst()
        val balances = balancesDto.balances.filter { it.isAssociatedTokenAccount == true }
        val ownerships = balances.map { balance ->
            SolanaOwnershipConverter.convert(balance, blockchain)
        }

        return Page(
            total = balancesDto.balances.size.toLong(),
            continuation = balancesDto.continuation,
            entities = ownerships
        )
    }

    private fun createBalanceNotFoundException(message: String) = WebClientResponseException(
        HttpStatus.NOT_FOUND.value(),
        message,
        null, null, null, null
    )
}
