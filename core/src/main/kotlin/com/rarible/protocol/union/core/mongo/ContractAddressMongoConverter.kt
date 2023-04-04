package com.rarible.protocol.union.core.mongo

import com.rarible.core.mongo.converter.SimpleMongoConverter
import com.rarible.protocol.union.dto.ContractAddress
import com.rarible.protocol.union.dto.parser.IdParser
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component
import java.util.Optional

@Component
class ContractAddressMongoConverter : SimpleMongoConverter<String, ContractAddress> {

    override fun isSimpleType(aClass: Class<*>?) = aClass == ContractAddress::class.java

    override fun getCustomWriteTarget(sourceType: Class<*>?): Optional<Class<*>> {
        return if (sourceType == ContractAddress::class.java) {
            Optional.of(String::class.java)
        } else Optional.empty()
    }

    override fun getFromMongoConverter(): Converter<String, ContractAddress> = ContractAddressReadConverter
    override fun getToMongoConverter(): Converter<ContractAddress, String> = ContractAddressWriteConverter

    object ContractAddressReadConverter : Converter<String, ContractAddress> {

        override fun convert(source: String) = IdParser.parseContract(source)
    }

    object ContractAddressWriteConverter : Converter<ContractAddress, String> {

        override fun convert(source: ContractAddress) = source.fullId()
    }
}
