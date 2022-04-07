package com.rarible.protocol.union.search.core.config

import org.springframework.boot.autoconfigure.AutoConfigurationPackage
import org.springframework.context.annotation.Configuration
import org.springframework.data.elasticsearch.repository.config.EnableReactiveElasticsearchRepositories


@Configuration
@AutoConfigurationPackage
@EnableReactiveElasticsearchRepositories(basePackages = [
    "com.rarible.protocol.union.search"
])
class SearchConfiguration