package ru.mishazx.otpsystemjavaspring.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.mishazx.otpsystemjavaspring.model.User;
import ru.mishazx.otpsystemjavaspring.repository.UserRepository;
import ru.mishazx.otpsystemjavaspring.service.TelegramBotService;
import ru.mishazx.otpsystemjavaspring.service.TelegramLinkService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import java.security.Principal;
import java.util.Map;

import java.util.HashMap;

@RestController
@RequestMapping("/api/telegram")
@RequiredArgsConstructor
@Slf4j
public class LinkTelegramController {
    private final TelegramBotService telegramBotService;
    private final TelegramLinkService telegramLinkService;
    private final UserRepository userRepository;

    //Генерация токена для связывания аккаунта с Telegram (требует JWT)
    @GetMapping("/link")
    public ResponseEntity<Map<String, String>> generateLinkToken(Authentication authentication) {
        String username = authentication.getName();
        String token = telegramLinkService.generateLinkToken(username);

        String botUsername = telegramBotService.getBotUsername();
        String telegramDeepLink = "https://t.me/" + botUsername + "?start=link_" + token;

        Map<String, String> response = new HashMap<>();
        response.put("token", token);
        response.put("deepLink", telegramDeepLink);

        return ResponseEntity.ok(response);
    }


    //Проверка статуса связи с Telegram (требует JWT)
    @GetMapping("/status")
    public ResponseEntity<?> getTelegramStatus(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
        }
        
        String username = principal.getName();
        User user = userRepository.findByUsername(username).orElse(null);
        
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "User not found"));
        }
        
        Long chatId = user.getTelegramChatId();
        boolean isLinked = chatId != null && chatId > 0;
        
        return ResponseEntity.ok(Map.of("linked", isLinked));
    }


    //Проверка кода и связывание аккаунта Telegram (требует JWT)
    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyLink(
            Authentication authentication,
            @RequestBody Map<String, String> request) {
        
        String code = request.get("code");
        String username = authentication.getName();
        
        Map<String, Object> response = new HashMap<>();
        
        if (code == null || code.isEmpty()) {
            response.put("success", false);
            response.put("message", "Код должен быть указан");
            return ResponseEntity.badRequest().body(response);
        }
        
        try {
            log.info("Начало проверки кода Telegram: '{}' для пользователя: '{}'", code, username);

            Map<String, Long> activeTokens = telegramBotService.getActiveLinkTokens();
            log.info("Активные токены привязки: {}", activeTokens);

            Long chatId = telegramBotService.getChatIdByToken(code);
            
            if (chatId == null) {
                log.warn("ChatId не найден для кода: '{}' (пользователь: '{}')", code, username);
                response.put("success", false);
                response.put("message", "Неверный код или истек срок его действия. Пожалуйста, получите новый код, нажав на кнопку \"Привязать аккаунт\" в Telegram боте.");
                return ResponseEntity.ok(response);
            }
            
            log.info("Найден chatId: {} для кода: '{}' (пользователь: '{}')", chatId, code, username);

            boolean status = telegramLinkService.directLinkTelegramAccount(username, chatId);
            log.info("Привязка напрямую выполнена для пользователя {} с chatId {}", username, chatId);

            telegramBotService.removeToken(code);
            
            response.put("success", true);
            response.put("message", "Telegram аккаунт успешно привязан");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Ошибка при связывании аккаунта: ", e);
            response.put("success", false);
            response.put("message", "Ошибка при связывании аккаунта: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
