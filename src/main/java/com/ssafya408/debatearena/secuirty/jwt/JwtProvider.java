package com.ssafya408.debatearena.secuirty.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.Jwts.SIG;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import java.util.Date;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtProvider {

  private final JwtProperties jwtProperties;
  private SecretKey key;
  @PostConstruct
  public void init() {
    // Base64 디코딩 → SecretKey 생성 (최신 버전 대응)
    this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtProperties.getSecret()));
  }
  public String createToken(String email) {
    Date now = new Date();
    Date expiredDate = new Date(now.getTime() + jwtProperties.getExpiration());

    return Jwts.builder()
        .subject(email)
        .issuedAt(now)
        .expiration(expiredDate)
        .signWith(key, SIG.HS256)
        .compact();
  }
  
  


  //jwt 유효성 검사
  public boolean validateToken(String token) {
    try {
      Jwts.parser()
          .verifyWith(key)
          .build()
          .parseSignedClaims(token);
      return true;
    } catch (JwtException | IllegalArgumentException exception) {
      exception.printStackTrace();
      return false;
    }
  }

  //jwt에서 이메일 추출
  public String getEmail(String token) {
    Claims claims = getAllClaims(token);

    return claims.getSubject();
  }

  //jwt에서 역할 추출
  public String getRole(String token) {
    Claims claims = getAllClaims(token);

    return claims.get("role",String.class);
  }


  public Claims getAllClaims(String token) {
    return Jwts.parser()
        .verifyWith(key)
        .build()
        .parseSignedClaims(token)
        .getPayload();
  }
  
  
}
