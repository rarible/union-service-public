package com.rarible.protocol.union.worker.task.meta

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.rarible.core.test.data.randomAddress
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.enrichment.service.SimpleHashService
import com.rarible.protocol.union.worker.job.meta.CollectionMetaRefreshSchedulingService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.SpyK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
internal class RefreshMetaSimpleHashTaskTest {
    @InjectMockKs
    private lateinit var refreshMetaSimpleHashTask: RefreshMetaSimpleHashTask

    @SpyK
    private var objectMapper: ObjectMapper = jacksonObjectMapper()

    @MockK
    private lateinit var simpleHashService: SimpleHashService

    @MockK
    private lateinit var collectionMetaRefreshSchedulingService: CollectionMetaRefreshSchedulingService

    @Test
    fun `run simplehash contract refresh`() = runTest {
        val address = randomAddress()
        val c1 = "ETHEREUM:$address"

        coEvery {
            simpleHashService.refreshContract(
                CollectionIdDto(
                    blockchain = BlockchainDto.ETHEREUM,
                    value = address.toString()
                )
            )
        } returns Unit

        coEvery {
            collectionMetaRefreshSchedulingService.scheduleTask(match {
                it.collectionId == c1 && !it.withSimpleHash && it.full && !it.scheduled
            })
        } returns Unit

        refreshMetaSimpleHashTask.runLongTask(
            from = null,
            param = objectMapper.writeValueAsString(
                RefreshSimpleHashTaskParam(
                    collectionId = c1
                )
            )
        ).toList()

        coVerify {
            simpleHashService.refreshContract(
                CollectionIdDto(
                    blockchain = BlockchainDto.ETHEREUM,
                    value = address.toString()
                )
            )
            collectionMetaRefreshSchedulingService.scheduleTask(match {
                it.collectionId == c1 && !it.withSimpleHash && it.full && !it.scheduled
            })
        }
    }
}