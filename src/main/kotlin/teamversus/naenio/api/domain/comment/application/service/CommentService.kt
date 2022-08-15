package teamversus.naenio.api.domain.comment.application.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import teamversus.naenio.api.domain.comment.application.CommentCreateUseCase
import teamversus.naenio.api.domain.comment.application.CommentDeleteUseCase
import teamversus.naenio.api.domain.comment.domain.event.CommentCreatedEvent
import teamversus.naenio.api.domain.comment.domain.model.CommentParent
import teamversus.naenio.api.domain.comment.domain.model.CommentRepository
import teamversus.naenio.api.domain.post.domain.model.PostRepository
import teamversus.naenio.api.query.handler.CommentQueryEventHandler

@Service
class CommentService(
    private val commentRepository: CommentRepository,
    private val postRepository: PostRepository,
    private val commentQueryEventHandler: CommentQueryEventHandler,
) : CommentCreateUseCase, CommentDeleteUseCase {
    override fun create(command: CommentCreateUseCase.Command, memberId: Long): Mono<CommentCreateUseCase.Result> =
        Mono.just(command)
            .filterWhen(::isParentCommentable)
            .switchIfEmpty { Mono.error(IllegalArgumentException("부모 엔티티가 존재하지 않거나 대댓글에 댓글 생성을 시도하였습니다. parentId=${command.parentId}, parentType=${command.parentType}")) }
            .flatMap {
                commentRepository.save(command.toDomain(memberId))
                    .doOnSuccess {
                        commentQueryEventHandler.handle(
                            CommentCreatedEvent(
                                it.id,
                                it.memberId,
                                it.parentId,
                                it.parentType,
                                it.content,
                                it.createdDateTime,
                                it.lastModifiedDateTime
                            )
                        )
                    }
            }
            .map { CommentCreateUseCase.Result.of(it) }

    private fun isParentCommentable(it: CommentCreateUseCase.Command): Mono<Boolean> {
        if (it.parentType == CommentParent.POST) {
            return postRepository.existsById(it.parentId)
        }
        if (it.parentType == CommentParent.COMMENT) {
            return commentRepository.existsByIdAndParentType(it.parentId, CommentParent.POST)
        }
        return Mono.just(false)
    }


    @Transactional
    override fun delete(id: Long): Mono<Void> =
        Mono.just(id)
            .filterWhen { commentRepository.existsById(it) }
            .switchIfEmpty { Mono.error(IllegalArgumentException("존재하지 않는 댓글 입니다. id=${id}}")) }
            .flatMap { commentRepository.deleteById(id) }
}