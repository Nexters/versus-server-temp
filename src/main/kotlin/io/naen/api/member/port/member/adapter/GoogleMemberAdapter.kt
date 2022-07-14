package io.naen.api.member.port.member.adapter

import io.naen.api.member.domain.model.AuthServiceType
import io.naen.api.member.port.member.ExternalMemberLoadPort
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

private const val GOOGLE_USER_INFO_URL = "https://www.googleapis.com/oauth2/v1/userinfo"
private const val TOKEN_PREFIX = "Bearer"

@Component
class GoogleMemberAdapter : ExternalMemberLoadPort {
    override fun findBy(authToken: String): Mono<ExternalMemberLoadPort.Response> =
        WebClient.builder()
            .baseUrl(GOOGLE_USER_INFO_URL)
            .defaultHeader(HttpHeaders.AUTHORIZATION, "$TOKEN_PREFIX $authToken")
            .build()
            .get()
            .retrieve()
            .bodyToMono(Response::class.java)
            .map { ExternalMemberLoadPort.Response(it.id, AuthServiceType.GOOGLE) }

    data class Response(val id: String)

    override fun support(authServiceType: AuthServiceType): Boolean = authServiceType == AuthServiceType.GOOGLE
}