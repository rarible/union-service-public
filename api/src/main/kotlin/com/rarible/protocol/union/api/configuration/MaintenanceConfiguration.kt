package com.rarible.protocol.union.api.configuration

import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories

@Configuration
@EnableReactiveMongoRepositories(basePackages = ["com.rarible.core.task"])
class MaintenanceConfiguration
