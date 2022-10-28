package com.rarible.protocol.union.core

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty

@ConditionalOnProperty(name = ["common.feature-flags.enableCustomWebClientCustomizer"], havingValue = "true")
annotation class UnionWebClientCustomizerEnabled
