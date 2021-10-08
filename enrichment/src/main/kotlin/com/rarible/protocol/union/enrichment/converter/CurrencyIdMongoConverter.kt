package com.rarible.protocol.union.enrichment.converter

import com.rarible.core.mongo.converter.SimpleMongoConverter
import com.rarible.protocol.union.enrichment.model.CurrencyId
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component
import java.util.*

@Component
class CurrencyIdMongoConverter : SimpleMongoConverter<String, CurrencyId> {
    override fun getFromMongoConverter(): Converter<String, CurrencyId> {
        return StringToCurrencyIdConverter()
    }

    override fun getToMongoConverter(): Converter<CurrencyId, String> {
        return CurrencyIdToStringConverter()
    }

    override fun isSimpleType(aClass: Class<*>): Boolean {
        return aClass == CurrencyId::class.java
    }

    override fun getCustomWriteTarget(sourceType: Class<*>): Optional<Class<*>> {
        return if (sourceType == CurrencyId::class.java) {
            Optional.of(String::class.java)
        } else Optional.empty()
    }

    class CurrencyIdToStringConverter: Converter<CurrencyId, String> {
        override fun convert(source: CurrencyId): String {
            return source.toString()
        }
    }

    class StringToCurrencyIdConverter: Converter<String, CurrencyId> {
        override fun convert(source: String): CurrencyId {
            return CurrencyId.fromString(source)
        }
    }
}
