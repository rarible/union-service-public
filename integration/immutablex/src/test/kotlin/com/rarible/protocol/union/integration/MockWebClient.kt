package com.rarible.protocol.union.integration

import org.assertj.core.api.Assertions
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

object MockWebClient {
    operator fun invoke(expectedPath: String, response: String, status: HttpStatus = HttpStatus.OK, contentType: MediaType = MediaType.APPLICATION_JSON) = WebClient
        .builder()
        .exchangeFunction { req ->
            Assertions.assertThat(req.url().toString()).isEqualTo(expectedPath)

            Mono.just(
                ClientResponse.create(status)
                    .header("content-type", contentType.toString())
                    .body(response)
                    .build()
            )
        }.build()
}
