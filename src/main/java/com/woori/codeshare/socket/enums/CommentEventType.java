package com.woori.codeshare.socket.enums;

/**
 * 댓글 WebSocket 이벤트 타입
 */
public enum CommentEventType {
    COMMENT_CREATED("댓글이 생성되었습니다"),
    REPLY_CREATED("답변이 생성되었습니다"),
    COMMENT_UPDATED("댓글이 수정되었습니다"),
    COMMENT_DELETED("댓글이 삭제되었습니다"),
    COMMENT_RESOLVED("댓글이 해결되었습니다"),
    COMMENT_UNRESOLVED("댓글이 미해결로 변경되었습니다");

    private final String description;

    CommentEventType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
