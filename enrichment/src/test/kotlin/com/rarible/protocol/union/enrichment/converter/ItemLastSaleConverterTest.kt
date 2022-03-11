package com.rarible.protocol.union.enrichment.converter

import com.rarible.protocol.union.enrichment.test.data.randomUnionActivitySale
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ItemLastSaleConverterTest {

    @Test
    fun convert() {
        val sell = randomUnionActivitySale(randomEthItemId())

        val lastSale = ItemLastSaleConverter.convert(sell)!!

        assertThat(lastSale.date).isEqualTo(sell.date)
        assertThat(lastSale.seller).isEqualTo(sell.seller)
        assertThat(lastSale.buyer).isEqualTo(sell.buyer)
        assertThat(lastSale.currency).isEqualTo(sell.payment.type)
        assertThat(lastSale.value).isEqualTo(sell.nft.value)
        assertThat(lastSale.price).isEqualTo(sell.price)
    }

}