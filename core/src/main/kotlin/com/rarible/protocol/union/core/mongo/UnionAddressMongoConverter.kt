package com.rarible.protocol.union.core.mongo

import com.rarible.core.mongo.converter.SimpleMongoConverter
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.dto.parser.IdParser
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component
import java.util.Optional

@Component
class UnionAddressMongoConverter : SimpleMongoConverter<String, UnionAddress> {

    override fun isSimpleType(aClass: Class<*>?) = aClass == UnionAddress::class.java

    override fun getCustomWriteTarget(sourceType: Class<*>?): Optional<Class<*>> {
        return if (sourceType == UnionAddress::class.java) {
            Optional.of(String::class.java)
        } else Optional.empty()
    }

    override fun getFromMongoConverter(): Converter<String, UnionAddress> = UnionAddressReadConverter
    override fun getToMongoConverter(): Converter<UnionAddress, String> = UnionAddressWriteConverter

    object UnionAddressReadConverter : Converter<String, UnionAddress> {

        override fun convert(source: String) = IdParser.parseAddress(source)
    }

    object UnionAddressWriteConverter : Converter<UnionAddress, String> {

        override fun convert(source: UnionAddress) = source.fullId()
    }
}
