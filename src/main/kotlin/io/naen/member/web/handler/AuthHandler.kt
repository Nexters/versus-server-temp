package io.naen.member.web.handler

import io.naen.member.application.LoginUseCase
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono

@Component
class AuthHandler(private val loginUseCase: LoginUseCase) {
    fun login(request: ServerRequest): Mono<ServerResponse> =
        loginUseCase.login(request.queryParam("code")
            .orElseThrow { IllegalArgumentException() })
            .map { Response.of(it) }
            .flatMap {
                ServerResponse.ok()
                    .body(BodyInserters.fromValue(it))
            }

    data class Response(val id: Long, val nickname: String) {
        companion object {
            fun of(result: LoginUseCase.LoginResult): Response = Response(result.id, result.nickname)
        }
    }
}
