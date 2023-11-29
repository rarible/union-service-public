package com.rarible.protocol.union.enrichment.util

import com.rarible.protocol.union.core.util.sha2
import com.rarible.protocol.union.dto.CollectionIdDto

private const val ID_PARTS_SEPARATOR = ":"

object TraitUtils {
    fun getId(collectionId: CollectionIdDto, key: String, value: String): String =
        listOf(collectionId.toString(), key.sha2(), value.sha2()).joinToString(ID_PARTS_SEPARATOR)
}
