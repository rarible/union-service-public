package com.rarible.protocol.union.integration.immutablex.converter

import com.rarible.core.logging.Logger
import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.core.model.UnionCollection
import com.rarible.protocol.union.core.model.UnionCollectionMeta
import com.rarible.protocol.union.core.model.UnionMetaContent
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.MetaContentDto
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexCollection

object ImxCollectionConverter {

    private val logger by Logger()

    fun convert(source: ImmutablexCollection, blockchain: BlockchainDto): UnionCollection {
        try {
            return convertInternal(source, blockchain)
        } catch (e: Exception) {
            logger.error("Failed to convert {} Collection: {} \n{}", blockchain, e.message, source)
            throw e
        }
    }

    private fun convertInternal(source: ImmutablexCollection, blockchain: BlockchainDto): UnionCollection {
        val minter = source.projectOwnerAddress?.let { UnionAddressConverter.convert(blockchain, it) }
        return UnionCollection(
            id = CollectionIdDto(blockchain, source.address),
            name = source.name,
            structureKind = UnionCollection.StructureKind.REGULAR,
            type = UnionCollection.Type.ERC721,
            features = listOf(UnionCollection.Features.APPROVE_FOR_ALL),
            minters = listOfNotNull(minter),
            owner = minter,
            meta = convertMeta(source)
        )
    }

    private fun convertMeta(source: ImmutablexCollection): UnionCollectionMeta {
        return UnionCollectionMeta(
            name = source.name,
            description = source.description,
            content = if (!source.collectionImageUrl.isNullOrEmpty()) listOf(
                UnionMetaContent(
                    url = source.collectionImageUrl,
                    representation = MetaContentDto.Representation.ORIGINAL
                )
            ) else emptyList()
        )
    }

}
