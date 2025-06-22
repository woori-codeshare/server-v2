package com.woori.codeshare.socket.service;

import com.woori.codeshare.room.domain.Room;
import com.woori.codeshare.vote.controller.dto.VoteResponseDTO;
import com.woori.codeshare.socket.controller.dto.VoteSocketDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class VoteSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 투표 결과 업데이트 브로드캐스트
     */
    public void notifyVoteUpdated(Room room, Long voteId, VoteResponseDTO.VoteResultResponse voteResult) {
        log.info("[WebSocket] 투표 결과 업데이트 알림: roomId={}, roomUuid={}, voteId={}", room.getRoomId(), room.getUuid(), voteId);

        VoteSocketDTO.VoteUpdatedResponse response =
                VoteSocketDTO.VoteUpdatedResponse.builder()
                        .roomId(room.getRoomId())
                        .voteId(voteId)
                        .voteResult(voteResult)
                        .timestamp(LocalDateTime.now())
                        .build();

        // 해당 방의 모든 사용자에게 브로드캐스트
        messagingTemplate.convertAndSend("/topic/room/" + room.getUuid() + "/votes", response);

        log.info("[WebSocket] 투표 결과 업데이트 알림 전송 완료: roomId={}, roomUuid={}, voteId={}", room.getRoomId(), room.getUuid(), voteId);
    }
}
