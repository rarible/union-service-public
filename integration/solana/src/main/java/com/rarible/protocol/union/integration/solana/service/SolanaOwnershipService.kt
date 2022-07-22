package com.rarible.protocol.union.integration.solana.service

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.common.mapAsync
import com.rarible.protocol.solana.api.client.BalanceControllerApi
import com.rarible.protocol.union.core.model.UnionOwnership
import com.rarible.protocol.union.core.service.OwnershipService
import com.rarible.protocol.union.core.service.router.AbstractBlockchainService
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.continuation.page.Page
import com.rarible.protocol.union.dto.continuation.page.Slice
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.integration.solana.converter.SolanaOwnershipConverter
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.WebClientResponseException

@CaptureSpan(type = "blockchain")
open class SolanaOwnershipService(
    private val balanceApi: BalanceControllerApi
) : AbstractBlockchainService(BlockchainDto.SOLANA), OwnershipService {

    /**
     * TODO: here we filter balances that correspond only to the associated token account.
     *  A better fix will be implemented in https://rarible.atlassian.net/browse/CHARLIE-272
     *  by means of introducing a new entity Ownership (Mint + Owner).
     */

    override suspend fun getOwnershipById(ownershipId: String): UnionOwnership {
        val pair = IdParser.split(ownershipId, 2)
        val mint = pair[0]
        val owner = pair[1]

        val balancesDto = balanceApi.getBalancesByMintAndOwner(mint, owner).awaitFirst()
        val associatedTokenAccountBalance = balancesDto.balances.find { it.isAssociatedTokenAccount == true }
            ?: throw createBalanceNotFoundException("No associated token account balance found for $ownershipId")
        return SolanaOwnershipConverter.convert(associatedTokenAccountBalance, blockchain)
    }

    override suspend fun getOwnershipsByIds(ownershipIds: List<String>): List<UnionOwnership> {
        val result = coroutineScope {
            ownershipIds.chunked(16).map { chunk ->
                chunk.mapAsync {
                    getOwnershipById(it)
                }
            }.flatten()
        }
        return result
    }

    override suspend fun getOwnershipsAll(continuation: String?, size: Int): Slice<UnionOwnership> {
        val balancesDto = balanceApi.getBalancesAll(continuation, size, false).awaitFirst()
        val converted = balancesDto.balances.map { SolanaOwnershipConverter.convert(it, blockchain) }

        return Slice(balancesDto.continuation, converted)
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
            total = 0,
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
            total = 0,
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
