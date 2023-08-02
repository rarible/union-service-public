package com.rarible.protocol.union.enrichment.meta.item

import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.MetaContentDto
import org.slf4j.LoggerFactory

object ItemMetaComparator {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun hasChanged(itemId: ItemIdDto, previous: UnionMeta, updated: UnionMeta): Boolean {
        // There is no sense to compare meta from different sources
        if (previous.source == null || updated.source == null || previous.source != updated.source) {
            return false
        }

        // Contributors like SimpleHash might change meta slightly, so it would be better to check
        // completely equivalent metadata in terms of source
        if (previous.contributors != updated.contributors) {
            return false
        }

        // Now lets check fields-markers which can indicate reveal of metadata: name, description, metaUri and content
        if (previous.name != updated.name) {
            logger.info("Meta of Item {} has been changed (name: {} -> {})", itemId, previous.name, updated.name)
            return true
        }

        val prevDesc = previous.description
        val updatedDesc = updated.description
        if (prevDesc != updatedDesc) {
            logger.info("Meta of Item {} changed (description: {} -> {}", itemId, prevDesc, updatedDesc)
            return true
        }

        val prevMetaUri = previous.originalMetaUri
        val updatedMetaUri = updated.originalMetaUri
        if (prevMetaUri != null && updatedMetaUri != null && prevMetaUri != updatedMetaUri) {
            logger.info("Meta of Item {} has been changed (metaUri: {} -> {})", itemId, prevMetaUri, updatedMetaUri)
            return true
        }

        val prevUrl = getOriginalContentUrl(previous)
        val updatedUrl = getOriginalContentUrl(updated)
        if (prevUrl != updatedUrl) {
            logger.info("Meta of Item {} has been changed (content: {} -> {})", itemId, prevUrl, updatedUrl)
            return true
        }

        return false
    }

    private fun getOriginalContentUrl(meta: UnionMeta): String? {
        return meta.content.find { it.representation == MetaContentDto.Representation.ORIGINAL }?.url
    }
}
