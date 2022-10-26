package com.rarible.protocol.union.core.util

import com.rarible.protocol.union.core.exception.UnionValidationException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PageSizeTest {

    @Test
    fun `incorrect page size`() {
        // Not allowed
        org.junit.jupiter.api.assertThrows<UnionValidationException> { (PageSize.OWNERSHIP.limit(-1)) }
        org.junit.jupiter.api.assertThrows<UnionValidationException> { (PageSize.OWNERSHIP.limit(0)) }
    }

    @Test
    fun `limited page size`() {
        // Replaced by maximum
        assertThat(PageSize.OWNERSHIP.limit(10000)).isEqualTo(1000)
    }

}