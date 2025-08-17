package com.ssafya408.debate.domain.api.service;

import com.ssafya408.debate.domain.api.dto.ai.DebateResultRequest;
import com.ssafya408.debate.domain.api.dto.ai.DebateResultRequest.Entire;
import com.ssafya408.debate.domain.api.dto.ai.DebateResultRequest.TeamData;
import com.ssafya408.debate.domain.api.dto.ai.DebateResultResponse;
import com.ssafya408.debate.domain.api.dto.ai.OpinionTextRequest;
import com.ssafya408.debate.domain.api.dto.ai.SiegeDefenseRequest;
import com.ssafya408.debate.domain.api.dto.ai.SiegeDefenseRequest.Side;
import com.ssafya408.debate.domain.api.dto.debate.*;
import com.ssafya408.debate.domain.api.dto.control.MediaControlInfo;
import com.ssafya408.debate.domain.api.dto.room.RoomStatus;
import com.ssafya408.debate.domain.api.dto.summary.DebateSummaryResponse.BattleSummary;
import com.ssafya408.debate.domain.api.dto.summary.DebateSummaryResponse.OpinionSummary;
import com.ssafya408.debate.domain.db.cache.DebateRedisInfo;
import com.ssafya408.debate.domain.db.cache.DebateRedisRepository;
import com.ssafya408.debate.domain.db.cache.SummaryRedisRepository;
import java.util.List;
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

    private static final int PREPARING_STAGE_TIME = 10;
    private static final int OPINION_STAGE_TIME = 30;
    private static final int OPINION_TURN_OVER_TIME = 4;
    private static final int BATTLE_VOTE_TIME = 10;
    private static final int BATTLE_STAGE_TIME = 15;
    private static final int BATTLE_TURN_OVER_TIME = 4;
    private static final int VOTING_STAGE_TIME = 10;
    private static final int VOTE_RESULT_STAGE_TIME = 4;
    private static final int AI_RESULT_STAGE_TIME = 4;

    private static final String SIGNALING_MIC_ON_CHANNEL = "signaling:mic:on";
    private static final String SIGNALING_MIC_OFF_CHANNEL = "signaling:mic:off";

    private static final String ROOM_TOPIC_PREFIX = "/sub/debate/room/";
    private static final String START_OPINION_SUFFIX = "/start/opinion";
    private static final String OPINION_SPEAK_START_SUFFIX = "/speak/start";
    private static final String OPINION_SPEAK_END_SUFFIX = "/speak/end";
    private static final String BATTLE_VOTE_START_SUFFIX = "/start/battle";
    private static final String BATTLE_ATTACK_SPEAK_START_SUFFIX = "/speak/attackStart";
    private static final String BATTLE_ATTACK_SPEAK_END_SUFFIX = "/speak/attackEnd";
    private static final String BATTLE_DEFENSE_SPEAK_START_SUFFIX = "/speak/defenseStart";
    private static final String BATTLE_DEFENSE_SPEAK_END_SUFFIX = "/speak/defenseEnd";
    private static final String VOTE_START_SUFFIX = "/vote/start";
    private static final String VOTE_END_SUFFIX = "/vote/end";

    private final SimpMessagingTemplate simpMessagingTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    @Qualifier("taskScheduler")
    private final TaskScheduler taskScheduler;
    private final DebateRedisRepository debateRedisRepository;
    private final AiService aiService;
    private final SummaryRedisRepository summaryRedisRepository;
    private final DebateUtil debateUtil;

    public void gameStart(RoomManager roomManager) {
        if (roomManager == null) return;

        startOpinion(roomManager);

    }
    // 게임 시작 대기 30초
    private void startOpinion(RoomManager roomManager) {
        if(roomManager.getStatus()!=RoomStatus.PREPARING){
            log.info("[게임 시작됨 || 이미 지나간 단계] {}",roomManager.getRoomId() );
            startBattleVote(roomManager);
            return;
        }
        log.info("[게임 시작 전 대기] room:{}",roomManager.getRoomId() );

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

        broadcastToRoom(roomManager.getRoomId(), START_OPINION_SUFFIX, dto);
        roomManager.advanceTurn();

        taskScheduler.schedule(() -> {
            startOpinionTurn(roomManager);
        }, Instant.now().plusSeconds(PREPARING_STAGE_TIME));
    }

    // 발언 제어 로직
    private void startOpinionTurn(RoomManager roomManager) {
        if (roomManager.getStatus()!=RoomStatus.OPINION) {
            log.info("[1페이즈 종료 || 이미 지나간 단계] {}",roomManager.getStatus());
            startBattleVote(roomManager);
            return;
        }

        String speaker=roomManager.getCurrentSpeaker();
        log.info("[1페이즈 발언 시작] {}",speaker);

        MediaControlInfo mediaControlInfo = MediaControlInfo.builder()
            .speaker(speaker)
            .roomId(roomManager.getRoomId())
            .build();
        SpeakerStartResponseDto dto = SpeakerStartResponseDto.builder()
                .speaker(speaker)
                .speakerStartAt(LocalDateTime.now())
                .build();

        redisTemplate.convertAndSend(SIGNALING_MIC_ON_CHANNEL, mediaControlInfo);
        broadcastToRoom(roomManager.getRoomId(), OPINION_SPEAK_START_SUFFIX, dto);

        taskScheduler.schedule(() -> {
            endOpinionTurn(roomManager,mediaControlInfo);
        }, Instant.now().plusSeconds(OPINION_STAGE_TIME));
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

        log.info("[1페이즈 발언 종료] {}",mediaControlInfo.getSpeaker() );


        redisTemplate.convertAndSend(SIGNALING_MIC_OFF_CHANNEL , mediaControlInfo);
        roomManager.advanceTurn();

        String nextSpeaker ;
        if(roomManager.getStatus()!=RoomStatus.OPINION){
            nextSpeaker = "";
        }else{
            nextSpeaker = roomManager.getCurrentSpeaker();
        }

        SpeakerEndResponseDto dto = SpeakerEndResponseDto.builder()
                .speaker(mediaControlInfo.getSpeaker())
                .speakerEndAt(LocalDateTime.now())
                .nextSpeaker(nextSpeaker)
                .build();

        broadcastToRoom(roomManager.getRoomId(), OPINION_SPEAK_END_SUFFIX, dto);

        taskScheduler.schedule(() -> {
            startOpinionTurn(roomManager);
        }, Instant.now().plusSeconds(OPINION_TURN_OVER_TIME));
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

    private void startBattleVote(RoomManager roomManager) {
        if(roomManager.getStatus()!=RoomStatus.BATTLE_VOTE){
            log.info("[공방전 시작됨 || 이미 지나간 단계] {}",roomManager.getRoomId() );
            startBattleTurn(roomManager);
            return;
        }
        log.info("[공방전 시작 전 대기 및 투표] room:{}",roomManager.getRoomId() );

        // 공방 시작 30초 남음 BROADCAST
        BattleStartResponseDto dto = BattleStartResponseDto.builder()
                .status("success")
                .battleStartAt(LocalDateTime.now())
                .build();

        broadcastToRoom(roomManager.getRoomId(), BATTLE_VOTE_START_SUFFIX, dto);
        // BATTLE_VOTE에서 BATTLE로 상태 전환
        log.info("[상태 전환] BATTLE_VOTE → BATTLE");
        roomManager.setStatus(RoomStatus.BATTLE);
        roomManager.setCurrentBattleIndex(0);
        
        taskScheduler.schedule(() -> {
            startBattleTurn(roomManager);
        }, Instant.now().plusSeconds(BATTLE_VOTE_TIME));
    }

    private void startBattleTurn(RoomManager roomManager) {
        if (roomManager.getStatus()!=RoomStatus.BATTLE) {
            if(roomManager.getAttackTarget().isEmpty()){
                log.info("[공방전 종료] 아무도 공방전 투표를 진행하지 않음 RoomId : {} ",roomManager.getRoomId());
            }
            log.info("[2페이즈 종료 || 이미 지나간 단계] {}",roomManager.getStatus());
            startVote(roomManager);

            return;
        }
        roomManager.setDefenseUsers();

        String speaker;

        /**
         * 공격 Turn 일때는 getCurrentSpeaker() 를 사용하여 speaker를 얻을 수 있음
         *  - 공격 Turn의 Speaker 가 AttackTarget 맵에 없다면 Turn을 넘긴 뒤 다시 할수를 실행함.
         * 방어 Turn 일때는 getCurrentSpeaker() 를 사용하여 공격자를 얻어낸 뒤 getDefender() 를 사용하여 speaker를 얻을 수 있음
         * 각 Turn이 넘어갈 때 endBattleTurn(...) 에서는 setTurn()을 사용하여 Turn을 변경함 (공격, 방어)
         **/
        if(roomManager.getTurn()==DebateTurn.ATTACK){
            speaker = roomManager.getCurrentSpeaker();
            String defender = roomManager.getDefender(speaker);
            if(defender==null){
                log.info("[2페이즈 발언 시작] {} 해당 유저는 공방전 투표를 하지 않았음 다음 Turn으로 넘어감",speaker);
                roomManager.advanceTurn();
                startBattleTurn(roomManager);
                return;
            }
        }else{
            String attacker = roomManager.getCurrentSpeaker();
            speaker = roomManager.getDefender(attacker);
        }
        log.info("[2페이즈 발언 시작] {} [{}]",speaker,roomManager.getTurn());

        MediaControlInfo mediaControlInfo = MediaControlInfo.builder()
                .speaker(speaker)
                .roomId(roomManager.getRoomId())
                .build();
        SpeakerStartResponseDto dto = SpeakerStartResponseDto.builder()
                .speaker(speaker)
                .speakerStartAt(LocalDateTime.now())
                .build();

        redisTemplate.convertAndSend(SIGNALING_MIC_ON_CHANNEL, mediaControlInfo);
        if(roomManager.getTurn()==DebateTurn.ATTACK){
            broadcastToRoom(roomManager.getRoomId(), BATTLE_ATTACK_SPEAK_START_SUFFIX, dto);
        }else{
            broadcastToRoom(roomManager.getRoomId(), BATTLE_DEFENSE_SPEAK_START_SUFFIX,dto);
        }
        taskScheduler.schedule(() -> {
            endBattleTurn(roomManager,mediaControlInfo);
        }, Instant.now().plusSeconds(BATTLE_STAGE_TIME));
    }

    // 발언 종료 이후 3초 대기
    private void endBattleTurn(RoomManager roomManager, MediaControlInfo mediaControlInfo) {
        log.info("[2페이즈 발언 종료] {}",mediaControlInfo.getSpeaker() );

        SpeakerEndResponseDto dto = SpeakerEndResponseDto.builder()
                .speaker(mediaControlInfo.getSpeaker())
                .speakerEndAt(LocalDateTime.now())
                .build();

        redisTemplate.convertAndSend(SIGNALING_MIC_OFF_CHANNEL , mediaControlInfo);

        /**
         * 공격 Turn 일때는 방어자에게 발언권을 주기위해 turn을 DEFENSE로 변경함
         * 방어 Turn 일때는 Turn을 넘기기 위해, advanceTurn() 을 사용함.
         */
        if(roomManager.getTurn()==DebateTurn.ATTACK){
            roomManager.setTurn(DebateTurn.DEFENSE);
            dto.setNextSpeaker(roomManager.getDefender(roomManager.getCurrentSpeaker()));
            broadcastToRoom(roomManager.getRoomId(), BATTLE_ATTACK_SPEAK_END_SUFFIX, dto);
            taskScheduler.schedule(() -> {
                startBattleTurn(roomManager);
            }, Instant.now().plusSeconds(BATTLE_TURN_OVER_TIME));
        }else{
            roomManager.setTurn(DebateTurn.ATTACK);
           summarizeBattle(roomManager)
                   .subscribe(
                           null,                                // next 없음
                           e -> log.error("pipeline error", e), // 에러 소비자 필수
                           () -> log.info("broadcast done")     // 완료 콜백
                   );
            roomManager.advanceTurn();
            String nextSpeaker ;
            if(roomManager.getStatus()!=RoomStatus.BATTLE){
                nextSpeaker = "";
            }else{
                nextSpeaker = roomManager.getCurrentSpeaker();
            }
            dto.setNextSpeaker(nextSpeaker);
            broadcastToRoom(roomManager.getRoomId(), BATTLE_DEFENSE_SPEAK_END_SUFFIX, dto);
            taskScheduler.schedule(() -> {
                startBattleTurn(roomManager);
            }, Instant.now().plusSeconds(BATTLE_TURN_OVER_TIME));
        }
    }

    private void startVote(RoomManager roomManager) {
        if(roomManager.getStatus()!=RoomStatus.VOTING){
            log.info("[이미 지나간 단계입니다.] {} ",roomManager.getStatus());
            endVote(roomManager);
            return;
        }

        log.info("[투표 진행 시작]");

        VoteStartResponseDto dto = VoteStartResponseDto.builder()
                .voteStartAt(LocalDateTime.now())
                .build();

        broadcastToRoom(roomManager.getRoomId(), VOTE_START_SUFFIX, dto);
        taskScheduler.schedule(() -> {
            roomManager.advanceTurn();
            endVote(roomManager);
        }, Instant.now().plusSeconds(VOTING_STAGE_TIME));
    }

    private void endVote(RoomManager roomManager) {
        log.info("[최종 투표 종료] roomId: {}, 현재 상태: {}", roomManager.getRoomId(), roomManager.getStatus());
        
        if(roomManager.getStatus()!=RoomStatus.VOTE_RESULT){
            log.info("[이미 지나간 단계입니다.] {}",roomManager.getStatus());
            aiResult(roomManager);
            return;
        }
        VoteEndResponseDto dto = VoteEndResponseDto.builder()
                .voteEndAt(LocalDateTime.now())
                .voteInfo(roomManager.getVoteTeam())
                .voteResult(roomManager.calculateWinner())
                .build();


        log.info("[투표 종료] 투표자:{} 승리팀:{}", roomManager.getVoteTeam(),roomManager.calculateWinner());

        broadcastToRoom(roomManager.getRoomId(), VOTE_END_SUFFIX, dto);
        roomManager.advanceTurn();
        taskScheduler.schedule(() -> {
            aiResult(roomManager);
        }, Instant.now().plusSeconds(VOTE_RESULT_STAGE_TIME));
    }

    private void aiResult(RoomManager roomManager) {
        if(roomManager.getStatus()!=RoomStatus.AI_RESULT){
            log.info("[이미 지나간 단계입니다.] {}",roomManager.getStatus());
            endGame(roomManager);
            return;
        }

        taskScheduler.schedule(() -> {
            endGame(roomManager);
        }, Instant.now().plusSeconds(AI_RESULT_STAGE_TIME));
    }

    private void endGame(RoomManager roomManager) {
        log.info("[게임 종료] {} ",roomManager.getStatus());
        if(roomManager.getStatus()!=RoomStatus.FINISH){
            log.info("[정상적으로 게임이 종료되지 않음.] {}",roomManager.getStatus());
            return;
        }
        debateUtil.deleteRoomInInMemory(roomManager.getRoomId());
        debateRedisRepository.delete(roomManager.getRoomId().toString());
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

    public Mono<Void> summarizeTotalText(RoomManager roomManager) {
        DebateRedisInfo redisRoom = debateRedisRepository.findByRoomId(roomManager.getRoomId());

        StringBuilder firstTeamText = new StringBuilder();
        StringBuilder secondTeamText = new StringBuilder();
        List<OpinionSummary> opinionSummaries = summaryRedisRepository.getOpinionSummaries(
            roomManager.getRoomId());
        List<BattleSummary> battleSummaries = summaryRedisRepository.getBattleSummaries(
            roomManager.getRoomId());

        int firstTeamScore=0, secondTeamScore=0;
        for (int i = 0; i < roomManager.getPlayerCount(); i++) {
            if(i<opinionSummaries.size()){
                OpinionSummary opinionSummary = opinionSummaries.get(i);
                if (opinionSummary.getTeam().equals("first")) {
                        firstTeamText.append(opinionSummary.getText()).append(" ");
                } else {
                    secondTeamText.append(opinionSummary.getText()).append(" ");
                }
            }

            if(i<battleSummaries.size()){
                BattleSummary battleSummary = battleSummaries.get(i);
                if (battleSummary.getAttack_team().equals("first")) {
                    firstTeamText.append(battleSummary.getText()).append(" ");
                    firstTeamScore+=battleSummary.getRebuttal_score();
                } else {
                    secondTeamText.append(battleSummary.getText()).append(" ");
                    secondTeamScore+= battleSummary.getRebuttal_score();
                }
            }


        }


        double firstTeamAvg = ((double) firstTeamScore) / roomManager.getFirstTeam().size();
        double secondTeamAvg = ((double) secondTeamScore) / roomManager.getSecondTeam().size();
        log.info("first team score:{}\n summary >>> {}",firstTeamAvg, firstTeamText.toString());
        log.info("second team score:{}\n >>> {}", secondTeamAvg, secondTeamText.toString());

        DebateResultRequest resultRequest = DebateResultRequest.builder()
            .topic(redisRoom.getTopicText())
            .draw(true)
            .entire(
                Entire.builder()
                    .num1(
                        TeamData.builder()
                            .position(redisRoom.getFirstOption())
                            .text(firstTeamText.toString())
                            .rebuttal_score(firstTeamAvg)
                            .build()
                    )
                    .num2(
                        TeamData.builder()
                            .position(redisRoom.getSecondOption())
                            .text(secondTeamText.toString())
                            .rebuttal_score(secondTeamAvg)
                            .build()
                    )
                    .build()
            )
            .build();
        return aiService.requestDebateResult(roomManager.getRoomId(), resultRequest);

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

    private void broadcastToRoom(Long roomId, String suffix, Object message) {

        String s = ROOM_TOPIC_PREFIX+roomId+suffix;
        log.info("[broadcast] : {}",s);
        simpMessagingTemplate.convertAndSend(s, message);
        
        ChatResponseDto responseDto = ChatResponseDto.builder()
            .message(LocalDateTime.now().toString())
            .nickname("server")
            .build();

        String url = "/sub/debate/room/"+roomId+"/chat";

        simpMessagingTemplate.convertAndSend(url, responseDto);
    }

    
}
