package com.ssafya408.debate.security.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

//매 요청마다 JWT를 검사하고 인증 객체를 등록함
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtProvider jwtProvider;

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {
    String token = extractTokenFromCookie(request);

    // 토큰 유효성 검사
    if (token != null && jwtProvider.validateToken(token)) {
      String email = jwtProvider.getEmail(token);
      String role = jwtProvider.getRole(token);

      //인증 객체 생성
      List<GrantedAuthority> authorityList
          = List.of(new SimpleGrantedAuthority("ROLE_"+role ));

      UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
          email, null, authorityList);

      //security context 에 등록
      SecurityContextHolder.getContext().setAuthentication(auth);

    }
    filterChain.doFilter(request,response);
  }

  private String extractTokenFromCookie(HttpServletRequest request){
    Cookie[] cookies = request.getCookies();
    if(cookies==null) return null;
    for (Cookie cookie : cookies) {
      if (cookie.getName().equals("access_token")) {
        return cookie.getValue();
      }
    }
    return null;
  }
}

