package com.rarible.protocol.union.enrichment.meta.collection.provider

import com.rarible.protocol.union.core.model.UnionCollectionMeta
import com.rarible.protocol.union.core.model.UnionImageProperties
import com.rarible.protocol.union.core.model.UnionMetaContent
import com.rarible.protocol.union.core.model.download.DownloadException
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.MetaContentDto
import com.rarible.protocol.union.enrichment.configuration.EnrichmentCollectionProperties
import com.rarible.protocol.union.enrichment.custom.collection.provider.ArtBlocksCustomCollectionProvider
import com.rarible.protocol.union.enrichment.meta.provider.MetaCustomProvider
import com.rarible.protocol.union.enrichment.model.EnrichmentCollection
import com.rarible.protocol.union.enrichment.model.EnrichmentCollectionId
import com.rarible.protocol.union.enrichment.repository.CollectionRepository
import com.rarible.protocol.union.enrichment.service.SimpleHashService
import org.springframework.stereotype.Component

@Component
class ArtBlocksCollectionMetaCustomProvider(
    enrichmentCollectionProperties: EnrichmentCollectionProperties,
    private val simpleHashService: SimpleHashService,
    private val collectionRepository: CollectionRepository
) : CollectionMetaCustomProvider {

    private val atrBlocksCollections = enrichmentCollectionProperties.mappings
        .find { it.name == ArtBlocksCustomCollectionProvider.MAPPING_NAME }
        ?.getCollectionIds()
        ?.toSet() ?: emptySet()

    override suspend fun fetch(blockchain: BlockchainDto, id: String): MetaCustomProvider.Result<UnionCollectionMeta> {
        val collectionId = EnrichmentCollectionId(blockchain, id)

        // TODO ideally we should get collection somewhere outside
        // Should never be null
        val collection = collectionRepository.get(collectionId)
        val projectId = collection?.extra?.get("project_id")
        if (collection == null ||
            !isArtBlocksCollection(collection) ||
            projectId == null
        ) {
            return MetaCustomProvider.Result(false)
        }

        val tokenId = (projectId + "000000").toBigInteger()

        val parent = collectionRepository.get(collection.parent!!)
            ?: throw DownloadException(
                "Failed to get custom ArtBlocks collection meta for ${collection.id} (projectId=$projectId), " +
                    "there is no parent collection ${collection.parent} in DB"
            )

        // We need original meta to copy some basic fields like fees
        val exist = parent.metaEntry?.data
            ?: throw DownloadException(
                "Failed to get custom ArtBlocks collection meta for ${collection.id} (projectId=$projectId), " +
                    "parent collection ${parent.id} has no metadata"
            )

        val firstItemId = ItemIdDto(collection.blockchain, parent.collectionId, tokenId)
        val shItem = simpleHashService.fetchRaw(firstItemId)
            ?: throw DownloadException(
                "Failed to get custom ArtBlocks collection meta for ${collection.id} (projectId=$projectId), " +
                    "first Item $firstItemId can't be fetched with SimpleHash"
            )

        val name = (shItem.extraMetadata?.collectionName ?: collection.name)
            ?: throw DownloadException(
                "Failed to get custom ArtBlocks collection meta for ${collection.id} (projectId=$projectId), " +
                    "can't determine collection name"
            )

        // Collection image OR original image of first item
        val original = (shItem.collection?.imageUrl ?: shItem.extraMetadata?.imageOriginalUrl)?.let {
            UnionMetaContent(
                url = it,
                representation = MetaContentDto.Representation.ORIGINAL,
                properties = UnionImageProperties()
            )
        }

        val banner = shItem.collection?.bannerImageUrl?.let {
            UnionMetaContent(
                url = it,
                representation = MetaContentDto.Representation.BIG,
                properties = UnionImageProperties()
            )
        }

        val result = exist.copy(
            name = name,
            description = shItem.description,
            content = listOfNotNull(original, banner)
        )
        return MetaCustomProvider.Result(true, result)
    }

    private fun isArtBlocksCollection(collection: EnrichmentCollection): Boolean {
        return collection.parent != null && atrBlocksCollections.contains(collection.parent)
    }
}
