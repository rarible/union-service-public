package com.rarible.protocol.union.integration.immutablex.scanner

import com.rarible.core.common.nowMillis
import com.rarible.protocol.union.core.test.ManualTest
import com.rarible.protocol.union.integration.ImxManualTest
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import scalether.domain.Address
import java.time.Instant

@ManualTest
class ImxEventsApiMt : ImxManualTest() {

    private val eventsApi = ImxEventsApi(activityClient, assetClient, orderClient, collectionClient)

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

    // Just to ensure this request works
    @Test
    fun getAssets() = runBlocking<Unit> {
        val result = eventsApi.assets(
            Instant.parse("2022-08-15T00:10:55.891Z"), "0xd5f5c0c7b335dcb63488f73e022ddf9c11df524a:206234"
        )
        println(result.size)
    }

    // Just to ensure this request works
    @Test
    fun getOrders() = runBlocking<Unit> {
        val result = eventsApi.orders(nowMillis(), "0")
        assertThat(result).isEmpty()
    }

    // Just to ensure this request works
    @Test
    fun getCollections() = runBlocking<Unit> {
        val result = eventsApi.collections(nowMillis(), Address.ZERO().prefixed())
        assertThat(result).isEmpty()
    }
}