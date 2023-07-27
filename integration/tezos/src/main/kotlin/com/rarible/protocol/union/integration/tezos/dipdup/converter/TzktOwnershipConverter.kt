package com.rarible.protocol.union.integration.tezos.dipdup.converter

import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.core.model.UnionOwnership
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.OwnershipIdDto
import com.rarible.protocol.union.dto.continuation.page.Page
import com.rarible.tzkt.model.TokenBalance
import org.slf4j.LoggerFactory
import java.math.BigInteger

object TzktOwnershipConverter {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun convert(ownership: TokenBalance, blockchain: BlockchainDto): UnionOwnership {
        try {
            return convertInternal(ownership, blockchain)
        } catch (e: Exception) {
            logger.error("Failed to convert {} Ownership: {} \n{}", blockchain, e.message, ownership)
            throw e
        }
    }

    fun convert(tzktPage: com.rarible.tzkt.model.Page<TokenBalance>, blockchain: BlockchainDto): Page<UnionOwnership> {
        return Page(
            total = tzktPage.items.size.toLong(),
            continuation = tzktPage.continuation,
            entities = tzktPage.items.map { convertInternal(it, blockchain) }
        )
    }

    private fun convertInternal(ownership: TokenBalance, blockchain: BlockchainDto): UnionOwnership {
        // nullable fields will be fix in the next version of tezos-api
        val tokenId = BigInteger(ownership.token!!.tokenId)
        val owner = UnionAddressConverter.convert(blockchain, ownership.account!!.address)
        val contract = ownership.token!!.contract!!.address

        return UnionOwnership(
            id = OwnershipIdDto(
                blockchain = blockchain,
                contract = contract,
                tokenId = tokenId,
                owner = owner
            ),
            collection = CollectionIdDto(blockchain, contract),
            value = BigInteger(ownership.balance),
            createdAt = ownership.firstTime.toInstant(),
            lastUpdatedAt = ownership.lastTime.toInstant(),
            creators = emptyList(), // Deprecated field
            lazyValue = BigInteger.ZERO, // lazy isn't supported yet
            pending = emptyList() // In Union we won't use this field for Tezos
        )
    }
}
