package com.rarible.protocol.union.enrichment.service

import com.rarible.ethereum.domain.Blockchain
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.enrichment.configuration.UnionMetaProperties
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import java.util.*

@Service
@ConditionalOnProperty("meta.simpleHash.enabled", havingValue = "true")
class SimpleHashService(
    private val props: UnionMetaProperties,
    private val simpleHashClient: WebClient
) {

    fun isSupported(blockchain: BlockchainDto) : Boolean {
        return blockchain in props.simpleHash.supported
    }

    suspend fun fetch(key: ItemIdDto): UnionMeta? {
        // TODO: request from simplehash
        return null
    }

    suspend fun fetch(key: CollectionIdDto): UnionMeta? {
        // TODO: request from simplehash
        return null
    }

}
