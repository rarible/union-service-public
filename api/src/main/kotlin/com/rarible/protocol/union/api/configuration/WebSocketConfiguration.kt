package com.rarible.protocol.union.api.configuration

import com.rarible.protocol.union.api.handler.ChangesHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.web.reactive.HandlerMapping
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping
import org.springframework.web.reactive.socket.server.WebSocketService
import org.springframework.web.reactive.socket.server.support.HandshakeWebSocketService
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter
import org.springframework.web.reactive.socket.server.upgrade.ReactorNettyRequestUpgradeStrategy

@Configuration
class WebSocketConfiguration {
    @Bean
    fun websocketHandlerMapping(handler: ChangesHandler): HandlerMapping {
        return SimpleUrlHandlerMapping().apply {
            urlMap = mapOf(
               "/v0.1/subscribe" to handler
            )
            order = Ordered.HIGHEST_PRECEDENCE
        }
    }

    @Bean
    fun handlerAdapter(webSocketService: WebSocketService): WebSocketHandlerAdapter {
        return WebSocketHandlerAdapter(webSocketService)
    }

    @Bean
    fun webSocketService(): WebSocketService {
        return HandshakeWebSocketService(ReactorNettyRequestUpgradeStrategy())
    }
}
