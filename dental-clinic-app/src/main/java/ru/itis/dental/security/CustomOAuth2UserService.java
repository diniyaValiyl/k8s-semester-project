package ru.itis.dental.security;

import org.springframework.context.annotation.Lazy;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import ru.itis.dental.entity.UserEntity;
import ru.itis.dental.service.UserService;

import java.util.Collections;
import java.util.Map;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserService userService;

    public CustomOAuth2UserService(@Lazy UserService userService) {
        this.userService = userService;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        Map<String, Object> attributes = oAuth2User.getAttributes();

        // Получаем email и имя от Google
        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");

        if (email == null) {
            email = (String) attributes.get("login");
        }
        if (name == null) {
            name = (String) attributes.get("login");
        }

        if (email == null) {
            throw new OAuth2AuthenticationException("Email not found from OAuth2 provider");
        }

        final String finalEmail = email;
        final String finalName = name;

        // Ищем пользователя по EMAIL
        UserEntity user = userService.findByEmail(finalEmail).orElseGet(() -> {
            String generatedPassword = "oauth2_" + System.currentTimeMillis();
            String userName = (finalName != null && !finalName.isEmpty()) ? finalName : finalEmail.split("@")[0];
            return userService.register(
                    userName,
                    finalEmail,
                    generatedPassword,
                    UserEntity.Role.PATIENT
            );
        });

        org.slf4j.LoggerFactory.getLogger(getClass()).info(
                "Пользователь {} вошел через Google, роль: {}",
                user.getEmail(), user.getRole().name()
        );

        return new DefaultOAuth2User(
                Collections.singleton(() -> "ROLE_" + user.getRole().name()),
                attributes,
                "name"
        );
    }
}