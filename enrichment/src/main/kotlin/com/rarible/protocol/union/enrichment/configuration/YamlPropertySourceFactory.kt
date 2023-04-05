package com.rarible.protocol.union.enrichment.configuration

import org.springframework.beans.factory.config.YamlPropertiesFactoryBean
import org.springframework.core.env.PropertiesPropertySource
import org.springframework.core.env.PropertySource
import org.springframework.core.io.support.EncodedResource
import org.springframework.core.io.support.PropertySourceFactory
import org.springframework.lang.Nullable
import java.util.Properties

class YamlPropertySourceFactory : PropertySourceFactory {
    override fun createPropertySource(@Nullable name: String?, encodedResource: EncodedResource): PropertySource<*> {
        val factory = YamlPropertiesFactoryBean()
        factory.setResources(encodedResource.resource)
        val properties: Properties = factory.getObject()!!
        return PropertiesPropertySource(encodedResource.resource.filename!!, properties)
    }
}
