package com.rarible.protocol.union.enrichment.meta.item.customizer

import com.rarible.protocol.union.core.model.UnionImageProperties
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.core.model.UnionMetaContent
import com.rarible.protocol.union.core.model.UnionVideoProperties
import com.rarible.protocol.union.core.util.CompositeItemIdParser
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.MetaContentDto
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.enrichment.configuration.EnrichmentMattelMetaCustomizerProperties
import com.rarible.protocol.union.enrichment.meta.item.ItemMetaCustomizer
import org.slf4j.LoggerFactory
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

// Targeted, collection-oriented customizers should be checked last
@Order(CustomizerOrder.MATTEL_META)
@Component
class MattelMetaCustomizer(
    mattelCollectionMetaCustomizers: List<MattelCollectionMetaCustomizer>
) : ItemMetaCustomizer {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val mattelCustomizers = mattelCollectionMetaCustomizers.map { customizer ->
        customizer.collectionIds.map { it to customizer }
    }.flatten().associateBy({ it.first }, { it.second })

    override suspend fun customize(id: ItemIdDto, meta: UnionMeta): UnionMeta {
        if (id.blockchain != BlockchainDto.FLOW) {
            return meta
        }
        val collectionId = CollectionIdDto(id.blockchain, CompositeItemIdParser.split(id.value).first)
        val customizer = mattelCustomizers[collectionId] ?: return meta

        logger.info("Customizing meta for Item {} with {}", id.fullId(), customizer::class.java.simpleName)

        val helper = ItemMetaCustomizerHelper(id, meta)

        return meta.copy(
            name = customizer.getName(helper) ?: meta.name,
            description = helper.attribute(*customizer.fieldDescription),
            rights = helper.attribute(*customizer.fieldRights),
            rightsUri = helper.attribute(*customizer.fieldRightsUri),
            attributes = helper.filterAttributes(
                customizer.attributesWhiteList,
                customizer.attributesValuesBlackList
            ),
            content = fixContentType(meta.content, helper.attribute(*customizer.fieldContentUrl))
        )
    }

    // Hack for non-resolved content properties -it should be VIDEO by default
    private fun fixContentType(content: List<UnionMetaContent>, originalUrl: String?): List<UnionMetaContent> {
        // TODO Compatibility with current customizer in flow-indexer, remove it later
        originalUrl ?: return content

        val targetContent = content.find {
            val properties = it.properties

            // We're looking only for non-resolved properties or images by "default"
            if (properties != null && properties !is UnionImageProperties) {
                return@find false
            }

            // Target content to "fix" - @IMAGE with ORIGINAL representation and same URL as in attributes
            it.representation == MetaContentDto.Representation.ORIGINAL &&
                it.url == originalUrl &&
                properties?.isFull() != true
        } ?: return content

        val fixedContent = targetContent.copy(
            properties = UnionVideoProperties(available = targetContent.properties?.available)
        )

        return content.map {
            if (it == targetContent) {
                fixedContent
            } else {
                it
            }
        }
    }
}

abstract class MattelCollectionMetaCustomizer {

    abstract fun getName(helper: ItemMetaCustomizerHelper): String?

    abstract val collectionIds: List<CollectionIdDto>
    abstract val fieldDescription: Array<String>
    abstract val fieldContentUrl: Array<String>
    abstract val fieldRights: Array<String>
    abstract val fieldRightsUri: Array<String>
    abstract val attributesWhiteList: Set<String>
    open val attributesValuesBlackList: Map<String, Set<String>> = emptyMap()

    protected fun fields(vararg fields: String): Array<String> {
        return fields.toList().toTypedArray()
    }
}

@Component
class HotWheelsCardMetaCustomizer(
    properties: EnrichmentMattelMetaCustomizerProperties
) : MattelCollectionMetaCustomizer() {

    override val collectionIds = properties.hwCard.map { IdParser.parseCollectionId(it) }

    override fun getName(helper: ItemMetaCustomizerHelper): String? {
        val name = helper.attribute("carName") ?: return null
        val cardId = helper.attribute("cardId")
        return "$name #$cardId"
    }

    override val fieldDescription = fields()
    override val fieldContentUrl = fields("imageUrl")
    override val fieldRights = fields("licensorLegal")
    override val fieldRightsUri = fields()

    override val attributesWhiteList = setOf(
        "seriesName",
        "releaseYear",
        "rarity",
        "redeemable",
        "type",
        "mint",
        "totalSupply",
        "cardId",
        "miniCollection"
    )

    override val attributesValuesBlackList = mapOf(
        "type" to setOf("Yes", "No")
    )
}

