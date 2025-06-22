package com.woori.codeshare.socket.controller.dto;

import com.woori.codeshare.snapshot.controller.dto.SnapshotResponseDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class SnapshotSocketDTO {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SnapshotCreatedResponse {
        private Long roomId;
        private SnapshotResponseDTO.SnapshotDetailResponse snapshot;
        private LocalDateTime timestamp;
    }
}
