package com.rarible.protocol.union.core.continuation

import com.rarible.protocol.union.dto.continuation.Continuation
import com.rarible.protocol.union.dto.continuation.ContinuationFactory
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ArgPagingTest {

    @Test
    fun `first page - final slice`() {
        val firstArgSlice = ArgSlice(
            "a", null,
            Slice(null, listOf(TestEntity("ID1", 10, 50)))
        )

        val secondArgSlice = ArgSlice(
            "b", null,
            Slice(null, listOf(TestEntity("ID2", 8, 60)))
        )

        val result = ArgPaging(
            ByOriginalAndIdAsc, BySellFinalAndIdAsc, listOf(firstArgSlice, secondArgSlice)
        ).getSlice(2)

        assertThat(result.entities).hasSize(2)
        assertThat(result.continuation).isNull()
    }

    @Test
    fun `first page - with continuation in the middle`() {
        val firstArgEntity1 = TestEntity("ID1", 10, 70)
        val firstArgEntity2 = TestEntity("ID2", 15, 105)
        // Expected ID1 in result
        val firstArgSlice = ArgSlice(
            "a", null,
            Slice(null, listOf(firstArgEntity1, firstArgEntity2))
        )

        val secondArgEntity1 = TestEntity("ID5", 7, 60)
        val secondArgEntity2 = TestEntity("ID9", 12, 90)
        // Expected ID5 in result
        val secondArgSlice = ArgSlice(
            "b", null,
            Slice(null, listOf(secondArgEntity1, secondArgEntity2))
        )

        val result = ArgPaging(
            ByOriginalAndIdAsc, BySellFinalAndIdAsc, listOf(firstArgSlice, secondArgSlice)
        ).getSlice(2)

        assertThat(result.entities).hasSize(2)
        assertThat(result.entities[0]).isEqualTo(secondArgEntity1)
        assertThat(result.entities[1]).isEqualTo(firstArgEntity1)
        assertThat(result.continuation).isEqualTo("a:10_ID1;b:7_ID5")
    }

    @Test
    fun `first page - only one arg in final slice`() {
        val firstArgEntity1 = TestEntity("ID1", 10, 70)
        val firstArgEntity2 = TestEntity("ID2", 15, 105)
        // Expected ID1 and ID2 in result
        val firstArgSlice = ArgSlice(
            "a", null,
            Slice(null, listOf(firstArgEntity1, firstArgEntity2))
        )

        val secondArgEntity1 = TestEntity("ID5", 25, 120)
        // Expected nothing from this slice in result
        val secondArgSlice = ArgSlice(
            "b", null,
            Slice(null, listOf(secondArgEntity1))
        )

        val result = ArgPaging(
            ByOriginalAndIdAsc, BySellFinalAndIdAsc, listOf(firstArgSlice, secondArgSlice)
        ).getSlice(2)

        assertThat(result.entities).hasSize(2)
        assertThat(result.entities[0]).isEqualTo(firstArgEntity1)
        assertThat(result.entities[1]).isEqualTo(firstArgEntity2)
        // b should not be in continuation, it still has initial value as null
        assertThat(result.continuation).isEqualTo("a:COMPLETED")
    }

    @Test
    fun `second page - with NULL continuation`() {
        val firstArgEntity1 = TestEntity("ID10", 15, 105)
        val firstArgEntity2 = TestEntity("ID11", 30, 210)
        // Expected ID10 in result
        val firstArgSlice = ArgSlice(
            "a", "10_ID1",
            Slice(null, listOf(firstArgEntity1, firstArgEntity2))
        )

        val secondArgEntity1 = TestEntity("ID13", 25, 120)
        // Arg 'b' haven't been requested before, but has only 1 record = COMPLETED expected
        val secondArgSlice = ArgSlice(
            "b", null,
            Slice(null, listOf(secondArgEntity1))
        )

        val result = ArgPaging(
            ByOriginalAndIdAsc, BySellFinalAndIdAsc, listOf(firstArgSlice, secondArgSlice)
        ).getSlice(2)

        assertThat(result.entities).hasSize(2)
        assertThat(result.entities[0]).isEqualTo(firstArgEntity1)
        assertThat(result.entities[1]).isEqualTo(secondArgEntity1)
        // Both args handled, continuation should be null
        assertThat(result.continuation).isEqualTo("a:15_ID10;b:COMPLETED")
    }

    @Test
    fun `second page - with same continuation`() {
        val firstArgEntity1 = TestEntity("ID10", 15, 105)
        val firstArgEntity2 = TestEntity("ID11", 30, 210)
        // Expected both in result
        val firstArgSlice = ArgSlice(
            "a", "10_ID1",
            Slice(null, listOf(firstArgEntity1, firstArgEntity2))
        )

        val secondArgEntity1 = TestEntity("ID13", 25, 320)
        // This entity will not get into final slice, but have to keep it's continuation
        val secondArgSlice = ArgSlice(
            "b", "20_ID12",
            Slice(null, listOf(secondArgEntity1))
        )

        val result = ArgPaging(
            ByOriginalAndIdAsc, BySellFinalAndIdAsc, listOf(firstArgSlice, secondArgSlice)
        ).getSlice(2)

        assertThat(result.entities).hasSize(2)
        assertThat(result.entities[0]).isEqualTo(firstArgEntity1)
        assertThat(result.entities[1]).isEqualTo(firstArgEntity2)
        // 'a' completed, but 'b' has previous continuation
        assertThat(result.continuation).isEqualTo("a:COMPLETED;b:20_ID12")
    }

    @Test
    fun `last page - with COMPLETED arg continuation`() {
        val firstArgEntity1 = TestEntity("ID10", 10, 70)
        // Since for arg 'a' we got only 1 record, it should be considered as last page
        val firstArgSlice = ArgSlice(
            "a", "10_ID1",
            Slice(null, listOf(firstArgEntity1))
        )

        // Arg 'b' already completed
        val secondArgSlice = ArgSlice(
            "b", "COMPLETED",
            Slice(null, listOf<TestEntity>())
        )

        val result = ArgPaging(
            ByOriginalAndIdAsc, BySellFinalAndIdAsc, listOf(firstArgSlice, secondArgSlice)
        ).getSlice(2)

        assertThat(result.entities).hasSize(1)
        assertThat(result.entities[0]).isEqualTo(firstArgEntity1)
        // Both args handled, continuation should be null
        assertThat(result.continuation).isNull()
    }

    private data class TestEntity(
        val id: String,
        val originalValue: Int,
        val finalValue: Int
    )

    private class TestContinuation(
        val id: String,
        val value: Int
    ) : Continuation<TestContinuation> {
        override fun compareTo(other: TestContinuation): Int {
            val result = this.value.compareTo(other.value)
            return if (result != 0) result else id.compareTo(other.id)
        }

        override fun toString(): String {
            return "${value}_${id}"
        }
    }

    private object BySellFinalAndIdAsc : ContinuationFactory<TestEntity, TestContinuation> {
        override fun getContinuation(entity: TestEntity): TestContinuation {
            return TestContinuation(entity.id, entity.finalValue)
        }
    }

    private object ByOriginalAndIdAsc : ContinuationFactory<TestEntity, TestContinuation> {
        override fun getContinuation(entity: TestEntity): TestContinuation {
            return TestContinuation(entity.id, entity.originalValue)
        }
    }
}