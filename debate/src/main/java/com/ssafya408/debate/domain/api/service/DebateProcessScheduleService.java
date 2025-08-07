package com.ssafya408.debate.domain.api.service;

import com.ssafya408.debate.domain.api.dto.debate.DebateTurn;
import com.ssafya408.debate.domain.api.dto.control.MediaControlInfo;
import com.ssafya408.debate.domain.api.dto.room.RoomStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Instant;
@Service
@RequiredArgsConstructor
@Slf4j
public class DebateProcessScheduleService {

    private final SimpMessagingTemplate simpMessagingTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    @Qualifier("taskScheduler")
    private final TaskScheduler taskScheduler;

    public void gameStart(RoomManager roomManager) {
        if (roomManager == null) return;

        startOpinion(roomManager);
        startBattle(roomManager);
    }
    // 게임 시작 대기 30초
    private void startOpinion(RoomManager roomManager) {
        log.info("[ 게임 시작 전 대기 ] {}",roomManager.getRoomId() );
        if(!roomManager.getStatus().equals("opinion")){
            return;
        }

        // 게임 시작 30초 남음 BROADCAST
        simpMessagingTemplate.convertAndSend("/sub/room/" + roomManager.getRoomId()
            ,  "게임 시작까지 30초 남았습니다.");

        taskScheduler.schedule(() -> {
            startOpinionTurn(roomManager);
        }, Instant.now().plusSeconds(5));
    }

    // 발언 제어 로직
    private void startOpinionTurn(RoomManager roomManager) {
        if (roomManager.isFinished()) {
            log.info("[1페이즈 종료]");
            roomManager.setStatus(RoomStatus.BATTLE);
            return;
        }
        log.info("[발언 시작] {}",roomManager.getRoomId() );
        log.info("[발언 시작] currentTurn : {}",roomManager.getTurn());
        log.info("[발언 시작] currentOpinionIndex : {}",roomManager.getCurrentOpinionIndex());
        log.info("[발언 시작] Count : {}",roomManager.getPlayerCount());

        int currentIndex = getRoomManager(roomManager).getCurrentOpinionIndex();
        DebateTurn turn = roomManager.getTurn();
        String speaker=roomManager.getCurrentSpeaker();

        roomManager.setCurrentOpinionIndex(++currentIndex);

        MediaControlInfo mediaControlInfo = MediaControlInfo.builder()
            .speaker(speaker)
            .roomId(roomManager.getRoomId())
            .build();

        log.info("[발언 시작] speaker : {}",speaker );

        // signaling server 에 publish [mic on]
        redisTemplate.convertAndSend("signaling:mic:on", mediaControlInfo);
        // room 참여자들에게 broadcast
        simpMessagingTemplate.convertAndSend("/sub/room/" + roomManager.getRoomId()
            , speaker + "님이 발언합니다.");

        taskScheduler.schedule(() -> {
            endOpinionTurn(roomManager,mediaControlInfo);
        }, Instant.now().plusSeconds(5));
    }

    private static RoomManager getRoomManager(RoomManager roomManager) {
        return roomManager;
    }

    // 발언 종료 이후 3초 대기
    private void endOpinionTurn(RoomManager roomManager,MediaControlInfo mediaControlInfo) {
        log.info("[발언 종료] {}",mediaControlInfo );
        // signaling server 에 publish [mic off]
        redisTemplate.convertAndSend("signaling:mic:off" + roomManager.getRoomId(), mediaControlInfo);
        // room 참여자들에게 broadcast
        simpMessagingTemplate.convertAndSend("/sub/room/" + roomManager.getRoomId()
            , mediaControlInfo.getSpeaker() + "님의 발언이 종료되었습니다.");
        taskScheduler.schedule(() -> {
            startOpinionTurn(roomManager);
        }, Instant.now().plusSeconds(3));
    }

    private void startBattle(RoomManager roomManager) {
    }
}
