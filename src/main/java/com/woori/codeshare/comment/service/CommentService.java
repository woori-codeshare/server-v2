package com.woori.codeshare.comment.service;

import com.woori.codeshare.comment.controller.dto.CommentRequestDTO;
import com.woori.codeshare.comment.controller.dto.CommentResponseDTO;
import com.woori.codeshare.comment.domain.Comment;
import com.woori.codeshare.comment.exception.CommentErrorCode;
import com.woori.codeshare.comment.exception.CommentException;
import com.woori.codeshare.comment.repository.CommentRepository;
import com.woori.codeshare.room.domain.Room;
import com.woori.codeshare.snapshot.domain.Snapshot;
import com.woori.codeshare.snapshot.exception.SnapshotErrorCode;
import com.woori.codeshare.snapshot.exception.SnapshotException;
import com.woori.codeshare.snapshot.repository.SnapshotRepository;
import com.woori.codeshare.socket.service.CommentSocketService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final SnapshotRepository snapshotRepository;
    private final CommentSocketService commentSocketService;

    /**
     * 댓글 등록 로직
     *
     * @param request 댓글 등록 요청 DTO
     * @return 등록된 댓글 응답 DTO
     */
    @Transactional
    public CommentResponseDTO.CommentCreateResponse addComment(Long snapshotId, CommentRequestDTO.CommentCreateRequest request) {
        Snapshot snapshot = snapshotRepository.findById(snapshotId)
                .orElseThrow(() -> new SnapshotException(SnapshotErrorCode.SNAPSHOT_NOT_FOUND));

        Comment parentComment = null;
        if (request.getParentCommentId() != null) {
            parentComment = commentRepository.findById(request.getParentCommentId())
                    .orElseThrow(() -> new IllegalArgumentException("부모 댓글이 존재하지 않습니다."));
        }

        Comment comment = new Comment();
        comment.setSnapshot(snapshot);
        comment.setContent(request.getContent());
        comment.setSolved(false);
        comment.setParentComment(parentComment);

        Comment savedComment = commentRepository.save(comment);

        // WebSocket 알림 전송 - 댓글 생성 알림
        try {
            Room room = snapshot.getRoom();

            CommentResponseDTO.CommentListResponse commentDetail = 
                CommentResponseDTO.CommentListResponse.builder()
                    .commentId(savedComment.getCommentId())
                    .parentCommentId(parentComment != null ? parentComment.getCommentId() : null)
                    .content(savedComment.getContent())
                    .solved(savedComment.isSolved())
                    .createdAt(savedComment.getCreatedAt())
                    .updatedAt(savedComment.getUpdatedAt())
                    .build();
                    
            commentSocketService.notifyCommentCreated(room, snapshotId, commentDetail);

        } catch (Exception e) {
            // WebSocket 알림 실패 시 로깅만 하고 계속 진행
            log.warn("댓글 생성 WebSocket 알림 전송 실패: snapshotId={}, commentId={}, error={}", snapshotId, savedComment.getCommentId(), e.getMessage());
        }

        return CommentResponseDTO.CommentCreateResponse.builder()
                .commentId(savedComment.getCommentId())
                .parentCommentId(parentComment != null ? parentComment.getCommentId() : null)
                .snapshotId(snapshot.getSnapshotId())
                .content(savedComment.getContent())
                .build();
    }

    /**
     * 질문 해결 여부 업데이트 로직
     *
     * @param request 해결 여부 업데이트 요청 DTO
     * @return 업데이트된 댓글 응답 DTO
     */
    @Transactional
    public CommentResponseDTO.CommentResolveResponse resolveComment(Long commentId, CommentRequestDTO.CommentResolveRequest request) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CommentException(CommentErrorCode.COMMENT_NOT_FOUND));

        comment.setSolved(request.isSolved());
        Comment updatedComment = commentRepository.save(comment);

        // WebSocket 알림 전송 - 댓글 해결 상태 변경 알림
        try {
            Room room = comment.getSnapshot().getRoom();

            CommentResponseDTO.CommentListResponse commentDetail = 
                CommentResponseDTO.CommentListResponse.builder()
                    .commentId(updatedComment.getCommentId())
                    .parentCommentId(updatedComment.getParentComment() != null ? updatedComment.getParentComment().getCommentId() : null)
                    .content(updatedComment.getContent())
                    .solved(updatedComment.isSolved())
                    .createdAt(updatedComment.getCreatedAt())
                    .updatedAt(updatedComment.getUpdatedAt())
                    .build();
                    
            commentSocketService.notifyCommentResolved(room, comment.getSnapshot().getSnapshotId(), commentDetail);

        } catch (Exception e) {
            // WebSocket 알림 실패 시 로깅만 하고 계속 진행
            log.warn("댓글 해결 상태 변경 WebSocket 알림 전송 실패: commentId={}, error={}", updatedComment.getCommentId(), e.getMessage());
        }

        return CommentResponseDTO.CommentResolveResponse.builder()
                .commentId(updatedComment.getCommentId())
                .solved(updatedComment.isSolved())
                .build();
    }

    /**
     * 질문 내용 수정 로직
     *
     * @param request 수정 요청 DTO
     * @return 수정된 댓글 응답 DTO
     */
    @Transactional
    public CommentResponseDTO.CommentUpdateResponse updateComment(Long commentId, CommentRequestDTO.CommentUpdateRequest request) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CommentException(CommentErrorCode.COMMENT_NOT_FOUND));

        comment.setContent(request.getContent());

        Comment updatedComment = commentRepository.save(comment);

        // WebSocket 알림 전송 - 댓글 수정 알림
        try {
            Room room = comment.getSnapshot().getRoom();

            CommentResponseDTO.CommentListResponse commentDetail = 
                CommentResponseDTO.CommentListResponse.builder()
                    .commentId(updatedComment.getCommentId())
                    .parentCommentId(updatedComment.getParentComment() != null ? updatedComment.getParentComment().getCommentId() : null)
                    .content(updatedComment.getContent())
                    .solved(updatedComment.isSolved())
                    .createdAt(updatedComment.getCreatedAt())
                    .updatedAt(updatedComment.getUpdatedAt())
                    .build();
                    
            commentSocketService.notifyCommentUpdated(room, comment.getSnapshot().getSnapshotId(), commentDetail);

        } catch (Exception e) {
            // WebSocket 알림 실패 시 로깅만 하고 계속 진행
            log.warn("댓글 수정 WebSocket 알림 전송 실패: commentId={}, error={}", updatedComment.getCommentId(), e.getMessage());
        }

        return CommentResponseDTO.CommentUpdateResponse.builder()
                .commentId(updatedComment.getCommentId())
                .content(updatedComment.getContent())
                .updatedAt(updatedComment.getUpdatedAt())
                .build();
    }

    /**
     * 질문 삭제 로직
     *
     * @param commentId 삭제할 댓글 ID
     */
    @Transactional
    public void deleteComment(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CommentException(CommentErrorCode.COMMENT_NOT_FOUND));
        
        // 삭제 전에 필요한 정보 저장
        Room room = comment.getSnapshot().getRoom();
        Long snapshotId = comment.getSnapshot().getSnapshotId();
        
        commentRepository.delete(comment);
        
        // WebSocket 알림 전송 - 댓글 삭제 알림
        try {
            commentSocketService.notifyCommentDeleted(room, snapshotId, commentId);
        } catch (Exception e) {
            // WebSocket 알림 실패 시 로깅만 하고 계속 진행
            log.warn("댓글 삭제 WebSocket 알림 전송 실패: commentId={}, error={}", commentId, e.getMessage());
        }
    }

    /**
     * 질문 목록 조회 로직
     *
     * @param snapshotId 스냅샷 ID
     */
    @Transactional
    public List<CommentResponseDTO.CommentListResponse> getComments(Long snapshotId) {
        Snapshot snapshot = snapshotRepository.findById(snapshotId)
                .orElseThrow(() -> new SnapshotException(SnapshotErrorCode.SNAPSHOT_NOT_FOUND));

        // snapshotId로 댓글 목록 조회
        List<Comment> comments = commentRepository.findBySnapshotId(snapshotId);

        // 댓글 목록 Response DTO로 변환 후 반환
        return comments.stream()
                .map(comment -> CommentResponseDTO.CommentListResponse.builder()
                        .commentId(comment.getCommentId())
                        .parentCommentId(comment.getParentComment() != null ? comment.getParentComment().getCommentId() : null)
                        .content(comment.getContent())
                        .solved(comment.isSolved())
                        .createdAt(comment.getCreatedAt())
                        .updatedAt(comment.getUpdatedAt())
                        .build())
                .collect(Collectors.toList());
    }
}
