package com.rarible.protocol.union.core.domain

import com.rarible.core.common.Identifiable
import com.rarible.domain.CarbonNegativeStatus
import com.rarible.domain.CarbonNegativeStatusInfo
import com.rarible.domain.Ownership
import com.rarible.marketplace.core.model.Blockchain
import com.rarible.marketplace.core.model.BlockchainAddress
import com.rarible.marketplace.core.model.toBlockchainAddress
import com.rarible.marketplace.core.model.toBlockchainAddressSafe
import com.rarible.protocol.union.core.domain.Item.Companion.ITEM_COLLECTION
import org.springframework.data.annotation.AccessType
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Version
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.CompoundIndexes
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.math.BigDecimal
import java.math.BigDecimal.ZERO
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Date

@Document(ITEM_COLLECTION)
@CompoundIndexes(
    CompoundIndex(def = "{token: 1, tokenId: 1}", background = true, unique = true, sparse = true)
)
data class Item(
    @Id
    override val id: String,
    val token: BlockchainAddress,
    val tokenId: String,

    /** Создатель айтема. */
    @Indexed(sparse = true, background = true)
    val creator: BlockchainAddress? = null,
    /** Сколько всего айтемов создано (существует). */
    val supply: BigDecimal = BigDecimal.ONE,
    /** Сколько айтемов из общего количества ([supply]) являются лейзи. */
    val lazySupply: BigDecimal = ZERO,
    /** Метаданные айтема. */
    val meta: ItemMeta? = null,
    @Deprecated("Request from protocol")
    val royalties: List<Royalty> = emptyList(),
    /** Лучший бид (оффер) на данный айтем (самый дорогой с учетом платформы, приоритет у офферов с Rarible). */
    val offerV2: Order? = null,
    /** Владелец лучшего ордера на продаже (самого дешевого). */
    val ownership: Ownership? = null,

    /** Статусы всех овнершипов данного айтема. */
    val statuses: Set<Ownership.Status> = emptySet(),
    /** Сколько владельцев сейчас продают данный айтем. */
    val sellers: Int = 0,
    /** Сколько айтемов сейчас находится на продаже. */
    val totalStock: BigDecimal = ZERO,
    /** Дата для сортировки в Explore. Зависит от множества параметров. */
    val sortDate: Date? = null,
    /** Находится ли хотя бы один экземпляр айтема не в руках создателя. */
    val haveSecondaryOwner: Boolean = false,

    val unlockable: Boolean = false,
    val categories: Set<String> = emptySet(),
    val verified: Boolean = false,
    val blacklisted: Boolean = false,
    val likes: Long = 0,
    val visitCounters: VisitCounters = VisitCounters(),
    val lastIndexDate: Instant? = null,

    val deleted: Boolean = false,

    val blockchain: Blockchain = token.blockchain,
    val carbonNegativeStatus: CarbonNegativeStatus = CarbonNegativeStatus.NON_CARBON_NEGATIVE,
    val carbonNegativeStatusInfo: CarbonNegativeStatusInfo? = null,
    val hide: Boolean? = false,
    val mintedAt: Date? = null,

    @Version
    val version: Long? = null,
    @CreatedDate
    val createdDate: Date? = null,
) : Identifiable<String> {

    /** Флаг, находится ли айтем на вторичной продаже. */
    var onSecondarySale: Boolean
        @AccessType(AccessType.Type.PROPERTY)
        get() = sellers > 1 || ownership != null && ownership.owner != creator
        set(onSecondarySale: Boolean) {}

    companion object {
        private const val ID_PARTS_SEPARATOR = ":"
        const val ITEM_COLLECTION = "item"
        const val MAX_ID_LENGTH = 512

        fun getId(token: BlockchainAddress, tokenId: String?): String = if (tokenId != null) {
            "$token$ID_PARTS_SEPARATOR$tokenId"
        } else {
            token.toString()
        }

        fun isValidId(value: String): Boolean {
            val parts = value.split(ID_PARTS_SEPARATOR)
            if (parts.size != 2) {
                return false
            }
            return parts[0].toBlockchainAddressSafe() != null
        }

        fun parseId(id: String): Pair<BlockchainAddress, String> {
            if (!isValidId(id)) throw IllegalArgumentException("Incorrect format of itemId: $id")

            val parts = id.split(ID_PARTS_SEPARATOR)
            return Pair(parts[0].toBlockchainAddress(), parts[1])
        }

        fun empty(token: BlockchainAddress, tokenId: String): Item =
            Item(id = getId(token, tokenId), token = token, tokenId = tokenId, supply = ZERO)
    }
}

data class VisitCounters(
    /**
     * Счетчики количества визитов по дням.
     *
     * Пример:
     * ```
     * [
     *     { "date": "2021-11-29", "count": 1 },
     *     { "date": "2021-12-01", "count": 10 },
     *     { "date": "2021-12-02", "count": 15 }
     * ]
     * ```
     * Такая структура позволяет отбрасывать лишние значения (например, старше 7 дней).
     */
    val days: List<DateCounter> = emptyList()
) {
    /** Возвращает суммарное значение счетчика за последнюю неделю. */
    fun getWeekCount(): Long {
        val date = LocalDate.now().minusDays(7).asString()

        return days.asSequence()
            .filter { it.date >= date }
            .map { it.count }
            .fold(0L, Long::plus)
    }
}

data class DateCounter(
    val date: String,
    val count: Long
)
fun LocalDate.asString(): String = this.format(DateTimeFormatter.ISO_DATE)
