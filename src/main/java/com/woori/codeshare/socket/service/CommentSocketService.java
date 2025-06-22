package com.woori.codeshare.socket.service;

import com.woori.codeshare.comment.controller.dto.CommentResponseDTO;
import com.woori.codeshare.room.domain.Room;
import com.woori.codeshare.socket.controller.dto.CommentSocketDTO;
import com.woori.codeshare.socket.enums.CommentEventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 댓글 생성 브로드캐스트
     */
    public void notifyCommentCreated(Room room, Long snapshotId, CommentResponseDTO.CommentListResponse comment) {
        CommentEventType eventType = comment.getParentCommentId() == null
                ? CommentEventType.COMMENT_CREATED
                : CommentEventType.REPLY_CREATED;

        notifyCommentEvent(room, snapshotId, comment.getCommentId(), comment, eventType);
    }

    /**
     * 댓글 수정 브로드캐스트
     */
    public void notifyCommentUpdated(Room room, Long snapshotId, CommentResponseDTO.CommentListResponse comment) {
        notifyCommentEvent(room, snapshotId, comment.getCommentId(), comment, CommentEventType.COMMENT_UPDATED);
    }

    /**
     * 댓글 삭제 브로드캐스트
     */
    public void notifyCommentDeleted(Room room, Long snapshotId, Long commentId) {
        notifyCommentEvent(room, snapshotId, commentId, null, CommentEventType.COMMENT_DELETED);
    }

    /**
     * 댓글 해결 상태 변경 브로드캐스트
     */
    public void notifyCommentResolved(Room room, Long snapshotId, CommentResponseDTO.CommentListResponse comment) {
        notifyCommentEvent(room, snapshotId, comment.getCommentId(), comment, CommentEventType.COMMENT_RESOLVED);
    }

    /**
     * 공통 댓글 이벤트 브로드캐스트 메서드
     */
    private void notifyCommentEvent(Room room, Long snapshotId, Long commentId, CommentResponseDTO.CommentListResponse comment, CommentEventType eventType) {
        log.info("[WebSocket] 댓글 이벤트 알림: roomId={}, roomUuid={}, snapshotId={}, commentId={}, eventType={}", room.getRoomId(), room.getUuid(), snapshotId, commentId, eventType);

        CommentSocketDTO.CommentEventResponse response =
                CommentSocketDTO.CommentEventResponse.builder()
                        .roomId(room.getRoomId())
                        .snapshotId(snapshotId)
                        .commentId(commentId)
                        .comment(comment)
                        .eventType(eventType)
                        .timestamp(LocalDateTime.now())
                        .build();

        // 해당 방의 모든 사용자에게 브로드캐스트
        messagingTemplate.convertAndSend("/topic/room/" + room.getUuid() + "/comments", response);

        log.info("[WebSocket] 댓글 이벤트 알림 전송 완료: roomId={}, roomUuid={}, eventType={}", room.getRoomId(), room.getUuid(), eventType);
    }
}
