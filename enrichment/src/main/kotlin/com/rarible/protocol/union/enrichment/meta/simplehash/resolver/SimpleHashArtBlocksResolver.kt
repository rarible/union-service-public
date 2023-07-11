package com.rarible.protocol.union.enrichment.meta.simplehash.resolver

import com.fasterxml.jackson.databind.ObjectMapper
import com.rarible.protocol.union.core.model.UnionMetaAttribute
import com.rarible.protocol.union.enrichment.meta.simplehash.SimpleHashItem

class SimpleHashArtBlocksResolver(
    mapper: ObjectMapper
) : SimpleHashResolver(mapper) {

    override fun support(source: SimpleHashItem): Boolean {
        return address(source.nftId) in ART_BLOCKS_ADDRESSES
    }

    override fun attributes(source: SimpleHashItem): List<UnionMetaAttribute> {
        val attributes = emptyList<UnionMetaAttribute>().toMutableList()
        source.extraMetadata?.features?.map {
            UnionMetaAttribute(it.key, it.value)
        }?.forEach(attributes::add)
        source.extraMetadata?.projectId?.let { attributes.add(UnionMetaAttribute("project_id", it)) }
        source.extraMetadata?.collectionName?.let { attributes.add(UnionMetaAttribute("collection_name", it)) }

        return attributes
    }

    companion object {
        val ART_BLOCKS_ADDRESSES = setOf(
            "0x059edd72cd353df5106d2b9cc5ab83a52287ac3a",
            "0xa7d8d9ef8d8ce8992df33d8b8cf4aebabd5bd270",
            "0x99a9b7c1116f9ceeb1652de04d5969cce509b069",
            "0x942bc2d3e7a589fe5bd4a5c6ef9727dfd82f5c8a",
            "0xea698596b6009a622c3ed00dd5a8b5d1cae4fc36"
        )
    }
}