package com.rarible.protocol.union.enrichment.migration

import com.github.cloudyrock.mongock.ChangeLog
import com.github.cloudyrock.mongock.ChangeSet
import com.rarible.protocol.union.enrichment.model.EnrichmentCollection
import com.rarible.protocol.union.enrichment.model.EnrichmentCollectionId
import io.changock.migration.api.annotations.NonLockGuarded
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.data.mongodb.core.query.inValues

@ChangeLog(order = "4")
class ChangeLog00004CollectionsWithoutTraits {

    @ChangeSet(
        id = "ChangeLog00004CollectionsWithoutTraits.deleteCollectionStatistics",
        order = "1",
        author = "dsaraykin"
    )
    fun updateCollectionsHasTraits(
        @NonLockGuarded template: ReactiveMongoTemplate
    ) {
        val query = Query(Criteria.where("_id").inValues(COLLECTION_IDS))
        template.updateMulti(
            query,
            Update().set(EnrichmentCollection::hasTraits.name, false),
            EnrichmentCollection::class.java,
        ).block()
    }

    companion object {
        private val COLLECTION_IDS = listOf(
            "ETHEREUM:0x82c7a8f707110f5fbb16184a5933e9f78a34c6ab",
            "ETHEREUM:0x495f947276749ce646f68ac8c248420045cb7b5e",
            "ETHEREUM:0xb66a603f4cfe17e3d27b87a8bfcad319856518b8",
            "ETHEREUM:0xc9154424b823b10579895ccbe442d41b9abd96ed",
            "POLYGON:0x35f8aee672cde8e5fd09c93d2bfe4ff5a9cf0756",
            "POLYGON:0xa2d9ded6115b7b7208459450d676f0127418ae7a",
            "POLYGON:0x2953399124f0cbb46d2cbacd8a89cf0599974963",
            "POLYGON:0x03e055692e77e56abf7f5570d9c64c194ba15616",
            "IMMUTABLEX:0x184612188346bf155f40861c3e72ae54e87358e6",
        ).map(EnrichmentCollectionId::of)
    }
}
