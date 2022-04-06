package com.rarible.protocol.union.enrichment.evaluator

import com.rarible.protocol.union.dto.OwnershipSourceDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class OwnershipSourceComparatorTest {

    @Test
    fun `null is not preferred`() {
        assertThat(OwnershipSourceComparator.getPreferred(null, OwnershipSourceDto.TRANSFER))
            .isEqualTo(OwnershipSourceDto.TRANSFER)

        assertThat(OwnershipSourceComparator.getPreferred(null, OwnershipSourceDto.PURCHASE))
            .isEqualTo(OwnershipSourceDto.PURCHASE)

        assertThat(OwnershipSourceComparator.getPreferred(null, OwnershipSourceDto.MINT))
            .isEqualTo(OwnershipSourceDto.MINT)
    }

    @Test
    fun `current is preferred`() {
        assertThat(OwnershipSourceComparator.getPreferred(OwnershipSourceDto.MINT, OwnershipSourceDto.TRANSFER))
            .isEqualTo(OwnershipSourceDto.MINT)

        assertThat(OwnershipSourceComparator.getPreferred(OwnershipSourceDto.MINT, OwnershipSourceDto.PURCHASE))
            .isEqualTo(OwnershipSourceDto.MINT)

        assertThat(OwnershipSourceComparator.getPreferred(OwnershipSourceDto.PURCHASE, OwnershipSourceDto.TRANSFER))
            .isEqualTo(OwnershipSourceDto.PURCHASE)
    }

    @Test
    fun `updated is preferred`() {
        assertThat(OwnershipSourceComparator.getPreferred(OwnershipSourceDto.TRANSFER, OwnershipSourceDto.MINT))
            .isEqualTo(OwnershipSourceDto.MINT)

        assertThat(OwnershipSourceComparator.getPreferred(OwnershipSourceDto.PURCHASE, OwnershipSourceDto.MINT))
            .isEqualTo(OwnershipSourceDto.MINT)

        assertThat(OwnershipSourceComparator.getPreferred(OwnershipSourceDto.TRANSFER, OwnershipSourceDto.PURCHASE))
            .isEqualTo(OwnershipSourceDto.PURCHASE)
    }

}