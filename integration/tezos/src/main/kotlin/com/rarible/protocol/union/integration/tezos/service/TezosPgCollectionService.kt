package com.rarible.protocol.union.integration.tezos.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.rarible.protocol.union.core.model.UnionCollection
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.BlockchainGroupDto
import com.rarible.protocol.union.dto.CollectionDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.UnionAddress
import io.r2dbc.spi.ConnectionFactory
import io.r2dbc.spi.Row
import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.r2dbc.core.DatabaseClient

class TezosPgCollectionService(
    val mapper: ObjectMapper,
    connectionFactory: ConnectionFactory
) {

    val client = DatabaseClient.create(connectionFactory)

    suspend fun getCollectionsByIds(ids: List<String>): List<UnionCollection> {
        val sql = """
            select c.address,
                   t.name,
                   c.royalties_id,
                   c.metadata,
                   c.owner,
                   c.minters
            from contracts c
                left join tzip16_metadata t on t.contract = c.address
                where c.address in (:ids)
        """.trimIndent()
        return client.sql(sql)
            .bind("ids", ids).map(this::convertList)
            .all()
            .collectList().awaitSingle()
    }

    private fun convertList(row: Row): UnionCollection {
        val address: String = row.get("address", String::class.java)
        val name: String = row.get("name", String::class.java) ?: "Unnamed Collection"
        val hasRoyalty = row["royalties_id"] != null
        val metadata: Map<String, Object> = mapper.readValue(row.get("metadata", String::class.java))
        val symbol = metadata.let { metadata["symbol"] }?.toString()
        var minters = emptyList<UnionAddress>().toMutableList()
        val owner = row.get("owner")?.let { UnionAddress(BlockchainGroupDto.TEZOS, it.toString()) }
        owner?.let { minters.add(it) }
        row.get("minters", Array<String>::class.java)
            ?.let { minter -> minter.map { UnionAddress(BlockchainGroupDto.TEZOS, it) }.forEach { minters.add(it) } }
        return UnionCollection(
            id = CollectionIdDto(BlockchainDto.TEZOS, address),
            name = name,
            type = CollectionDto.Type.TEZOS_MT, // it's safe to return MT always due to no difference in Tezos for NFT and MT tokens
            minters = minters,
            features = features(hasRoyalty),
            owner = owner,
            symbol = symbol,
        )
    }

    private fun features(hasRoyalty: Boolean): List<CollectionDto.Features> {
        return when (hasRoyalty) {
            true -> listOf(CollectionDto.Features.BURN, CollectionDto.Features.SECONDARY_SALE_FEES)
            else -> emptyList()
        }
    }
}
