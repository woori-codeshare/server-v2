package com.woori.codeshare.snapshot.service;

import com.woori.codeshare.room.domain.Room;
import com.woori.codeshare.room.exception.RoomErrorCode;
import com.woori.codeshare.room.exception.RoomException;
import com.woori.codeshare.room.repository.RoomRepository;
import com.woori.codeshare.snapshot.controller.dto.SnapshotRequestDTO;
import com.woori.codeshare.snapshot.controller.dto.SnapshotResponseDTO;
import com.woori.codeshare.snapshot.domain.Snapshot;
import com.woori.codeshare.snapshot.repository.SnapshotRepository;
import com.woori.codeshare.vote.service.VoteService;
import com.woori.codeshare.socket.service.SnapshotSocketService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SnapshotService {

    private final SnapshotRepository snapshotRepository;
    private final RoomRepository roomRepository;
    private final VoteService voteService;
    private final SnapshotSocketService snapshotSocketService;

    /**
     * 스냅샷 저장 로직 (기본 투표 생성 포함)
     *
     * @param request 스냅샷 저장 요청 DTO
     * @return 저장된 스냅샷 응답 DTO
     */
    @Transactional
    public SnapshotResponseDTO.SnapshotCreateResponse saveSnapshot(Long roomId, SnapshotRequestDTO.SnapshotCreateRequest request) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RoomException(RoomErrorCode.ROOM_NOT_FOUND));

        // 제목이 없을 경우 시간으로 설정
        String snapshotTitle = request.getTitle();
        if (snapshotTitle == null || snapshotTitle.trim().isEmpty()) {
            snapshotTitle = getCurrentFormattedTime();
        }

        // Snapshot 엔티티 생성
        Snapshot snapshot = new Snapshot();
        snapshot.setRoom(room);
        snapshot.setTitle(snapshotTitle);
        snapshot.setDescription(request.getDescription());
        snapshot.setCode(request.getCode());

        // Snapshot 저장
        Snapshot savedSnapshot = snapshotRepository.save(snapshot);

        // 기본 투표 생성
        Long voteId = voteService.createDefaultVoteForSnapshot(savedSnapshot.getSnapshotId());

        // Response DTO 생성
        SnapshotResponseDTO.SnapshotCreateResponse response = SnapshotResponseDTO.SnapshotCreateResponse.builder()
                .roomId(room.getRoomId())
                .snapshotId(savedSnapshot.getSnapshotId())
                .title(savedSnapshot.getTitle())
                .description(savedSnapshot.getDescription())
                .code(savedSnapshot.getCode())
                .voteId(voteId)
                .createdAt(savedSnapshot.getCreatedAt())
                .build();

        // WebSocket 알림 전송 - 스냅샷 생성 알림
        try {
            SnapshotResponseDTO.SnapshotDetailResponse snapshotDetail =
                    SnapshotResponseDTO.SnapshotDetailResponse.builder()
                            .snapshotId(savedSnapshot.getSnapshotId())
                            .title(savedSnapshot.getTitle())
                            .description(savedSnapshot.getDescription())
                            .code(savedSnapshot.getCode())
                            .createdAt(savedSnapshot.getCreatedAt())
                            .comments(List.of()) // 새로 생성된 스냅샷은 댓글이 없음
                            .build();

            snapshotSocketService.notifySnapshotCreated(room, snapshotDetail);

        } catch (Exception e) {
            // WebSocket 알림 실패 시 로깅만 하고 계속 진행
            log.warn("스냅샷 생성 WebSocket 알림 전송 실패: roomId={}, snapshotId={}, error={}", room.getRoomId(), savedSnapshot.getSnapshotId(), e.getMessage());
        }

        return response;
    }


    /**
     * 현재 시간을 "2025년 2월 17일, 15:00" 형식으로 반환
     *
     * @return 포맷된 시간 문자열
     */
    private String getCurrentFormattedTime() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy년 M월 d일, HH:mm");
        return LocalDateTime.now().format(formatter);
    }

    /**
     * 스냅샷 목록 조회
     *
     * @param uuid 방 UUID
     * @return 스냅샷 상세 목록 응답 DTO
     */
    @Transactional
    public List<SnapshotResponseDTO.SnapshotDetailResponse> getSnapshots(String uuid) {
        Room room = roomRepository.findByUuid(uuid)
                .orElseThrow(() -> new RoomException(RoomErrorCode.ROOM_NOT_FOUND));

        List<Snapshot> snapshots = snapshotRepository.findSnapshotsWithCommentsByRoomAndCreatedAtAfter(room, room.getCreatedAt());

        // 스냅샷 목록 변환
        return snapshots.stream().map(snapshot -> SnapshotResponseDTO.SnapshotDetailResponse.builder()
                .snapshotId(snapshot.getSnapshotId())
                .title(snapshot.getTitle())
                .description(snapshot.getDescription())
                .code(snapshot.getCode())
                .createdAt(snapshot.getCreatedAt())
                .comments(snapshot.getComments().stream().map(comment -> SnapshotResponseDTO.CommentDetailResponse.builder()
                        .commentId(comment.getCommentId())
                        .parentCommentId(comment.getParentComment() != null ? comment.getParentComment().getCommentId() : null)
                        .content(comment.getContent())
                        .solved(comment.isSolved())
                        .createdAt(comment.getCreatedAt())
                        .build()).toList())
                .build()).toList();
    }
}
