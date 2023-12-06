package com.rarible.protocol.union.enrichment.util

import com.rarible.protocol.union.core.util.sha2
import com.rarible.protocol.union.enrichment.model.EnrichmentCollectionId

private const val ID_PARTS_SEPARATOR = ":"

object TraitUtils {
    fun getId(collectionId: EnrichmentCollectionId, key: String, value: String?): String =
        listOfNotNull(collectionId.toString(), key.sha2(), value?.sha2()).joinToString(ID_PARTS_SEPARATOR)
}
