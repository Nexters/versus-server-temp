package teamversus.naenio.api.domain.vote.application.service

import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import teamversus.naenio.api.domain.vote.application.VoteCastUseCase
import teamversus.naenio.api.domain.vote.domain.event.VoteChangedEvent
import teamversus.naenio.api.domain.vote.domain.event.VoteCreatedEvent
import teamversus.naenio.api.domain.vote.domain.model.VoteRepository
import teamversus.naenio.api.query.handler.VoteQueryEventHandler

@Service
class VoteService(
    private val voteRepository: VoteRepository,
    private val voteQueryEventHandler: VoteQueryEventHandler,
) : VoteCastUseCase {
    override fun cast(command: VoteCastUseCase.Command, memberId: Long): Mono<VoteCastUseCase.Result> =
        voteRepository.findByPostIdAndMemberId(command.postId, memberId)
            .map { it.changeChoice(command.choiceId, memberId) }
            .flatMap {
                voteRepository.save(it)
                    .doOnSuccess { vote ->
                        voteQueryEventHandler.handle(
                            VoteChangedEvent(
                                vote.id,
                                vote.memberId,
                                vote.postId,
                                vote.choiceId,
                                vote.lastModifiedDateTime,
                            )
                        )
                    }
            }
            .switchIfEmpty {
                Mono.just(command.toDomain(memberId))
                    .flatMap {
                        voteRepository.save(it)
                            .doOnSuccess { vote ->
                                voteQueryEventHandler.handle(
                                    VoteCreatedEvent(
                                        vote.id,
                                        vote.memberId,
                                        vote.postId,
                                        vote.choiceId,
                                        vote.createdDateTime
                                    )
                                )
                            }
                    }
            }
            .map { VoteCastUseCase.Result.of(it) }
}
