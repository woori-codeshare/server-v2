package com.woori.codeshare.socket.service;

import com.woori.codeshare.room.domain.Room;
import com.woori.codeshare.snapshot.controller.dto.SnapshotResponseDTO;
import com.woori.codeshare.socket.controller.dto.SnapshotSocketDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class SnapshotSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 스냅샷 생성 알림 브로드캐스트
     */
    public void notifySnapshotCreated(Room room, SnapshotResponseDTO.SnapshotDetailResponse snapshot) {
        log.info("[WebSocket] 스냅샷 생성 알림: roomId={}, roomUuid={}, snapshotId={}", room.getRoomId(), room.getUuid(), snapshot.getSnapshotId());

        SnapshotSocketDTO.SnapshotCreatedResponse response =
                SnapshotSocketDTO.SnapshotCreatedResponse.builder()
                        .roomId(room.getRoomId())
                        .snapshot(snapshot)
                        .timestamp(LocalDateTime.now())
                        .build();

        // 해당 방의 모든 사용자에게 브로드캐스트
        messagingTemplate.convertAndSend("/topic/room/" + room.getUuid() + "/snapshots", response);

        log.info("[WebSocket] 스냅샷 생성 알림 전송 완료: roomId={}, roomUuid={}", room.getRoomId(), room.getUuid());
    }
}
