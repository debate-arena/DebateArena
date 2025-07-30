package com.arena.test04spring.dto;

public class ProduceResponse {
    private String producerId;
    private String kind;

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public String getProducerId() { return producerId; }
    public void setProducerId(String producerId) { this.producerId = producerId; }
}