package com.rarible.protocol.union.core.restriction

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.RestrictionApiRule
import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

@Component
@CaptureSpan(type = SpanType.EXT)
class RestrictionApiRuleChecker(
    private val webClient: WebClient
) {

    suspend fun checkRule(
        itemId: ItemIdDto,
        rule: RestrictionApiRule,
        parameters: Map<String, String>
    ): RestrictionCheckResult {
        val substitutor = RestrictionTemplateSubstitutor(itemId, parameters)
        val url = substitutor.substitute(rule.uriTemplate)
        val body = rule.bodyTemplate?.let { substitutor.substitute(it) } ?: ""
        val result = when (rule.method) {

            RestrictionApiRule.Method.GET -> webClient.get()
                .uri(url)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(RestrictionCheckResult::class.java)

            RestrictionApiRule.Method.POST -> webClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(RestrictionCheckResult::class.java)
        }
        return result.awaitFirst()
    }
}