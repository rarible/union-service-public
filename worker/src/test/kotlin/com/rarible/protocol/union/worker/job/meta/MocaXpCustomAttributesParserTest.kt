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
                {"moca_id": "0", "moca_name": "#6475", "tribe": "connector", "total_xp": 73.6, "rank": 1277},
                {"moca_id": "1", "moca_name": "#6478", "tribe": "something", "total_xp": 56.1, "rank": 1277}
            ]
        """

        val result = MocaXpCustomAttributesParser.parse(json, collectionId)

        assertThat(result[0].id.fullId()).isEqualTo("ETHEREUM:0x59325733eb952a92e069c87f0a6168b29e80627f:0")
        assertThat(result[0].attributes).isEqualTo(
            listOf(UnionMetaAttribute("tribe", "connector"), UnionMetaAttribute("total_xp", "73.6"))
        )

        assertThat(result[1].id.fullId()).isEqualTo("ETHEREUM:0x59325733eb952a92e069c87f0a6168b29e80627f:1")
        assertThat(result[1].attributes).isEqualTo(
            listOf(UnionMetaAttribute("tribe", "something"), UnionMetaAttribute("total_xp", "56.1"))
        )
    }

}