package ru.mishazx.otpsystemjavaspring.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.mishazx.otpsystemjavaspring.model.otp.OTPCode;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileOTPService {

    @Value("${otp.file.directory:./otp_codes}")
    private String otpFileDirectory;

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    /**
     * Сохраняет OTP-код в файл
     * @param otpCode код OTP для сохранения
     * @return путь к файлу, в который был сохранен код
     */
    public String saveOTPToFile(OTPCode otpCode) {
        // Создаем директорию, если не существует
        try {
            Path directoryPath = Paths.get(otpFileDirectory);
            if (!Files.exists(directoryPath)) {
                Files.createDirectories(directoryPath);
                log.info("Создана директория для OTP-кодов: {}", directoryPath);
            }
        } catch (IOException e) {
            log.error("Не удалось создать директорию для OTP: {}", e.getMessage());
            throw new RuntimeException("Не удалось создать директорию для OTP", e);
        }

        // Генерируем имя файла в формате username_timestamp_code.txt
        String filename = String.format("%s_%s_%s.txt",
                otpCode.getUser().getUsername(),
                LocalDateTime.now().format(formatter),
                otpCode.getCode());

        String fullPath = Paths.get(otpFileDirectory, filename).toString();

        try (FileWriter writer = new FileWriter(fullPath)) {
            // Записываем информацию о OTP-коде
            writer.write("OTP Code Information\n");
            writer.write("-------------------\n");
            writer.write(String.format("Code: %s\n", otpCode.getCode()));
            writer.write(String.format("User: %s\n", otpCode.getUser().getUsername()));
            writer.write(String.format("Created At: %s\n", otpCode.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));
            writer.write(String.format("Expires At: %s\n", otpCode.getExpiresAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));
            writer.write(String.format("Operation ID: %s\n", otpCode.getOperationId()));
            writer.write(String.format("Channel: %s\n", otpCode.getChannel()));
            writer.write(String.format("Status: %s\n", otpCode.getStatus()));
            
            log.info("OTP-код {} сохранен в файл: {}", otpCode.getCode(), fullPath);
            return fullPath;
        } catch (IOException e) {
            log.error("Ошибка при сохранении OTP-кода в файл: {}", e.getMessage());
            throw new RuntimeException("Не удалось сохранить OTP-код в файл", e);
        }
    }

    /**
     * Получает список всех файлов с OTP-кодами для указанного пользователя
     * @param username имя пользователя
     * @return массив файлов с OTP-кодами
     */
    public File[] getOTPFilesForUser(String username) {
        File directory = new File(otpFileDirectory);
        if (!directory.exists() || !directory.isDirectory()) {
            log.warn("Директория для OTP-кодов не существует: {}", otpFileDirectory);
            return new File[0];
        }

        return directory.listFiles(file -> file.getName().startsWith(username + "_"));
    }

    /**
     * Получает содержимое файла с OTP-кодом
     * @param filename имя файла
     * @return содержимое файла в виде строки
     */
    public String getOTPFileContent(String filename) throws IOException {
        Path filePath = Paths.get(otpFileDirectory, filename);
        if (!Files.exists(filePath)) {
            log.warn("Файл с OTP-кодом не найден: {}", filePath);
            throw new IOException("Файл с OTP-кодом не найден");
        }
        
        return Files.readString(filePath);
    }
} 
