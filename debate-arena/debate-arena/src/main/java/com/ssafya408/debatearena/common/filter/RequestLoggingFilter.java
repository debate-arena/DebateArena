package com.ssafya408.debatearena.common.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class RequestLoggingFilter implements Filter {

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    
    HttpServletRequest httpRequest = (HttpServletRequest) request;
    HttpServletResponse httpResponse = (HttpServletResponse) response;
    
    String requestURI = httpRequest.getRequestURI();
    String method = httpRequest.getMethod();
    
    // OAuth2 관련 요청만 로깅
    if (requestURI.startsWith("/oauth2/") || requestURI.startsWith("/login/oauth2/")) {
      long startTime = System.currentTimeMillis();
      
      log.info("🔍 OAuth2 요청 시작:");
      log.info("   - Method: {}", method);
      log.info("   - URI: {}", requestURI);
      log.info("   - Query String: {}", httpRequest.getQueryString());
      log.info("   - Remote Address: {}", httpRequest.getRemoteAddr());
      log.info("   - User Agent: {}", httpRequest.getHeader("User-Agent"));
      log.info("   - Referer: {}", httpRequest.getHeader("Referer"));
      
      try {
        chain.doFilter(request, response);
        
        long duration = System.currentTimeMillis() - startTime;
        log.info("✅ OAuth2 요청 완료:");
        log.info("   - Status: {}", httpResponse.getStatus());
        log.info("   - Duration: {}ms", duration);
        log.info("   - Location Header: {}", httpResponse.getHeader("Location"));
        
      } catch (Exception e) {
        long duration = System.currentTimeMillis() - startTime;
        log.error("❌ OAuth2 요청 실패:");
        log.error("   - Duration: {}ms", duration);
        log.error("   - Error: {}", e.getMessage(), e);
        throw e;
      }
    } else {
      chain.doFilter(request, response);
    }
  }
}


