package com.rarible.protocol.union.enrichment.meta.simplehash

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.enrichment.meta.simplehash.resolver.SimpleHashArtBlocksResolver
import com.rarible.protocol.union.enrichment.meta.simplehash.resolver.SimpleHashResolver
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class SimpleHashConverterService {

    private val logger = LoggerFactory.getLogger(javaClass)

    val mapper: ObjectMapper
        get() = ObjectMapper().run {
            registerKotlinModule()
            registerModule(JavaTimeModule())
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            setSerializationInclusion(JsonInclude.Include.NON_NULL)
        }

    val orderedResolvers = resolvers()

    fun convert(source: SimpleHashItem): UnionMeta {
        for (resolver in orderedResolvers) {
            try {
                if (resolver.support(source)) {
                    val itemProperties = resolver.convert(source)
                    if (itemProperties != null) {
                        return itemProperties
                    }
                }
            } catch (e: Exception) {
                logger.warn("Failed to resolve item=${source.nftId} using ${javaClass.name}: ${e.message}", e)
            }
        }

        throw RuntimeException("All resolvers failed")
    }

    fun convertRawToUnionMeta(json: String): UnionMeta {
        return convert(convertRawToSimpleHashItem(json))
    }

    fun convertRawToSimpleHashItem(json: String): SimpleHashItem {
        return mapper.readValue<SimpleHashItem>(json)
    }

    fun resolvers(): List<SimpleHashResolver> = listOf(
        SimpleHashArtBlocksResolver(mapper),
        SimpleHashResolver(mapper)
    )
}
