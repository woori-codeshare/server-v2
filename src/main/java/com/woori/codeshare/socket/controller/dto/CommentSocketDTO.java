package com.woori.codeshare.socket.controller.dto;

import com.woori.codeshare.comment.controller.dto.CommentResponseDTO;
import com.woori.codeshare.socket.enums.CommentEventType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class CommentSocketDTO {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CommentEventResponse {
        private Long roomId;
        private Long snapshotId;
        private Long commentId;
        private CommentResponseDTO.CommentListResponse comment; // 생성/수정 시 사용
        private CommentEventType eventType;
        private LocalDateTime timestamp;
    }
}
