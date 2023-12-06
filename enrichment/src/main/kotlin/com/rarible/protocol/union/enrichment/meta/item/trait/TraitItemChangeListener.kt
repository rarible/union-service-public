package com.rarible.protocol.union.enrichment.meta.item.trait

import com.rarible.core.common.EventTimeMarks
import com.rarible.protocol.union.core.model.UnionMetaAttribute
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.enrichment.meta.item.ItemChangeListener
import com.rarible.protocol.union.enrichment.model.ItemAttributeCountChange
import com.rarible.protocol.union.enrichment.model.ItemAttributeShort
import com.rarible.protocol.union.enrichment.model.ShortItem
import com.rarible.protocol.union.enrichment.model.ItemChangeEvent
import com.rarible.protocol.union.enrichment.model.ItemState
import com.rarible.protocol.union.enrichment.util.toItemAttributeShort
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class TraitItemChangeListener : ItemChangeListener {

    override suspend fun onItemChange(change: ItemChangeEvent) {
        triggerTraits(current = change.current, updated = change.updated)
    }

    private suspend fun triggerTraits(
        current: ItemState?,
        updated: ItemState,
    ) {
        val (oldCollection, oldAttributes) = calcProperties(current)
        val (newCollection, newAttributes) = calcProperties(updated)
        val newListed = updated.isListed
        val oldListed = current?.isListed ?: false

        if (oldCollection == newCollection) {
            if (oldAttributes != newAttributes) {
                when {
                    newAttributes == null -> changeItemsCount(newCollection!!, oldAttributes!!.map {
                        ItemAttributeCountChange(
                            attribute = it,
                            totalChange = -1,
                            listedChange = if (oldListed) -1 else 0
                        )
                    }.toSet(), eventTimeMarks)

                    oldAttributes == null -> changeItemsCount(newCollection!!, newAttributes.map {
                        ItemAttributeCountChange(
                            attribute = it,
                            totalChange = 1,
                            listedChange = if (newListed) 1 else 0
                        )
                    }.toSet(), eventTimeMarks)

                    else -> {
                        val removedAttributes = oldAttributes - newAttributes
                        val addedAttributes = newAttributes - oldAttributes
                        val allAttributes = newAttributes + oldAttributes
                        val changes = allAttributes.map {
                            ItemAttributeCountChange(
                                attribute = it,
                                totalChange = if (addedAttributes.contains(it)) {
                                    1L
                                } else if (removedAttributes.contains(it)) {
                                    -1L
                                } else {
                                    0L
                                },
                                listedChange = if (newListed && (!oldListed && !removedAttributes.contains(it) ||
                                        addedAttributes.contains(it))
                                ) {
                                    1
                                } else if (oldListed && (!newListed && !addedAttributes.contains(it) ||
                                        removedAttributes.contains(it))
                                ) {
                                    -1
                                } else {
                                    0
                                }
                            )
                        }.filter { it.listedChange != 0L || it.totalChange != 0L }.toSet()

                        changeItemsCount(newCollection!!, changes, eventTimeMarks)
                    }
                }
            } else if (newListed != oldListed && newAttributes != null) {
                changeItemsCount(newCollection!!, newAttributes.map {
                    ItemAttributeCountChange(
                        attribute = it,
                        totalChange = 0,
                        listedChange = if (newListed) 1 else -1
                    )
                }.toSet(), eventTimeMarks)
            }
        } else {
            if (newCollection != null && newAttributes != null) {
                changeItemsCount(newCollection, newAttributes.map {
                    ItemAttributeCountChange(
                        attribute = it,
                        totalChange = 1,
                        listedChange = if (newListed) 1 else 0
                    )
                }.toSet(), eventTimeMarks)
            }
            if (oldCollection != null && oldAttributes != null) {
                changeItemsCount(oldCollection, oldAttributes.map {
                    ItemAttributeCountChange(
                        attribute = it,
                        totalChange = -1,
                        listedChange = if (oldListed) -1 else 0
                    )
                }.toSet(), eventTimeMarks)
            }
        }
    }

    private fun calcProperties(item: ShortItem?): ItemPropertiesForTraitStatistics =
        item
            ?.let {
                ItemPropertiesForTraitStatistics(
                    collection = it.metaEntry?.data?.collectionId?.let { collectionId ->
                        CollectionIdDto(it.blockchain, collectionId)
                    },
                    attributes = it.metaEntry?.data?.attributes
                        ?.mapNotNull { attribute -> attribute.toItemAttributeShort() }
                        ?.toSet()
                )
            } ?: ItemPropertiesForTraitStatistics(collection = null, attributes = null)

    private suspend fun changeItemsCount(
        collectionId: CollectionIdDto,
        attributes: List<UnionMetaAttribute>,
    ) {
        val collection = collectionDao.getOrNull(collectionId) ?: return

        if (!collection.hasTraits || attributes.isEmpty()) return

        logger.info("Changes itemsCount in traits for collection: $collectionId and attributes: $attributes")
        traitService.changeItemsCount(collectionId, attributes, eventTimeMarks)
    }

    private suspend fun

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(TraitItemChangeListener::class.java)
    }

    private data class ItemPropertiesForTraitStatistics(
        val collection: String?,
        val attributes: Set<ItemAttributeShort>?,
    ) {
        init {
            require(attributes == null || collection != null)
        }
    }
}
