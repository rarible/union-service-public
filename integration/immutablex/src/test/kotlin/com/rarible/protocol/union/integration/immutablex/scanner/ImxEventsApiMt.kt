package com.rarible.protocol.union.integration.immutablex.scanner

import com.rarible.protocol.union.core.test.ManualTest
import com.rarible.protocol.union.integration.ImmutablexManualTest
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

@ManualTest
class ImxEventsApiMt : ImmutablexManualTest() {

    private val eventsApi = ImxEventsApi(activityClient, orderClient)

    @Test
    fun getLastMint() = runBlocking<Unit> {
        val mint = eventsApi.lastMint()

        println(mint)
        assertThat(mint.transactionId).isGreaterThan(5344327L)
    }

    @Test
    fun getMintEvents() = runBlocking<Unit> {
        val mint = eventsApi.lastMint()
        val nothingLeft = eventsApi.mints(mint.transactionId.toString())
            .filter { it.transactionId < mint.transactionId }

        assertThat(nothingLeft).isEmpty()
    }

    @Test
    fun getLastTrade() = runBlocking<Unit> {
        val trade = eventsApi.lastTrade()

        println(trade)
        assertThat(trade.transactionId).isGreaterThan(5343119L)
    }

    @Test
    fun getTradeEvents() = runBlocking<Unit> {
        val trade = eventsApi.lastTrade()
        val nothingLeft = eventsApi.trades(trade.transactionId.toString())
            .filter { it.transactionId < trade.transactionId }

        assertThat(nothingLeft).isEmpty()
    }

    @Test
    fun getLastTransfer() = runBlocking<Unit> {
        val transfer = eventsApi.lastTransfer()

        println(transfer)
        assertThat(transfer.transactionId).isGreaterThan(5344295L)
    }

    @Test
    fun getTransferEvents() = runBlocking<Unit> {
        val transfer = eventsApi.lastTransfer()
        println(transfer)
        val nothingLeft = eventsApi.transfers(transfer.transactionId.toString())
            .filter { it.transactionId < transfer.transactionId }

        assertThat(nothingLeft).isEmpty()
    }
}