package com.rarible.protocol.union.core.model.elastic

import com.rarible.protocol.union.core.model.elastic.EsEntitiesConfig.loadMapping
import com.rarible.protocol.union.core.model.elastic.EsEntitiesConfig.loadSettings
import com.rarible.protocol.union.dto.BlockchainDto
import org.springframework.data.annotation.Id
import org.springframework.data.elasticsearch.annotations.Field
import org.springframework.data.elasticsearch.annotations.FieldType
import java.time.Instant

data class EsOwnership(
    @Id
    @Field(fielddata = true)
    val ownershipId: String, // holds sha256 in case original OwnershipId is bigger than 512 bytes
    val originalOwnershipId: String?, // only filled when above field holds sha256
    val blockchain: BlockchainDto,
    val itemId: String? = null,
    val collection: String? = null,
    val owner: String,
    @Field(type = FieldType.Date, fielddata = true)
    val date: Instant,
    val auctionId: String?,
    val auctionOwnershipId: String?,

    val bestSellAmount: Double? = null,
    val bestSellCurrency: String? = null, // blockchain:currencyAddress
    val bestSellMarketplace: String? = null,
) {

    val id: String
        get() = originalOwnershipId ?: ownershipId

    companion object {
        private const val VERSION: Int = 1

        val ENTITY_DEFINITION = EsEntity.OWNERSHIP.let {
            EntityDefinition(
                entity = it,
                mapping = loadMapping(it),
                versionData = VERSION,
                settings = loadSettings(it)
            )
        }
    }
}
