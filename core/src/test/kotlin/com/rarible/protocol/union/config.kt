package com.rarible.protocol.union

import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.core.test.containers.KafkaTestContainer
import com.rarible.core.test.containers.MongodbTestContainer
import com.rarible.protocol.union.TestCommonConfiguration.Companion.kafkaContainer
import com.rarible.protocol.union.dto.ItemEventDto
import com.rarible.protocol.union.dto.UnionEventTopicProvider
import com.rarible.protocol.union.subscriber.UnionKafkaJsonSerializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.task.TaskSchedulingAutoConfiguration
import org.springframework.boot.env.EnvironmentPostProcessor
import org.springframework.boot.env.YamlPropertySourceLoader
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.FilterType
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.PropertiesPropertySource
import org.springframework.core.io.ClassPathResource
import java.net.ServerSocket
import java.util.*

@ComponentScan(
    basePackageClasses = [TestCommonConfiguration::class],
    excludeFilters = [ComponentScan.Filter(value = [Configuration::class], type = FilterType.ANNOTATION)]
)

@EnableAutoConfiguration(exclude = [TaskSchedulingAutoConfiguration::class])
class TestCommonConfiguration(@Value("\${application.environment:dev}") private val applicationEnvironment: String) {

    @Bean
    fun itemEventProducer(): RaribleKafkaProducer<ItemEventDto> {
        val topic = UnionEventTopicProvider.getItemTopic(applicationEnvironment)
        return createUnionProducer("item", topic, ItemEventDto::class.java)
    }

    private fun <T> createUnionProducer(clientSuffix: String, topic: String, type: Class<T>): RaribleKafkaProducer<T> {
        return RaribleKafkaProducer(
            clientId = "$applicationEnvironment.protocol-union-listener.$clientSuffix",
            valueSerializerClass = UnionKafkaJsonSerializer::class.java,
            valueClass = type,
            defaultTopic = topic,
            bootstrapServers = kafkaContainer.kafkaBoostrapServers()
        )
    }

    companion object {
        val mongodbContainer = MongodbTestContainer()
        val kafkaContainer = KafkaTestContainer()
    }
}

class TestConfigLoader : EnvironmentPostProcessor {
    init {
        val socket = ServerSocket(0)
        socket.close()
    }

    override fun postProcessEnvironment(environment: ConfigurableEnvironment, application: SpringApplication?) {
        val containerProperties = Properties().apply {
            setProperty("rarible.kafka.hosts", kafkaContainer.kafkaBoostrapServers())
        }
        val containersSource = PropertiesPropertySource("test-containers.properties", containerProperties)

        val defaultPropertiesSource = YamlPropertySourceLoader().load(
            "rarible.test.properties",
            ClassPathResource("/rarible/test/config/application-e2e.yml")
        )

        environment.propertySources.addFirst(containersSource)
        for (source in defaultPropertiesSource.asReversed()) {
            environment.propertySources.addLast(source)
        }
    }
}

