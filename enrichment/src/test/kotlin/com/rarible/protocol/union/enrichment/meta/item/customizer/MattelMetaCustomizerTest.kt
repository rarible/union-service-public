package com.rarible.protocol.union.enrichment.meta.item.customizer

import com.rarible.core.test.data.randomString
import com.rarible.protocol.union.core.model.UnionImageProperties
import com.rarible.protocol.union.core.model.UnionMetaAttribute
import com.rarible.protocol.union.core.model.UnionMetaContent
import com.rarible.protocol.union.core.model.UnionVideoProperties
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.MetaContentDto
import com.rarible.protocol.union.enrichment.configuration.EnrichmentMattelMetaCustomizerProperties
import com.rarible.protocol.union.enrichment.test.data.randomUnionMeta
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class MattelMetaCustomizerTest {

    private val collectionId = CollectionIdDto(BlockchainDto.FLOW, randomString())
    private val itemId = ItemIdDto(BlockchainDto.FLOW, "${collectionId.value}:1")
    private val properties = EnrichmentMattelMetaCustomizerProperties(
        barbieCard = listOf(collectionId.fullId()),
        barbieToken = listOf(collectionId.fullId()),
        barbiePack = listOf(collectionId.fullId()),
        hwCard = listOf(collectionId.fullId()),
        hwPack = listOf(collectionId.fullId()),
    )

    @Test
    fun `customize - hw card`() = runBlocking<Unit> {
        val whitListAttributes = listOf(
            UnionMetaAttribute("seriesName", "seriesName_value"),
            UnionMetaAttribute("releaseYear", "releaseYear_value"),
            UnionMetaAttribute("rarity", "rarity_value"),
            UnionMetaAttribute("redeemable", "redeemable_value"),
            UnionMetaAttribute("type", "type_value"),
            UnionMetaAttribute("mint", "mint_value"),
            UnionMetaAttribute("totalSupply", "totalSupply_value"),
            UnionMetaAttribute("cardId", "cardId_value"),
            UnionMetaAttribute("miniCollection", "miniCollection_value")
        )
        val attributes = listOf(
            UnionMetaAttribute("carName", "carName_value"),
            UnionMetaAttribute("licensorLegal", "licensorLegal_value")
        ) + whitListAttributes

        val meta = randomUnionMeta().copy(attributes = attributes)

        val customizer = MattelMetaCustomizer(listOf(HotWheelsCardMetaCustomizer(properties)))

        val customized = customizer.customize(itemId, meta)

        assertThat(customized.name).isEqualTo("carName_value #cardId_value")
        assertThat(customized.rights).isEqualTo("licensorLegal_value")
        assertThat(customized.attributes).isEqualTo(whitListAttributes)
    }

    @Test
    fun `customize - hw pack`() = runBlocking<Unit> {
        val whitListAttributes = listOf(
            UnionMetaAttribute("totalItemCount", "totalItemCount_value"),
            UnionMetaAttribute("tokenReleaseDate", "tokenReleaseDate_value"),
            UnionMetaAttribute("tokenExpireDate", "tokenExpireDate_value"),
            UnionMetaAttribute("collectionName", "collectionName_value"),
            UnionMetaAttribute("collectionDescription", "collectionDescription_value")
        )
        val attributes = listOf(
            UnionMetaAttribute("seriesName", "seriesName_value"),
            UnionMetaAttribute("packDescription", "packDescription_value")
        ) + whitListAttributes

        val meta = randomUnionMeta().copy(attributes = attributes)

        val customizer = MattelMetaCustomizer(listOf(HotWheelsPackMetaCustomizer(properties)))

        val customized = customizer.customize(itemId, meta)

        assertThat(customized.name).isEqualTo("seriesName_value")
        assertThat(customized.description).isEqualTo("packDescription_value")
        assertThat(customized.attributes).isEqualTo(whitListAttributes)
    }

    @Test
    fun `customize - barbie card`() = runBlocking<Unit> {
        val whitListAttributes = listOf(
            UnionMetaAttribute("lips", "lips_value"),
            UnionMetaAttribute("hairColor", "hairColor_value"),
            UnionMetaAttribute("mint", "mint_value"),
            UnionMetaAttribute("hair", "hair_value"),
            UnionMetaAttribute("tokenId", "tokenId_value"),
            UnionMetaAttribute("nose", "nose_value"),
            UnionMetaAttribute("eyeColor", "eyeColor_value"),
            UnionMetaAttribute("makeup", "makeup_value"),
            UnionMetaAttribute("releaseYear", "releaseYear_value"),
            UnionMetaAttribute("eyebrowsColor", "eyebrowsColor_value"),
            UnionMetaAttribute("freckles", "freckles_value"),
            UnionMetaAttribute("cardId", "cardId_value"),
            UnionMetaAttribute("glasses", "glasses_value"),
            UnionMetaAttribute("releaseDate", "releaseDate_value"),
            UnionMetaAttribute("career", "career_value"),
            UnionMetaAttribute("eyes", "eyes_value"),
            UnionMetaAttribute("version", "version_value"),
            UnionMetaAttribute("skinTone", "skinTone_value"),
            UnionMetaAttribute("lipColor", "lipColor_value"),
            UnionMetaAttribute("seriesName", "seriesName_value"),
            UnionMetaAttribute("earrings", "earrings_value"),
            UnionMetaAttribute("faceShape", "faceShape_value"),
            UnionMetaAttribute("type", "type_value"),
            UnionMetaAttribute("rarity", "rarity_value"),
            UnionMetaAttribute("firstAppearance", "firstAppearance_value"),
            UnionMetaAttribute("necklace", "necklace_value"),
            UnionMetaAttribute("background", "background_value"),
            UnionMetaAttribute("eyebrows", "eyebrows_value"),
            UnionMetaAttribute("miniCollection", "miniCollection_value"),
            UnionMetaAttribute("totalSupply", "totalSupply_value"),
            UnionMetaAttribute("editionSize", "editionSize_value"),
            UnionMetaAttribute("redeemable", "redeemable_value"),
        )
        val attributes = listOf(
            UnionMetaAttribute("imageUrl", "imageUrl_value"),
            UnionMetaAttribute("eula", "eula_value"),
            UnionMetaAttribute("name", "name_value")
        ) + whitListAttributes

        val meta = randomUnionMeta().copy(attributes = attributes)

        val customizer = MattelMetaCustomizer(listOf(BarbieCardMetaCustomizer(properties)))

        val customized = customizer.customize(itemId, meta)

        assertThat(customized.name).isEqualTo("name_value #cardId_value")
        assertThat(customized.rights).isEqualTo("eula_value")
        assertThat(customized.attributes).isEqualTo(whitListAttributes)
    }

    @Test
    fun `customize - barbie pack`() = runBlocking<Unit> {
        val whitListAttributes = listOf(
            UnionMetaAttribute("type", "type_value"),
            UnionMetaAttribute("totalItemCount", "totalItemCount_value"),
            UnionMetaAttribute("collectionName", "collectionName_value")
        )
        val attributes = listOf(
            UnionMetaAttribute("thumbnailCID", "thumbnailCID_value"),
            UnionMetaAttribute("packDescription", "packDescription_value"),
            UnionMetaAttribute("packName", "packName_value")
        ) + whitListAttributes

        val meta = randomUnionMeta().copy(attributes = attributes)

        val customizer = MattelMetaCustomizer(listOf(BarbiePackMetaCustomizer(properties)))

        val customized = customizer.customize(itemId, meta)

        assertThat(customized.name).isEqualTo("packName_value #1")
        assertThat(customized.description).isEqualTo("packDescription_value")
        assertThat(customized.attributes).isEqualTo(whitListAttributes)
    }

    @Test
    fun `customize - barbie token`() = runBlocking<Unit> {
        val whitListAttributes = listOf(
            UnionMetaAttribute("expirationDate", "expirationDate_value"),
            UnionMetaAttribute("releaseDate", "releaseDate_value")
        )
        val attributes = listOf(
            UnionMetaAttribute("tokenImageHash", "imageUrl_value"),
            UnionMetaAttribute("eula", "eula_value"),
            UnionMetaAttribute("legal", "legal_value"),
            UnionMetaAttribute("name", "name_value"),
            UnionMetaAttribute("cardId", "cardId_value")
        ) + whitListAttributes

        val meta = randomUnionMeta().copy(attributes = attributes)

        val customizer = MattelMetaCustomizer(listOf(BarbieTokenMetaCustomizer(properties)))

        val customized = customizer.customize(itemId, meta)

        assertThat(customized.name).isEqualTo("name_value #cardId_value")
        assertThat(customized.rights).isEqualTo("legal_value")
        assertThat(customized.rightsUri).isEqualTo("eula_value")
        assertThat(customized.attributes).isEqualTo(whitListAttributes)
    }

    @Test
    fun `customize content type - ok, no properties`() = runBlocking<Unit> {
        val meta = randomUnionMeta().copy(
            attributes = listOf(UnionMetaAttribute("imageUrl", "imageUrl_value")),
            content = listOf(UnionMetaContent("imageUrl_value", MetaContentDto.Representation.ORIGINAL))
        )

        val customizer = MattelMetaCustomizer(listOf(BarbieCardMetaCustomizer(properties)))

        val customized = customizer.customize(itemId, meta)

        assertThat(customized.content[0].properties).isInstanceOf(UnionVideoProperties::class.java)
    }

    @Test
    fun `customize content type - ok, image type`() = runBlocking<Unit> {
        val meta = randomUnionMeta().copy(
            attributes = listOf(UnionMetaAttribute("imageUrl", "imageUrl_value")),
            content = listOf(
                UnionMetaContent(
                    url = "imageUrl_value",
                    representation = MetaContentDto.Representation.ORIGINAL,
                    properties = UnionImageProperties()
                )
            )
        )

        val customizer = MattelMetaCustomizer(listOf(BarbieCardMetaCustomizer(properties)))

        val customized = customizer.customize(itemId, meta)

        assertThat(customized.content[0].properties).isInstanceOf(UnionVideoProperties::class.java)
    }

    @Test
    fun `customize content type - skipped, another url`() = runBlocking<Unit> {
        val meta = randomUnionMeta().copy(
            attributes = listOf(UnionMetaAttribute("imageUrl", "imageUrl_value")),
            content = listOf(UnionMetaContent(randomString(), MetaContentDto.Representation.ORIGINAL))
        )

        val customizer = MattelMetaCustomizer(listOf(BarbieTokenMetaCustomizer(properties)))

        val customized = customizer.customize(itemId, meta)

        assertThat(customized.content[0].properties).isNull()
    }

    @Test
    fun `customize content type - skipped, another representation`() = runBlocking<Unit> {
        val meta = randomUnionMeta().copy(
            attributes = listOf(UnionMetaAttribute("imageUrl", "imageUrl_value")),
            content = listOf(
                UnionMetaContent(
                    url = "imageUrl_value",
                    representation = MetaContentDto.Representation.PREVIEW,
                    properties = UnionImageProperties()
                )
            )
        )

        val customizer = MattelMetaCustomizer(listOf(BarbieTokenMetaCustomizer(properties)))

        val customized = customizer.customize(itemId, meta)

        assertThat(customized.content[0].properties).isInstanceOf(UnionImageProperties::class.java)
    }

    @Test
    fun `customize content type - skipped, image fully qualified`() = runBlocking<Unit> {
        val meta = randomUnionMeta().copy(
            attributes = listOf(UnionMetaAttribute("imageUrl", "imageUrl_value")),
            content = listOf(
                UnionMetaContent(
                    url = "imageUrl_value",
                    representation = MetaContentDto.Representation.ORIGINAL,
                    properties = UnionImageProperties("image/png", 1, true, 1, 1)
                )
            )
        )

        val customizer = MattelMetaCustomizer(listOf(BarbieTokenMetaCustomizer(properties)))

        val customized = customizer.customize(itemId, meta)

        assertThat(customized.content[0].properties).isInstanceOf(UnionImageProperties::class.java)
    }
}
