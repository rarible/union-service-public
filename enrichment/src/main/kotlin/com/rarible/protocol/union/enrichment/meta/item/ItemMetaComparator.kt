package com.rarible.protocol.union.enrichment.meta.item

import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.dto.ItemIdDto
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

interface ItemMetaComparator {
    fun hasChanged(
        itemId: ItemIdDto,
        previous: UnionMeta,
        updated: UnionMeta
    ): Boolean
}

@Component
class StrictItemMetaComparator(
    private val defaultItemMetaComparator: DefaultItemMetaComparator
) : ItemMetaComparator {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun hasChanged(
        itemId: ItemIdDto,
        previous: UnionMeta,
        updated: UnionMeta
    ): Boolean {

        // There is no sense to compare meta from different sources
        if (previous.source == null || updated.source == null || previous.source != updated.source) {
            logger.info(
                "Meta for item {} has different sources (previous={}, updated={})," +
                    " meta refresh for collection prohibited", itemId, previous.source, updated.source
            )
            return false
        }

        // Contributors like SimpleHash might change meta slightly, so it would be better to check
        // completely equivalent metadata in terms of source
        // TODO we should deal with it if new meta provider will be added
        if (previous.contributors != updated.contributors) {
            logger.info(
                "Meta for item {} has different contributors (previous={}, updated={})," +
                    " meta refresh for collection prohibited", itemId, previous.contributors, updated.contributors
            )
            return false
        }

        return defaultItemMetaComparator.hasChanged(itemId, previous, updated)
    }
}

@Component
class DefaultItemMetaComparator : ItemMetaComparator {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun hasChanged(
        itemId: ItemIdDto,
        previous: UnionMeta,
        updated: UnionMeta,
    ): Boolean {
        val result = previous.toComparable() != updated.toComparable()
        if (result) {
            logger.info(
                "Meta changed for item $itemId from $previous to $updated " +
                    "will allow meta refresh for collection"
            )
        }
        return result
    }
}
