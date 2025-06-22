package com.woori.codeshare.socket.controller.dto;

import com.woori.codeshare.vote.controller.dto.VoteResponseDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class VoteSocketDTO {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VoteUpdatedResponse {
        private Long roomId;
        private Long voteId;
        private VoteResponseDTO.VoteResultResponse voteResult;
        private LocalDateTime timestamp;
    }
}
