package com.rarible.protocol.union.worker.job.meta

import com.rarible.protocol.union.core.model.UnionMetaAttribute
import com.rarible.protocol.union.dto.parser.IdParser
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class MocaXpCustomAttributesParserTest {

    private val collectionId = IdParser.parseCollectionId("ETHEREUM:0x59325733eb952a92e069c87f0a6168b29e80627f")

    @Test
    fun `parse - ok`() {
        val json = """
            [
                {"moca_id": "0", "moca_name": "#6475", "tribe": "connector", "total_xp": 1000000.0, "rank": 1277},
                {"moca_id": "1", "moca_name": "#6478", "tribe": "something", "total_xp": 1000.0, "rank": 1277},
                {"moca_id": "2", "moca_name": "#6479", "tribe": "something", "total_xp": 1.0, "rank": 1277}
            ]
        """

        val result = MocaXpCustomAttributesParser.parse(json, collectionId)

        assertThat(result[0].id.fullId()).isEqualTo("${collectionId.fullId()}:0")
        assertThat(result[0].attributes).isEqualTo(
            listOf(
                UnionMetaAttribute("total_xp", "1000000.0"),
                UnionMetaAttribute("total_xp_percentage", "99.900")
            )
        )

        assertThat(result[1].id.fullId()).isEqualTo("${collectionId.fullId()}:1")
        assertThat(result[1].attributes).isEqualTo(
            listOf(
                UnionMetaAttribute("total_xp", "1000.0"),
                UnionMetaAttribute("total_xp_percentage", "0.100")
            )
        )

        assertThat(result[2].id.fullId()).isEqualTo("${collectionId.fullId()}:2")
        assertThat(result[2].attributes).isEqualTo(
            listOf(
                UnionMetaAttribute("total_xp", "1.0"),
                UnionMetaAttribute("total_xp_percentage", "0.000")
            )
        )
    }

}