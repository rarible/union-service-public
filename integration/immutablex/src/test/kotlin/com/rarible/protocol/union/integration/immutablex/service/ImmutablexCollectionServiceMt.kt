package com.rarible.protocol.union.integration.immutablex.service

import com.rarible.protocol.union.core.test.ManualTest
import com.rarible.protocol.union.integration.ImmutablexManualTest
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

@ManualTest
class ImmutablexCollectionServiceMt : ImmutablexManualTest() {

    private val service = ImmutablexCollectionService(collectionClient)

    @Test
    fun getById() = runBlocking<Unit> {
        val result = service.getCollectionById("0x0015953831777d0d97361d6ca7032e44979de86b")

        println(result)
        assertThat(result.name).isEqualTo("Pip Token")
    }

    @Test
    fun getAll() = runBlocking<Unit> {
        val page = service.getAllCollections(null, 2).entities

        println(page)
        assertThat(page).hasSize(2)

        val withContinuation = service.getAllCollections(page[0].id.value, 1).entities
        assertThat(withContinuation[0]).isEqualTo(page[1])
    }

}