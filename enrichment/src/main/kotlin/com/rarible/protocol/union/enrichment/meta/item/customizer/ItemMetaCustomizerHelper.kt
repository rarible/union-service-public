package com.rarible.protocol.union.enrichment.meta.item.customizer

import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.core.model.UnionMetaAttribute
import com.rarible.protocol.union.dto.ItemIdDto

class ItemMetaCustomizerHelper(
    val itemId: ItemIdDto,
    val meta: UnionMeta,
) {

    // Just for case if there are duplicated attributes - should not be so, originally
    private val associatedAttributes = meta.attributes.groupBy { it.key }

    fun filterAttributes(whitelist: Set<String>): List<UnionMetaAttribute> {
        return meta.attributes.filter { it.key in whitelist }
    }

    fun attribute(vararg keys: String): String? {
        return keys.firstNotNullOfOrNull { key ->
            associatedAttributes[key]?.get(0)?.value
        }
    }
}
