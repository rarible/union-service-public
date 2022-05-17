package com.rarible.protocol.union.core.converter

import com.rarible.protocol.union.core.test.randomOwnership
import com.rarible.protocol.union.core.test.randomOwnershipId
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OwnershipDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

internal class EsOwnershipConverterTest {

    private val converter = EsOwnershipConverter

    @ParameterizedTest
    @MethodSource("source")
    fun `should convert`(source: OwnershipDto) {
        val result = converter.convert(source)
        assertThat(result.ownershipId).isEqualTo(source.id.fullId())
        assertThat(result.blockchain).isEqualTo(source.blockchain)
        assertThat(result.itemId).isEqualTo(source.itemId?.fullId())
        assertThat(result.collection).isEqualTo(source.collection?.fullId())
        assertThat(result.owner).isEqualTo(source.owner.fullId())
        assertThat(result.date).isEqualTo(source.createdAt)
    }

    companion object {
        @JvmStatic
        fun source(): Stream<Arguments> = BlockchainDto.values().map {
            randomOwnership(id = randomOwnershipId(it))
        }.map { Arguments.of(it) }.stream()
    }
}
