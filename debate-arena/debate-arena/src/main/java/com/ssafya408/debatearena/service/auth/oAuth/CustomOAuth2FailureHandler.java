package com.ssafya408.debatearena.service.auth.oAuth;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2FailureHandler implements AuthenticationFailureHandler {

  @Value("${app.frontend.base-url:http://localhost:3000}")
  private String frontendBaseUrl;

  @Value("${app.frontend.failure-path:/login/failure}")
  private String loginFailurePath;

  @Override
  public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
      AuthenticationException exception) throws IOException, ServletException {
    
    // 🔍 OAuth 인증 실패 로그
    log.error("❌ OAuth2 인증 실패 - 요청 정보:");
    log.error("   - Request URI: {}", request.getRequestURI());
    log.error("   - Request URL: {}", request.getRequestURL());
    log.error("   - Remote Address: {}", request.getRemoteAddr());
    log.error("   - User Agent: {}", request.getHeader("User-Agent"));
    log.error("   - Error: {}", exception.getMessage(), exception);
    
    // 리다이렉트 URL 구성
    String redirectUrl = frontendBaseUrl + loginFailurePath + "?error=" + 
        java.net.URLEncoder.encode(exception.getMessage(), "UTF-8");
    
    log.error("🔍 실패 페이지로 리다이렉트 시도:");
    log.error("   - Frontend Base URL: {}", frontendBaseUrl);
    log.error("   - Failure Path: {}", loginFailurePath);
    log.error("   - Full Redirect URL: {}", redirectUrl);
    
    try {
      response.sendRedirect(redirectUrl);
      log.info("✅ 실패 리다이렉트 성공 - URL: {}", redirectUrl);
    } catch (Exception e) {
      log.error("❌ 실패 리다이렉트 실패 - URL: {}, Error: {}", redirectUrl, e.getMessage(), e);
      throw e;
    }
  }
}


