package com.ssafya408.debatearena.common.security.oAuth;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CustomOAuth2SuccessHandler implements AuthenticationSuccessHandler {
  private final JwtProvider jwtProvider;
  @Override
  public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
      Authentication authentication) throws IOException, ServletException {
    OAuth2User oAuth2User=(OAuth2User) authentication.getPrincipal();
    String email = oAuth2User.getAttribute("email");
    
    // jwtProvider 로 토큰 생성
    String token=jwtProvider.createToken(email);
    // jwt 토큰 쿠키에 담아서 전송
    // 헤더에 담는 것은 js 조작으로 인해 보안상 문제가 될 수 있음
    Cookie cookie = new Cookie("access_token", token);
    cookie.setHttpOnly(true);
    cookie.setSecure(true); // 임지 주석
    cookie.setPath("/"); // 모든 경로에 자동 퐇마
    cookie.setMaxAge(60*60*24); // 1일
    response.addCookie(cookie);
    response.sendRedirect("http://localhost:3000/login/success");
  }
}
