package com.rarible.protocol.union.enrichment.meta.item.trait

import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.enrichment.meta.item.ItemChangeListener
import com.rarible.protocol.union.enrichment.model.EnrichmentCollectionId
import com.rarible.protocol.union.enrichment.model.ItemAttributeCountChange
import com.rarible.protocol.union.enrichment.model.ItemAttributeShort
import com.rarible.protocol.union.enrichment.model.ItemChangeEvent
import com.rarible.protocol.union.enrichment.model.ItemState
import com.rarible.protocol.union.enrichment.repository.CollectionRepository
import com.rarible.protocol.union.enrichment.service.TraitService
import com.rarible.protocol.union.enrichment.util.toItemAttributeShort
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class TraitItemChangeListener(
    private val collectionRepository: CollectionRepository,
    private val traitService: TraitService,
) : ItemChangeListener {

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
            handleItemChange(
                collection = newCollection!!,
                oldAttributes = oldAttributes,
                newAttributes = newAttributes,
                oldListed = oldListed,
                newListed = newListed
            )
        } else {
            handleCollectionChange(
                newCollection = newCollection,
                oldCollection = oldCollection,
                oldAttributes = oldAttributes,
                newAttributes = newAttributes,
                oldListed = oldListed,
                newListed = newListed
            )
        }
    }

    private suspend fun handleItemChange(
        collection: CollectionIdDto,
        oldAttributes: Set<ItemAttributeShort>?,
        newAttributes: Set<ItemAttributeShort>?,
        oldListed: Boolean,
        newListed: Boolean
    ) {
        if (oldAttributes != newAttributes) {
            onAttributesChange(
                collection = collection,
                oldAttributes = oldAttributes,
                newAttributes = newAttributes,
                oldListed = oldListed,
                newListed = newListed
            )
        } else if (newListed != oldListed && newAttributes != null) {
            onListedChange(
                collection = collection,
                attributes = newAttributes,
                listed = newListed
            )
        }
    }

    private suspend fun handleCollectionChange(
        newCollection: CollectionIdDto?,
        oldCollection: CollectionIdDto?,
        oldAttributes: Set<ItemAttributeShort>?,
        newAttributes: Set<ItemAttributeShort>?,
        oldListed: Boolean,
        newListed: Boolean
    ) {
        if (newCollection != null && newAttributes != null) {
            changeItemsCount(
                collectionId = newCollection,
                attributes = newAttributes.map {
                    ItemAttributeCountChange(
                        attribute = it,
                        totalChange = 1,
                        listedChange = if (newListed) 1 else 0
                    )
                }.toSet()
            )
        }
        if (oldCollection != null && oldAttributes != null) {
            changeItemsCount(
                collectionId = oldCollection,
                oldAttributes.map {
                    ItemAttributeCountChange(
                        attribute = it,
                        totalChange = -1,
                        listedChange = if (oldListed) -1 else 0
                    )
                }.toSet()
            )
        }
    }

    private suspend fun onAttributesChange(
        collection: CollectionIdDto,
        oldAttributes: Set<ItemAttributeShort>?,
        newAttributes: Set<ItemAttributeShort>?,
        oldListed: Boolean,
        newListed: Boolean
    ) {
        when {
            newAttributes == null -> changeItemsCount(
                collectionId = collection,
                attributes = oldAttributes!!.map {
                    ItemAttributeCountChange(
                        attribute = it,
                        totalChange = -1,
                        listedChange = if (oldListed) -1 else 0
                    )
                }.toSet()
            )

            oldAttributes == null -> changeItemsCount(
                collectionId = collection,
                attributes = newAttributes.map {
                    ItemAttributeCountChange(
                        attribute = it,
                        totalChange = 1,
                        listedChange = if (newListed) 1 else 0
                    )
                }.toSet()
            )

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

                changeItemsCount(collection, changes)
            }
        }
    }

    private suspend fun onListedChange(
        collection: CollectionIdDto,
        attributes: Set<ItemAttributeShort>,
        listed: Boolean
    ) {
        changeItemsCount(
            collectionId = collection,
            attributes = attributes.map {
                ItemAttributeCountChange(
                    attribute = it,
                    totalChange = 0,
                    listedChange = if (listed) 1 else -1
                )
            }.toSet()
        )
    }

    private fun calcProperties(item: ItemState?): ItemPropertiesForTraitStatistics =
        item?.takeIfNotDeleted()
            ?.let {
                ItemPropertiesForTraitStatistics(
                    collection = it.collectionId?.let { collectionId ->
                        CollectionIdDto(it.blockchain, collectionId)
                    },
                    attributes = it.attributes
                        ?.mapNotNull { attribute -> attribute.toItemAttributeShort() }
                        ?.toSet()
                )
            } ?: ItemPropertiesForTraitStatistics(collection = null, attributes = null)

    private suspend fun changeItemsCount(
        collectionId: CollectionIdDto,
        attributes: Set<ItemAttributeCountChange>,
    ) {
        val collection = collectionRepository.get(EnrichmentCollectionId(collectionId)) ?: return
        if (!collection.hasTraits || attributes.isEmpty()) return
        logger.info("Changes itemsCount in traits for collection: $collectionId and attributes: $attributes")
        traitService.changeItemsCount(collectionId, attributes)
    }

    private data class ItemPropertiesForTraitStatistics(
        val collection: CollectionIdDto?,
        val attributes: Set<ItemAttributeShort>?,
    ) {
        init {
            require(attributes == null || collection != null)
        }
    }

    private fun ItemState.takeIfNotDeleted(): ItemState? =
        if (this.deleted) null else this

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(TraitItemChangeListener::class.java)
    }
}
