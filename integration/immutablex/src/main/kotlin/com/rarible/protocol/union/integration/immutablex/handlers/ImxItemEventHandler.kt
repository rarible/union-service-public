package com.rarible.protocol.union.integration.immutablex.handlers

import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionItemMetaEvent
import com.rarible.protocol.union.core.model.UnionItemMetaUpdateEvent
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexAsset
import com.rarible.protocol.union.integration.immutablex.client.ImxActivityClient
import com.rarible.protocol.union.integration.immutablex.converter.ImxItemConverter
import com.rarible.protocol.union.integration.immutablex.converter.ImxItemMetaConverter

class ImxItemEventHandler(
    private val itemMetaHandler: IncomingEventHandler<UnionItemMetaEvent>,
    private val activityClient: ImxActivityClient
) {

    private val blockchain = BlockchainDto.IMMUTABLEX

    suspend fun handle(asset: ImmutablexAsset) {
        val creator = activityClient.getItemCreator(asset.itemId)
        val item = ImxItemConverter.convert(asset, creator, blockchain)
        val meta = ImxItemMetaConverter.convert(asset, blockchain)
        itemMetaHandler.onEvent(UnionItemMetaUpdateEvent(item.id, item, meta))
    }
}
