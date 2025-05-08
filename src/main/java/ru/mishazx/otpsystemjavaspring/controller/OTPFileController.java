package ru.mishazx.otpsystemjavaspring.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import ru.mishazx.otpsystemjavaspring.model.User;
import ru.mishazx.otpsystemjavaspring.model.otp.OTPCode;
import ru.mishazx.otpsystemjavaspring.repository.UserRepository;
import ru.mishazx.otpsystemjavaspring.service.FileOTPService;
import ru.mishazx.otpsystemjavaspring.service.OTPDownloadService;
import ru.mishazx.otpsystemjavaspring.service.OTPService;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/export")
@RequiredArgsConstructor
@Slf4j
public class OTPFileController {
    private final OTPDownloadService otpDownloadService;
    private final FileOTPService fileOTPService;
    private final OTPService otpService;
    private final UserRepository userRepository;

    //Экспорт истории OTP в формате CSV для текущего пользователя
    @GetMapping("/csv")
    public ResponseEntity<Resource> exportOtpCsv(Authentication authentication) {
        String username = authentication.getName();
        log.info("Запрос на экспорт OTP истории в CSV для пользователя: {}", username);

        Resource fileResource = otpDownloadService.exportOtpHistoryToCsv(username);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION, 
                        "attachment; filename=\"otp_history_" + username + ".csv\"")
                .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .header(HttpHeaders.EXPIRES, "0")
                .body(fileResource);
    }

    //Экспорт всей истории OTP в формате CSV (только для администраторов)
    @GetMapping("/csv/all")
    public ResponseEntity<Resource> exportAllOtpCsv(Authentication authentication) {
//        String username = authentication.getName();
        log.info("Запрос на экспорт всей истории OTP в CSV");

        Resource fileResource = otpDownloadService.exportAllOtpHistoryToCsv();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION, 
                        "attachment; filename=\"all_otp_history.csv\"")
                .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .header(HttpHeaders.EXPIRES, "0")
                .body(fileResource);
    }

    // Получение списка файлов с OTP-кодами для текущего пользователя
    @GetMapping("/files")
    public ResponseEntity<?> getOtpFiles(Authentication authentication) {
        String username = authentication.getName();
        log.info("Запрос на получение списка файлов с OTP-кодами для пользователя: {}", username);

        try {
            File[] files = fileOTPService.getOTPFilesForUser(username);
            
            List<Map<String, String>> fileInfoList = new ArrayList<>();
            for (File file : files) {
                Map<String, String> fileInfo = new HashMap<>();
                fileInfo.put("name", file.getName());
                fileInfo.put("path", file.getAbsolutePath());
                fileInfo.put("size", file.length() + " bytes");
                fileInfo.put("lastModified", new Date(file.lastModified()).toString());
                fileInfoList.add(fileInfo);
            }

            return ResponseEntity.ok(fileInfoList);
        } catch (Exception e) {
            log.error("Ошибка при получении списка файлов с OTP-кодами: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(
                    Map.of("error", "Не удалось получить список файлов: " + e.getMessage()));
        }
    }

    // Получение содержимого файла с OTP-кодом
    @GetMapping("/file/{filename}")
    public ResponseEntity<?> getOtpFileContent(
            @PathVariable String filename,
            Authentication authentication) {
        
        String username = authentication.getName();
        log.info("Запрос на получение содержимого файла {} для пользователя: {}", filename, username);

        try {
            // Проверяем, что файл принадлежит текущему пользователю
            if (!filename.startsWith(username + "_")) {
                log.warn("Попытка доступа к файлу другого пользователя: {}", filename);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                        Map.of("error", "Доступ запрещен. Вы можете просматривать только свои файлы."));
            }

            String content = fileOTPService.getOTPFileContent(filename);
            
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(content);
        } catch (IOException e) {
            log.error("Ошибка при получении содержимого файла: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    Map.of("error", "Файл не найден или недоступен: " + e.getMessage()));
        }
    }

    // Генерация OTP и сохранение его в файл (защищено JWT)
    @PostMapping("/otp")
    public ResponseEntity<Map<String, Object>> generateOtpFile(Authentication authentication) {
        String username = authentication.getName();
        log.info("Запрос на генерацию OTP-кода и сохранение в файл для пользователя: {}", username);
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        Map<String, Object> response = new HashMap<>();

        try {
            OTPCode otpCode = otpService.generateFileOtpWithoutSending(user);
            
            response.put("success", true);
            response.put("message", "Код сохранен в файл");
            response.put("code", otpCode.getCode());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Ошибка при сохранении OTP в файл: {}", e.getMessage());
            response.put("success", false);
            response.put("message", "Не удалось сохранить код: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
