package com.rarible.protocol.union.enrichment.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.ReadingConverter
import org.springframework.data.convert.WritingConverter
import org.springframework.data.elasticsearch.core.convert.ElasticsearchCustomConversions
import java.math.BigInteger

@Configuration
class SearchConfiguration  {
    @Bean
    fun elasticsearchCustomConversions(): ElasticsearchCustomConversions {
        return ElasticsearchCustomConversions(listOf(BigIntegerWriter(), BigIntegerReader()))
    }

    @WritingConverter
    internal class BigIntegerWriter : Converter<BigInteger, String> {
        override fun convert(source: BigInteger) = source.toString()

    }

    @ReadingConverter
    internal class BigIntegerReader : Converter<String, BigInteger> {
        override fun convert(source: String) = BigInteger(source)
    }
}