package com.rarible.protocol.union.enrichment.meta.collection

import com.rarible.protocol.union.core.model.UnionCollectionMeta
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.enrichment.meta.MetaCustomizer

interface CollectionMetaCustomizer : MetaCustomizer<CollectionIdDto, UnionCollectionMeta>