@Component
class HotWheelsPackMetaCustomizer(
    properties: EnrichmentMattelMetaCustomizerProperties
) : MattelCollectionMetaCustomizer() {

    override val collectionIds = properties.hwPack.map { IdParser.parseCollectionId(it) }

    override fun getName(helper: ItemMetaCustomizerHelper): String? {
        return helper.attribute(*fieldName)
    }

    // "seriesName" - for v1, "carName" - for v2, packName - for V2
    private val fieldName = fields("seriesName", "carName", "packName")
    override val fieldDescription = fields("packDescription")
    override val fieldContentUrl = fields("thumbnailCID")
    override val fieldRights = fields()
    override val fieldRightsUri = fields()

    override val attributesWhiteList = setOf(
        // for v1
        "totalItemCount",
        // for v2
        "tokenReleaseDate",
        "tokenExpireDate",
        "collectionName",
        "collectionDescription",
    )
}

@Component
class HotWheelsTokenMetaCustomizer(
    properties: EnrichmentMattelMetaCustomizerProperties
) : MattelCollectionMetaCustomizer() {

    override val collectionIds = properties.hwToken.map { IdParser.parseCollectionId(it) }

    override fun getName(helper: ItemMetaCustomizerHelper): String? {
        return helper.attribute(*fieldName)
    }

    private val fieldName = fields("carName", "originalCarName")
    override val fieldDescription = fields()
    override val fieldContentUrl = fields("imageCID")
    override val fieldRights = fields()
    override val fieldRightsUri = fields()

    override val attributesWhiteList = setOf(
        "tokenExpireDate",
        "tokenExpirationDate",
        "tokenReleaseDate",
    )
}

@Component
class BarbieCardMetaCustomizer(
    properties: EnrichmentMattelMetaCustomizerProperties
) : MattelCollectionMetaCustomizer() {

    override val collectionIds = properties.barbieCard.map { IdParser.parseCollectionId(it) }

    override fun getName(helper: ItemMetaCustomizerHelper): String? {
        val name = helper.attribute("name") ?: return null
        val cardId = helper.attribute("cardId")
        return "$name #$cardId"
    }

    override val fieldDescription = fields()
    override val fieldContentUrl = fields("imageUrl")
    override val fieldRights = fields("eula")
    override val fieldRightsUri = fields()

    override val attributesWhiteList = setOf(
        "lips",
        "hairColor",
        "mint",
        "hair",
        "tokenId",
        "nose",
        "eyeColor",
        "makeup",
        "releaseYear",
        "eyebrowsColor",
        "freckles",
        "cardId",
        "glasses",
        "releaseDate",
        "career",
        "eyes",
        "version",
        "skinTone",
        "lipColor",
        "seriesName",
        "earrings",
        "faceShape",
        "type",
        "rarity",
        "firstAppearance",
        "necklace",
        "background",
        "eyebrows",
        "miniCollection",
        "totalSupply",
        "editionSize",
        "redeemable"
    )
}

@Component
class BarbiePackMetaCustomizer(
    properties: EnrichmentMattelMetaCustomizerProperties
) : MattelCollectionMetaCustomizer() {

    override val collectionIds = properties.barbiePack.map { IdParser.parseCollectionId(it) }

    override fun getName(helper: ItemMetaCustomizerHelper): String? {
        val name = helper.attribute("packName") ?: return null
        val tokenId = CompositeItemIdParser.split(helper.itemId.value).second
        return "$name #$tokenId"
    }

    override val fieldDescription = fields("packDescription")
    override val fieldContentUrl = fields("thumbnailCID")
    override val fieldRights = fields()
    override val fieldRightsUri = fields()

    override val attributesWhiteList = setOf(
        "type",
        "totalItemCount",
        "collectionName"
    )
}

@Component
class BarbieTokenMetaCustomizer(
    properties: EnrichmentMattelMetaCustomizerProperties
) : MattelCollectionMetaCustomizer() {

    override val collectionIds = properties.barbieToken.map { IdParser.parseCollectionId(it) }

    override fun getName(helper: ItemMetaCustomizerHelper): String? {
        val name = helper.attribute("name") ?: return null
        val cardId = helper.attribute("cardId")
        return "$name #$cardId"
    }

    override val fieldDescription = fields("description")
    override val fieldContentUrl = fields("imageCID")
    override val fieldRights = fields("legal")
    override val fieldRightsUri = fields("eula")

    override val attributesWhiteList = setOf(
        "expirationDate",
        "releaseDate"
    )
}
