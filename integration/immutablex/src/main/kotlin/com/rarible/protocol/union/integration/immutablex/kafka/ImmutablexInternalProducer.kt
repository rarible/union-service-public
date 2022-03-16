package com.rarible.protocol.union.integration.immutablex.kafka

import com.rarible.core.kafka.KafkaMessage
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.protocol.union.integration.immutablex.dto.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach

class ImmutablexInternalProducer(
    private val mintProducer: RaribleKafkaProducer<ImmutablexMint>,
    private val transferProducer: RaribleKafkaProducer<ImmutablexTransfer>,
    private val tradingProducer: RaribleKafkaProducer<ImmutablexTrade>,
    private val depositsProducer: RaribleKafkaProducer<ImmutablexDeposit>,
    private val withdrawalsProducer: RaribleKafkaProducer<ImmutablexWithdrawal>
) {

    suspend fun mints(items: List<ImmutablexMint>) {
        mintProducer.send(items.map {
            KafkaMessage(
                key = "${it.transactionId}.${it.timestamp.toEpochMilli()}",
                value = it
            )
        }).onEach { it.ensureSuccess() }.collect()
    }

    suspend fun transfers(items: List<ImmutablexTransfer>) {
        transferProducer.send(items.map {
            KafkaMessage(
                key = "${it.transactionId}.${it.timestamp.toEpochMilli()}",
                value = it
            )
        }).onEach { it.ensureSuccess() }.collect()
    }

    suspend fun trades(items: List<ImmutablexTrade>) {
        tradingProducer.send(items.map {
            KafkaMessage(
                key = "${it.transactionId}.${it.timestamp.toEpochMilli()}",
                value = it
            )
        }).onEach { it.ensureSuccess() }.collect()
    }

    suspend fun deposits(items: List<ImmutablexDeposit>) {
        depositsProducer.send(items.map {
            KafkaMessage(
                key = "${it.transactionId}.${it.timestamp.toEpochMilli()}",
                value = it
            )
        }).onEach { it.ensureSuccess() }.collect()
    }

    suspend fun withdrawals(items: List<ImmutablexWithdrawal>) {
        withdrawalsProducer.send(items.map {
            KafkaMessage(
                key = "${it.transactionId}.${it.timestamp.toEpochMilli()}",
                value = it
            )
        }).onEach { it.ensureSuccess() }.collect()
    }


}
