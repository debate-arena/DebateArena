package com.ssafya408.debate.domain.api.service;

import com.ssafya408.debate.domain.api.dto.ai.OpinionTextRequest;
import com.ssafya408.debate.domain.api.dto.ai.SiegeDefenseRequest;
import com.ssafya408.debate.domain.api.dto.ai.SiegeDefenseRequest.Side;
import com.ssafya408.debate.domain.api.dto.ai.SiegeDefenseResponse;
import com.ssafya408.debate.domain.api.dto.debate.*;
import com.ssafya408.debate.domain.api.dto.control.MediaControlInfo;
import com.ssafya408.debate.domain.api.dto.room.RoomStatus;
import com.ssafya408.debate.domain.db.cache.DebateRedisInfo;
import com.ssafya408.debate.domain.db.cache.DebateRedisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class DebateProcessScheduleService {

    private final SimpMessagingTemplate simpMessagingTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    @Qualifier("taskScheduler")
    private final TaskScheduler taskScheduler;
    private final DebateRedisRepository debateRedisRepository;
    private final AiService aiService;

    public void gameStart(RoomManager roomManager) {
        if (roomManager == null) return;

        startOpinion(roomManager);

    }
    // 게임 시작 대기 30초
    private void startOpinion(RoomManager roomManager) {
        log.info("[ 게임 시작 전 대기 ] {}",roomManager.getRoomId() );
        if(!roomManager.getStatus().equals(RoomStatus.OPINION)){
            startBattle(roomManager);
            return;
        }

        // 게임 시작 30초 남음 BROADCAST
        DebateRedisInfo debateInfo = debateRedisRepository.findByRoomId(roomManager.getRoomId());

        OpinionStartResponseDto dto = OpinionStartResponseDto.builder()
                .roomId(debateInfo.getRoomId())
                .type(debateInfo.getType())
                .topicId(debateInfo.getTopicId())
                .topicText(debateInfo.getTopicText())
                .firstOption(debateInfo.getFirstOption())
                .secondOption(debateInfo.getSecondOption())
                .status(debateInfo.getStatus())
                .webRTCStatus(debateInfo.getWebRTCStatus())
                .firstTeam(debateInfo.getFirstTeam())
                .secondTeam(debateInfo.getSecondTeam())
                .debateStartAt(LocalDateTime.now())
                .build();

        simpMessagingTemplate.convertAndSend("/debate/room/" + roomManager.getRoomId(),dto);

        taskScheduler.schedule(() -> {
            startOpinionTurn(roomManager);
        }, Instant.now().plusSeconds(5));
    }

    // 발언 제어 로직
    private void startOpinionTurn(RoomManager roomManager) {
        if (roomManager.isFinished()) {
            log.info("[1페이즈 종료]");
            startBattle(roomManager);
            return;
        }
        log.info("[1페이즈 발언 시작] currentOpinionIndex : {}",roomManager.getCurrentOpinionIndex());

        String speaker=roomManager.getCurrentSpeaker();

        MediaControlInfo mediaControlInfo = MediaControlInfo.builder()
            .speaker(speaker)
            .roomId(roomManager.getRoomId())
            .build();
        SpeakerStartResponseDto dto = SpeakerStartResponseDto.builder()
                .speaker(speaker)
                .speakerStartAt(LocalDateTime.now())
                .build();

        log.info("[1페이즈 발언 시작] speaker : {}",speaker );

        // signaling server 에 publish [mic on]
        redisTemplate.convertAndSend("signaling:mic:on", mediaControlInfo);
        // room 참여자들에게 broadcast
        simpMessagingTemplate.convertAndSend("/debate/room/" + roomManager.getRoomId()+"/speak/start", dto);

        taskScheduler.schedule(() -> {
            endOpinionTurn(roomManager,mediaControlInfo);
        }, Instant.now().plusSeconds(5));
    }

    private static RoomManager getRoomManager(RoomManager roomManager) {
        return roomManager;
    }

    // 발언 종료 이후 3초 대기
    private void endOpinionTurn(RoomManager roomManager,MediaControlInfo mediaControlInfo) {
        summarizeOpinion(roomManager)
            .subscribe(
                null,                                // next 없음
                e -> log.error("pipeline error", e), // 에러 소비자 필수
                () -> log.info("broadcast done")     // 완료 콜백
            );

        log.info("[1페이즈 발언 종료] {}",mediaControlInfo );
        SpeakerEndResponseDto dto = SpeakerEndResponseDto.builder()
                .speaker(mediaControlInfo.getSpeaker())
                .speakerEndAt(LocalDateTime.now())
                .build();
        // signaling server 에 publish [mic off]
        redisTemplate.convertAndSend("signaling:mic:off" + roomManager.getRoomId(), mediaControlInfo);
        // room 참여자들에게 broadcast
        simpMessagingTemplate.convertAndSend("/debate/room/" + roomManager.getRoomId()+"/speak/end",dto);
        roomManager.advanceTurn();
        taskScheduler.schedule(() -> {
            startOpinionTurn(roomManager);
        }, Instant.now().plusSeconds(3));
    }


    public Mono<Void> summarizeOpinion(RoomManager roomManager) {
        DebateRedisInfo debateInfo = debateRedisRepository.findByRoomId(roomManager.getRoomId());

        String currentSpeaker = roomManager.getCurrentSpeaker();
        log.info("[current speaker] >>> {}",currentSpeaker);

        String opinion = roomManager.getSpeakerTotalOpinion(currentSpeaker);
        int teamIdx = roomManager.getTeamIdx();
        String position= teamIdx==0? debateInfo.getFirstOption() : debateInfo.getSecondOption();

        OpinionTextRequest request = OpinionTextRequest.builder()
            .user_id(currentSpeaker)
            .topic(debateInfo.getTopicText())
            .text(opinion)
            .position(position)
            .build();
        log.info("[opinion summary req] >>> {}, idx >>> {}",request.toString(),
            roomManager.getCurrentIndex());
        return aiService.requestOpinionSummary(roomManager.getRoomId(),
            roomManager.getCurrentIndex(), request );
    }

    private void startBattle(RoomManager roomManager) {
        log.info("[ 공방전 시작 전 대기 ] {}",roomManager.getRoomId() );
        if(roomManager.getStatus().equals(RoomStatus.BATTLE)){
            startBattleTurn(roomManager);
            return;
        }

        // 공방 시작 30초 남음 BROADCAST
        BattleStartResponseDto dto = BattleStartResponseDto.builder()
                .status("success")
                .battleStartAt(LocalDateTime.now())
                .build();

        simpMessagingTemplate.convertAndSend("/debate/room/" + roomManager.getRoomId()+"/start/battle", dto);

        roomManager.advanceTurn();
        taskScheduler.schedule(() -> {
            startBattleTurn(roomManager);
        }, Instant.now().plusSeconds(15));
    }

    private void startBattleTurn(RoomManager roomManager) {
        if (roomManager.isFinished() || roomManager.getAttackTarget().isEmpty()) {
            if(roomManager.getAttackTarget().isEmpty()){
                log.info("[공방전 종료] 아무도 공방전 투표를 진행하지 않음 RoomId : {} ",roomManager.getRoomId());
            }
            log.info("[2페이즈 종료]");
            log.info("[2페이즈 종료 전 상태] > {}",roomManager.getStatus());

            roomManager.setStatus(RoomStatus.VOTING);
            log.info("[2페이즈 종료 후 상태] > {}",roomManager.getStatus());
            
            startVote(roomManager);

            return;
        }

        log.info("[2페이즈 발언 시작] currentTurn : {}",roomManager.getTurn());
        log.info("[2페이즈 발언 시작] currentBattleIndex : {}",roomManager.getCurrentBattleIndex());

        int currentIndex = getRoomManager(roomManager).getCurrentBattleIndex();
        DebateTurn turn = roomManager.getTurn();
        String speaker;

        if(turn.equals(DebateTurn.ATTACK)){
            speaker=roomManager.getCurrentSpeaker();
            if(roomManager.getAttackTarget().get(speaker)==null){
                startBattleTurn(roomManager);
                return ;
            }
            roomManager.setTurn(DebateTurn.DEFENSE);
        }else{
            String attacker;
            if(currentIndex%2==0){
                attacker = roomManager.getFirstTeam().get(currentIndex/2);
            }else{
                attacker = roomManager.getSecondTeam().get(currentIndex/2);
            }
            speaker = roomManager.getAttackTarget().get(attacker);
            roomManager.setTurn(DebateTurn.ATTACK);
        }

        MediaControlInfo mediaControlInfo = MediaControlInfo.builder()
                .speaker(speaker)
                .roomId(roomManager.getRoomId())
                .build();
        SpeakerStartResponseDto dto = SpeakerStartResponseDto.builder()
                .speaker(speaker)
                .speakerStartAt(LocalDateTime.now())
                .build();

        // signaling server 에 publish [mic on]
        redisTemplate.convertAndSend("signaling:mic:on", mediaControlInfo);
        // room 참여자들에게 broadcast
        simpMessagingTemplate.convertAndSend("/debate/room/" + roomManager.getRoomId()+"/speak/start", dto);
        taskScheduler.schedule(() -> {
            endBattleTurn(roomManager,mediaControlInfo);
        }, Instant.now().plusSeconds(5));
    }

    // 발언 종료 이후 3초 대기
    private void endBattleTurn(RoomManager roomManager, MediaControlInfo mediaControlInfo) {
        summarizeBattle(roomManager)
            .subscribe(
                null,                                // next 없음
                e -> log.error("pipeline error", e), // 에러 소비자 필수
                () -> log.info("broadcast done")     // 완료 콜백
            );


        log.info("[2페이즈 발언 종료] {}",mediaControlInfo );
        // signaling server 에 publish [mic off]
        SpeakerEndResponseDto dto = SpeakerEndResponseDto.builder()
                .speaker(mediaControlInfo.getSpeaker())
                .speakerEndAt(LocalDateTime.now())
                .build();
        redisTemplate.convertAndSend("signaling:mic:off" + roomManager.getRoomId(), mediaControlInfo);
        // room 참여자들에게 broadcast
        simpMessagingTemplate.convertAndSend("/debate/room/" + roomManager.getRoomId()+"/speak/end",dto);
        roomManager.advanceTurn();


        taskScheduler.schedule(() -> {

            startBattleTurn(roomManager);
        }, Instant.now().plusSeconds(3));
    }

    private void startVote(RoomManager roomManager) {
        if(roomManager.getStatus() ==RoomStatus.RESULT){
            log.info("[투표 종료] {}",roomManager.getStatus());
            endGame(roomManager);
            return;
        }

        log.info("[투표 진행 시작]");
        log.info("{}",roomManager.getStatus());

        VoteStartResponseDto dto = VoteStartResponseDto.builder()
                .voteStartAt(LocalDateTime.now())

                .build();
        simpMessagingTemplate.convertAndSend("/debate/room/" + roomManager.getRoomId()+"/vote/start",dto);
        taskScheduler.schedule(() -> {
            endVote(roomManager);
        }, Instant.now().plusSeconds(30));
    }

    private void endVote(RoomManager roomManager) {
        VoteEndResponseDto dto = VoteEndResponseDto.builder()
                .voteEndAt(LocalDateTime.now())
                .voteInfo(roomManager.getVoteTeam())
                .voteResult(roomManager.calculateWinner())
                .build();
        log.info("[투표 종료] {} 승리팀 : {}", roomManager.getVoteTeam(),roomManager.calculateWinner());

        simpMessagingTemplate.convertAndSend("/debate/room/" + roomManager.getRoomId()+"/vote/end",
                dto);
        roomManager.advanceTurn();
        taskScheduler.schedule(() -> {
            endGame(roomManager);
        }, Instant.now().plusSeconds(5));
    }

    private void endGame(RoomManager roomManager) {

        log.info("[GAME ENDED]");
    }

    public Mono<Void> summarizeBattle(RoomManager roomManager) {
        DebateRedisInfo debateInfo = debateRedisRepository.findByRoomId(roomManager.getRoomId());

        String attacker = roomManager.getCurrentSpeaker();
        String defender = roomManager.getDefender(attacker);
        log.info("[current attacker] >>> {}, defenser : {}",attacker,defender);

        STTAttackDefense currentTotalSTTBattle = roomManager.getCurrentTotalSTTBattle();

        // 공격자와 방어자의 입장 정보 가져오기
        String attackerPosition = getPositionByUser(roomManager, attacker);
        String defenderPosition = getPositionByUser(roomManager, defender);

        SiegeDefenseRequest request = SiegeDefenseRequest.builder()
                .topic(debateInfo.getTopicText())
                .key(SiegeDefenseRequest.Key.builder()
                        .attack(Side.builder()
                                .user_id(attacker)
                                .position(attackerPosition)
                                .text(currentTotalSTTBattle.getAttackTotalMessage())
                                .build())
                        .defense(Side.builder()
                                .user_id(defender)
                                .position(defenderPosition)
                                .text(currentTotalSTTBattle.getDefenseTotalMessage())
                                .build())
                        .build())
                .build();
        
        log.info("[battle summary req] >>> {}, idx >>> {}", request.toString()
            ,roomManager.getCurrentIndex());
        return aiService.requestSiegeDefenseSummary(roomManager.getRoomId(),
            roomManager.getCurrentIndex(),request);
    }

    /**
     * 사용자의 입장/진영 정보를 가져오는 헬퍼 메서드
     */
    private String getPositionByUser(RoomManager roomManager, String userId) {
        // 첫 번째 팀에 속하는지 확인
        if (roomManager.getFirstTeam().contains(userId)) {
            return debateRedisRepository.findByRoomId(roomManager.getRoomId()).getFirstOption();
        }
        // 두 번째 팀에 속하는지 확인
        else if (roomManager.getSecondTeam().contains(userId)) {
            return debateRedisRepository.findByRoomId(roomManager.getRoomId()).getSecondOption();
        }
        // 기본값
        return "입장 정보 없음";
    }
}
