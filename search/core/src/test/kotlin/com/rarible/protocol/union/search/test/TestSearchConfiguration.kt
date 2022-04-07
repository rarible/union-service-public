package com.rarible.protocol.union.search.test

import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Lazy

@Lazy
@Configuration
@ComponentScan(basePackages = ["com.rarible.protocol.union.search"])
class TestSearchConfiguration {

}
