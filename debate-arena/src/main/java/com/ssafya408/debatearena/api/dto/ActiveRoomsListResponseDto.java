package com.ssafya408.debatearena.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActiveRoomsListResponseDto {
    private List<ActiveRoomResponseDto> activeRooms;
    private Integer totalCount;
    private String timestamp; // LocalDateTime 대신 String 사용
    private String message;
    
    public static ActiveRoomsListResponseDto of(List<ActiveRoomResponseDto> rooms) {
        return ActiveRoomsListResponseDto.builder()
                .activeRooms(rooms)
                .totalCount(rooms.size())
                .timestamp(java.time.LocalDateTime.now().toString())
                .message("활성 토론방 목록을 성공적으로 조회했습니다.")
                .build();
    }
    
    public static ActiveRoomsListResponseDto empty() {
        return ActiveRoomsListResponseDto.builder()
                .activeRooms(List.of())
                .totalCount(0)
                .timestamp(java.time.LocalDateTime.now().toString())
                .message("현재 활성화된 토론방이 없습니다.")
                .build();
    }
}
