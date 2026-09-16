package ru.itis.dental.config;

import ru.itis.dental.entity.UserEntity;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class StringToRoleConverter implements Converter<String, UserEntity.Role> {

    @Override
    public UserEntity.Role convert(String source) {
        if (source == null || source.isEmpty()) {
            return UserEntity.Role.PATIENT;
        }
        try {
            return UserEntity.Role.valueOf(source.toUpperCase());
        } catch (IllegalArgumentException e) {
            return UserEntity.Role.PATIENT;
        }
    }
}