package com.ssafya408.debatearena.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 닉네임 업데이트 요청 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public  class NicknameUpdateRequest {
  @NotBlank(message = "닉네임은 필수입니다.")
  @Size(min = 2, max = 16, message = "닉네임은 2자 이상 20자 이하여야 합니다.")
  private String nickname;

  // Getter and Setter
  public String getNickname() {
    return nickname;
  }

  public void setNickname(String nickname) {
    this.nickname = nickname;
  }
}
