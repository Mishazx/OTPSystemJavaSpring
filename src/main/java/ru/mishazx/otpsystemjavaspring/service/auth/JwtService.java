package ru.mishazx.otpsystemjavaspring.service.auth;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Date;
import java.util.List;


//Сервис для работы с JWT токенами.
public interface JwtService {
    // Извлечь имя пользователя из токена
    String extractUsername(String token);
    // Извлечь все утверждения (claims) из токена
    Date extractExpiration(String token);
    // Извлечь все роли из токена
    List<String> extractRoles(String token);
    // Сгенерировать JWT токен для указанного пользователя
    String generateToken(Authentication authentication);
    // Проверить, является ли токен действительным для данного пользователя
    boolean isTokenValid(String token, UserDetails userDetails);
}
