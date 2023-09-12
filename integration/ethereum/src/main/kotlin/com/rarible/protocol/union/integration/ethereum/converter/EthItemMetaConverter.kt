package com.rarible.protocol.union.integration.ethereum.converter

import com.rarible.protocol.dto.NftItemMetaEventDto
import com.rarible.protocol.dto.NftItemMetaRefreshEventDto
import com.rarible.protocol.union.core.model.UnionItemMetaEvent
import com.rarible.protocol.union.core.model.UnionItemMetaRefreshEvent
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ItemIdDto

object EthItemMetaConverter {
    fun convert(event: NftItemMetaEventDto, blockchain: BlockchainDto): UnionItemMetaEvent {
        return when (event) {
            is NftItemMetaRefreshEventDto -> {
                UnionItemMetaRefreshEvent(
                    itemId = ItemIdDto(
                        blockchain = blockchain,
                        value = event.itemId
                    )
                )
            }
        }
    }
}
