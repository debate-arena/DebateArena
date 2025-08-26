package com.ssafya408.debatearena.service.auth.oAuth;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

public class Custom2OAuthUser implements OAuth2User {
  private final Map<String,Object> attributes;

  public Custom2OAuthUser(Map<String, Object> attributes) {
    this.attributes = attributes;
  }

  @Override
  public Map<String, Object> getAttributes() {
    return attributes;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return Collections.singleton(new SimpleGrantedAuthority("ROLE_USER"));
  }

  @Override
  public String getName() {
    return (String) attributes.get("sub");
  }

  public String getEmail() {
    return (String) attributes.get("email");
  }


  public String getProvider() {
    return "google";
  }
}
