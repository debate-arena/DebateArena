package com.ssafya408.debatearena.common.auth.oAuth;

import com.ssafya408.debatearena.common.secuirty.db.User;
import com.ssafya408.debatearena.common.secuirty.db.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

  private final UserRepository userRepository;

  @Override
  public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
    OAuth2User oAuth2User = super.loadUser(userRequest);
    Custom2OAuthUser customUser = new Custom2OAuthUser(oAuth2User.getAttributes());

    String email = customUser.getEmail();
    String name = customUser.getName();

    userRepository.findByEmail(email)
        .orElseGet(()->{
          User user=User.builder()
              .email(email)
              .provider(customUser.getProvider())
              .build();

          return userRepository.save(user);
        });

    return customUser;
  }
}
