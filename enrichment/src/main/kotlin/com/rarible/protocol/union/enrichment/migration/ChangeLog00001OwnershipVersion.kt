package com.rarible.protocol.union.enrichment.migration

import com.github.cloudyrock.mongock.ChangeLog
import com.github.cloudyrock.mongock.ChangeSet
import com.rarible.protocol.union.enrichment.model.ShortOwnership
import io.changock.migration.api.annotations.NonLockGuarded
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.runBlocking
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.data.mongodb.core.query.exists

@ChangeLog(order = "1")
class ChangeLog00001OwnershipVersion {

    @ChangeSet(
        id = "ChangeLog00001OwnershipVersion.setOwnershipVersion",
        order = "1",
        author = "protocol"
    )
    fun setOwnershipVersion(
        @NonLockGuarded template: ReactiveMongoTemplate
    ) = runBlocking {
        val query = Query(
            Criteria().andOperator(
                ShortOwnership::version exists false
            )
        )
        template.updateMulti(
            query,
            Update.update(ShortOwnership::version.name, 0L),
            "ownership"
        ).awaitFirstOrNull()
    }
}