package ru.mishazx.otpsystemjavaspring.service;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import ru.mishazx.otpsystemjavaspring.model.User;
import ru.mishazx.otpsystemjavaspring.repository.UserRepository;

@RequiredArgsConstructor
@Slf4j
@Service
public class TelegramLinkService {

    private final UserRepository userRepository;
    private final Map<String, String> linkTokenStorage = new ConcurrentHashMap<>();

    public Long getUserTelegramChatId(String username) {
        Optional<User> userOptional = userRepository.findByUsername(username);
        return userOptional.map(User::getTelegramChatId).orElse(null);
    }

    public String generateLinkToken(String username) {
        String token = UUID.randomUUID().toString();

        linkTokenStorage.put(token, username);

        log.info("Сгенерирован токен привязки для пользователя {}: {}", username, token);
        return token;
    }


    @Transactional
    public boolean directLinkTelegramAccount(String username, Long chatId) {
        log.info("Прямая привязка Telegram chatId {} к пользователю {}", chatId, username);
        
        try {
            Optional<User> userOptional = userRepository.findByUsername(username);
            if (userOptional.isPresent()) {
                User user = userOptional.get();
                user.setTelegramChatId(chatId);
                userRepository.save(user);
                
                log.info("Успешно привязан Telegram chatId {} к пользователю {}", chatId, username);
                return true;
            } else {
                log.warn("Пользователь {} не найден при попытке прямой привязки Telegram", username);
                return false;
            }
        } catch (Exception e) {
            log.error("Ошибка при прямой привязке Telegram к пользователю {}: {}", username, e.getMessage(), e);
            return false;
        }
    }

    @Transactional
    public boolean linkTelegramAccount(String token, Long chatId) {
        log.info("Попытка привязки Telegram для chatId {} с токеном {}", chatId, token);

        String username = linkTokenStorage.get(token);

        if (username != null) {
            try {
                Optional<User> userOptional = userRepository.findByUsername(username);
                if (userOptional.isPresent()) {
                    User user = userOptional.get();
                    user.setTelegramChatId(chatId);
                    userRepository.save(user);

                    linkTokenStorage.remove(token);

                    log.info("Успешно связан Telegram (chatId: {}) с пользователем {}", chatId, username);
                    return true;
                }
            } catch (Exception e) {
                log.error("Ошибка при привязке Telegram аккаунта: {}", e.getMessage(), e);
            }
        } else {

            try {
                Optional<User> userOptional = userRepository.findByTelegramLinkToken(token);
                if (userOptional.isPresent()) {
                    User user = userOptional.get();
                    user.setTelegramChatId(chatId);
                    user.setTelegramLinkToken(null);
                    userRepository.save(user);

                    log.info("Успешно связан Telegram (chatId: {}) с пользователем {} (из базы данных)",
                            chatId, user.getUsername());
                    return true;
                }
            } catch (Exception e) {
                log.error("Ошибка при проверке токена в базе данных: {}", e.getMessage(), e);
            }
        }

        log.warn("Не удалось связать Telegram аккаунт: токен {} не найден", token);
        return false;
    }



}
