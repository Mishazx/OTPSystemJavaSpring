package ru.mishazx.otpsystemjavaspring.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import ru.mishazx.otpsystemjavaspring.service.TelegramLinkService;
import ru.mishazx.otpsystemjavaspring.model.User;
import ru.mishazx.otpsystemjavaspring.model.otp.OTPCode;
import ru.mishazx.otpsystemjavaspring.repository.UserRepository;
import ru.mishazx.otpsystemjavaspring.service.*;
// import ru.mishazx.otpsystemjavaspring.service.TelegramBotService;
// import ru.mishazx.otpsystemjavaspring.service.SMSService;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/otp")
@RequiredArgsConstructor
@Slf4j
public class OTPController {
    private final TelegramLinkService telegramLinkService;
    private final TelegramBotService telegramBotService;
    private final OTPService otpService;
    private final SMSService smsService;
    private final EmailService emailService;

    private final UserRepository userRepository;

    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyOTP(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String code = request.get("code");
        Map<String, Object> response = new HashMap<>();

        if (username == null || username.isEmpty() || code == null || code.isEmpty()) {
            response.put("success", false);
            response.put("message", "Имя пользователя и код должны быть указаны");
            return ResponseEntity.badRequest().body(response);
        }

        User user = userRepository.findByUsername(username)
                .orElse(null);

        if (user == null) {
            response.put("success", false);
            response.put("message", "Пользователь не найден");
            return ResponseEntity.badRequest().body(response);
        }

        boolean verified = otpService.verify(user, code);

        if (verified) {
            response.put("success", true);
            response.put("message", "Код подтвержден");
        } else {
            response.put("success", false);
            response.put("message", "Неверный код или истек срок действия");
        }

        return ResponseEntity.ok(response);
    }

    @PostMapping("/telegram")
    public ResponseEntity<?> sendOTPTelegram(@RequestBody Map<String, String> request) {
        String username = request.get("username");

        Long chatId = telegramLinkService.getUserTelegramChatId(username);

        if (chatId == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Telegram не привязан к вашему аккаунту");
            return ResponseEntity.badRequest().body(response);
        }

        boolean success = telegramBotService.sendOTPForUser(chatId);

        Map<String, Object> response = new HashMap<>();
        if (success) {
            response.put("success", true);
            response.put("message", "Код отправлен в ваш Telegram");
            return ResponseEntity.ok(response);
        } else {
            response.put("success", false);
            response.put("message", "Не удалось отправить OTP код");
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PostMapping("/email")
    public ResponseEntity<Map<String, Object>> sendOtpEmail(@RequestBody Map<String, String> request) {

        String username = request.get("username");
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Map<String, Object> response = new HashMap<>();

        String email = user.getEmail();

        if (email == null || email.isEmpty()) {
            response.put("success", false);
            response.put("message", "Email должен быть указан в профиле");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            OTPCode otpCode = otpService.generateEmailOtpWithoutSending(user, email);

            emailService.sendOTPEmail(email, otpCode.getCode());

            response.put("success", true);
            response.put("message", "Код отправлен на email");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Ошибка при отправке OTP на email: {}", e.getMessage());
            response.put("success", false);
            response.put("message", "Не удалось отправить код: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PostMapping("/sms")
    public ResponseEntity<Map<String, Object>> sendOtpSms(
        @RequestBody Map<String, String> request) {
        
        String username = request.get("username");
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String phone = user.getPhone();

        Map<String, Object> response = new HashMap<>();

        try {
            OTPCode otpCode = otpService.generateSmsOtpWithoutSending(user, phone);

            smsService.sendOTPCode(phone, otpCode.getCode());
            
            response.put("success", true);
            response.put("message", "Код отправлен по SMS");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Ошибка при отправке OTP по SMS: {}", e.getMessage());
            response.put("success", false);
            response.put("message", "Не удалось отправить код: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
