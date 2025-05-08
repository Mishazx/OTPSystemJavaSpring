package ru.mishazx.otpsystemjavaspring.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import ru.mishazx.otpsystemjavaspring.model.User;
import ru.mishazx.otpsystemjavaspring.model.otp.OTPCode;
import ru.mishazx.otpsystemjavaspring.model.otp.OTPStatus;
import ru.mishazx.otpsystemjavaspring.model.role.RoleUser;
import ru.mishazx.otpsystemjavaspring.repository.OTPCodeRepository;
import ru.mishazx.otpsystemjavaspring.repository.RoleRepository;
import ru.mishazx.otpsystemjavaspring.repository.UserRepository;
import ru.mishazx.otpsystemjavaspring.service.FileOTPService;
import ru.mishazx.otpsystemjavaspring.service.OTPDownloadService;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {
    
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final OTPCodeRepository otpCodeRepository;
    private final FileOTPService fileOTPService;
    private final OTPDownloadService otpDownloadService;
    
    // Получение списка всех пользователей
    @GetMapping("/users")
    public ResponseEntity<List<Map<String, Object>>> getAllUsers(Authentication authentication) {
        log.info("Запрос на получение списка всех пользователей от администратора: {}", authentication.getName());
        
        List<User> users = userRepository.findAll();
        List<Map<String, Object>> userDTOs = users.stream()
                .map(user -> {
                    Map<String, Object> userDTO = new HashMap<>();
                    userDTO.put("id", user.getId());
                    userDTO.put("username", user.getUsername());
                    userDTO.put("email", user.getEmail());
                    userDTO.put("phone", user.getPhone());
                    userDTO.put("enabled", user.isEnabled());
                    userDTO.put("roles", user.getRoleUsers().stream()
                            .map(RoleUser::getNameRole)
                            .collect(Collectors.toList()));
                    userDTO.put("telegramLinked", user.getTelegramChatId() != null);
                    return userDTO;
                })
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(userDTOs);
    }
    
    // Получение детальной информации о пользователе
    @GetMapping("/users/{userId}")
    public ResponseEntity<?> getUserDetails(@PathVariable Long userId, Authentication authentication) {
        log.info("Запрос на получение информации о пользователе id={} от администратора: {}", 
                userId, authentication.getName());
        
        return userRepository.findById(userId)
                .map(user -> {
                    Map<String, Object> userDTO = new HashMap<>();
                    userDTO.put("id", user.getId());
                    userDTO.put("username", user.getUsername());
                    userDTO.put("email", user.getEmail());
                    userDTO.put("phone", user.getPhone());
                    userDTO.put("enabled", user.isEnabled());
                    userDTO.put("roles", user.getRoleUsers().stream()
                            .map(RoleUser::getNameRole)
                            .collect(Collectors.toList()));
                    userDTO.put("telegramChatId", user.getTelegramChatId());
                    userDTO.put("telegramLinkToken", user.getTelegramLinkToken());
                    
                    // Получаем историю OTP кодов пользователя
                    List<OTPCode> otpCodes = otpCodeRepository.findByUser(user);
                    List<Map<String, Object>> otpHistory = otpCodes.stream()
                            .map(code -> {
                                Map<String, Object> otpDTO = new HashMap<>();
                                otpDTO.put("id", code.getId());
                                otpDTO.put("code", code.getCode());
                                otpDTO.put("status", code.getStatus());
                                otpDTO.put("channel", code.getChannel());
                                otpDTO.put("createdAt", code.getCreatedAt());
                                otpDTO.put("expiresAt", code.getExpiresAt());
                                otpDTO.put("operationId", code.getOperationId());
                                return otpDTO;
                            })
                            .collect(Collectors.toList());
                    userDTO.put("otpHistory", otpHistory);
                    
                    return ResponseEntity.ok(userDTO);
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    // Блокировка/разблокировка пользователя
    @PutMapping("/users/{userId}/toggle-status")
    public ResponseEntity<?> toggleUserStatus(@PathVariable Long userId, Authentication authentication) {
        log.info("Запрос на изменение статуса пользователя id={} от администратора: {}", 
                userId, authentication.getName());
        
        return userRepository.findById(userId)
                .map(user -> {
                    user.setEnabled(!user.isEnabled());
                    User savedUser = userRepository.save(user);
                    
                    Map<String, Object> response = new HashMap<>();
                    response.put("id", savedUser.getId());
                    response.put("username", savedUser.getUsername());
                    response.put("enabled", savedUser.isEnabled());
                    response.put("message", savedUser.isEnabled() ? 
                            "Пользователь активирован" : "Пользователь деактивирован");
                    
                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    // Получение всех активных OTP кодов
    @GetMapping("/otp/active")
    public ResponseEntity<List<Map<String, Object>>> getActiveOTPCodes(Authentication authentication) {
        log.info("Запрос на получение всех активных OTP кодов от администратора: {}", 
                authentication.getName());
        
        List<OTPCode> activeCodes = otpCodeRepository.findByStatusAndExpiresAtBefore(
                OTPStatus.ACTIVE, 
                java.time.LocalDateTime.now().plusYears(1));
        
        List<Map<String, Object>> codesList = activeCodes.stream()
                .map(code -> {
                    Map<String, Object> codeDTO = new HashMap<>();
                    codeDTO.put("id", code.getId());
                    codeDTO.put("code", code.getCode());
                    codeDTO.put("status", code.getStatus());
                    codeDTO.put("channel", code.getChannel());
                    codeDTO.put("createdAt", code.getCreatedAt());
                    codeDTO.put("expiresAt", code.getExpiresAt());
                    codeDTO.put("operationId", code.getOperationId());
                    codeDTO.put("username", code.getUser().getUsername());
                    return codeDTO;
                })
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(codesList);
    }
    
    // Аннулирование конкретного OTP кода
    @PostMapping("/otp/{otpId}/revoke")
    public ResponseEntity<?> revokeOTPCode(@PathVariable Long otpId, Authentication authentication) {
        log.info("Запрос на аннулирование OTP кода id={} от администратора: {}", 
                otpId, authentication.getName());
        
        return otpCodeRepository.findById(otpId)
                .map(code -> {
                    if (code.getStatus() != OTPStatus.ACTIVE) {
                        return ResponseEntity.badRequest().body(
                                Map.of("error", "Этот OTP код уже не активен"));
                    }
                    
                    code.setStatus(OTPStatus.EXPIRED);
                    OTPCode savedCode = otpCodeRepository.save(code);
                    
                    log.info("OTP код id={} аннулирован администратором: {}", 
                            otpId, authentication.getName());
                    
                    Map<String, Object> response = new HashMap<>();
                    response.put("id", savedCode.getId());
                    response.put("code", savedCode.getCode());
                    response.put("status", savedCode.getStatus());
                    response.put("message", "OTP код аннулирован");
                    
                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    // Получение всех файлов с OTP кодами
    @GetMapping("/otp/files")
    public ResponseEntity<?> getAllOTPFiles(Authentication authentication) {
        log.info("Запрос на получение всех файлов с OTP кодами от администратора: {}", 
                authentication.getName());
        
        try {
            // Получаем всех пользователей
            List<User> users = userRepository.findAll();
            
            // Для каждого пользователя получаем его файлы с OTP кодами
            Map<String, List<Map<String, String>>> allFiles = new HashMap<>();
            
            for (User user : users) {
                String username = user.getUsername();
                File[] files = fileOTPService.getOTPFilesForUser(username);
                
                if (files.length > 0) {
                    List<Map<String, String>> fileInfoList = new ArrayList<>();
                    for (File file : files) {
                        Map<String, String> fileInfo = new HashMap<>();
                        fileInfo.put("name", file.getName());
                        fileInfo.put("path", file.getAbsolutePath());
                        fileInfo.put("size", file.length() + " bytes");
                        fileInfo.put("lastModified", new Date(file.lastModified()).toString());
                        fileInfoList.add(fileInfo);
                    }
                    allFiles.put(username, fileInfoList);
                }
            }
            
            return ResponseEntity.ok(allFiles);
        } catch (Exception e) {
            log.error("Ошибка при получении списка файлов с OTP кодами: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(
                    Map.of("error", "Не удалось получить список файлов: " + e.getMessage()));
        }
    }
    
    // Очистка устаревших OTP кодов
    @PostMapping("/otp/cleanup")
    public ResponseEntity<?> cleanupExpiredOTPCodes(Authentication authentication) {
        log.info("Запрос на очистку устаревших OTP кодов от администратора: {}", 
                authentication.getName());
        
        try {
            List<OTPCode> expiredCodes = otpCodeRepository.findByStatusAndExpiresAtBefore(
                    OTPStatus.ACTIVE, 
                    java.time.LocalDateTime.now());
            
            int count = expiredCodes.size();
            
            for (OTPCode code : expiredCodes) {
                code.setStatus(OTPStatus.EXPIRED);
                otpCodeRepository.save(code);
            }
            
            log.info("Очищено {} устаревших OTP кодов администратором: {}", 
                    count, authentication.getName());
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Очищено " + count + " устаревших OTP кодов",
                    "count", count
            ));
        } catch (Exception e) {
            log.error("Ошибка при очистке устаревших OTP кодов: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "Ошибка при очистке устаревших OTP кодов: " + e.getMessage()
            ));
        }
    }
}
