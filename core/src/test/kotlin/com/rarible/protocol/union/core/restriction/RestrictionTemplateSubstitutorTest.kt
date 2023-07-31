package com.rarible.protocol.union.core.restriction

import com.rarible.core.test.data.randomString
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ItemIdDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class RestrictionTemplateSubstitutorTest {

    @Test
    fun `substitute item values and params`() {
        val itemId = ItemIdDto(BlockchainDto.ETHEREUM, randomString())
        val substitutor = RestrictionTemplateSubstitutor(itemId, mapOf("param1" to "abc"))

        val itemIdAndParams = substitutor.substitute("test.com/\${itemId}?param1=\${param1}&param2=\${param2}")

        assertThat(itemIdAndParams).isEqualTo("test.com/${itemId.value}?param1=abc&param2=")
    }
}
