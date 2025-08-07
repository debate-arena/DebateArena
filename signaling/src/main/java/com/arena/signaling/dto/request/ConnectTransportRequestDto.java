package com.arena.signaling.dto.request;


import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConnectTransportRequestDto {
    private String userEmail;
    private String transportId;
    private Object dtlsParameters;
    private Long roomId;
}
