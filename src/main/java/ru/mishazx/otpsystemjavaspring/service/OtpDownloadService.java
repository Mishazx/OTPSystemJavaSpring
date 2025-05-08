package ru.mishazx.otpsystemjavaspring.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import ru.mishazx.otpsystemjavaspring.model.otp.OTPCode;
import ru.mishazx.otpsystemjavaspring.model.User;
import ru.mishazx.otpsystemjavaspring.repository.OTPCodeRepository;
import ru.mishazx.otpsystemjavaspring.repository.UserRepository;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OTPDownloadService {
    private final OTPCodeRepository otpCodeRepository;
    private final UserRepository userRepository;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Экспорт истории OTP в CSV для конкретного пользователя.
     * Формирует CSV-файл динамически в памяти и возвращает его как Resource.
     *
     * @param username имя пользователя
     * @return CSV-файл с историей OTP в виде ресурса
     */
    public Resource exportOtpHistoryToCsv(String username) {
        log.info("Экспорт истории OTP в CSV для пользователя: {}", username);
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        
        String csvContent = generateOtpHistoryCsv(user);
        return new ByteArrayResource(csvContent.getBytes(StandardCharsets.UTF_8));
    }
    
    /**
     * Экспорт всей истории OTP в CSV для администраторов.
     * Формирует CSV-файл для всех пользователей, без сохранения на диск.
     *
     * @return CSV-файл со всей историей OTP в виде ресурса
     */
    public Resource exportAllOtpHistoryToCsv() {
        log.info("Экспорт всей истории OTP в CSV");
        
        StringBuilder csv = new StringBuilder();
        csv.append("ID,Пользователь,Код,Статус,Канал,Создан,Действителен до,ID операции\n");
        
        List<User> allUsers = userRepository.findAll();
        for (User user : allUsers) {
            List<OTPCode> codes = otpCodeRepository.findByUser(user);
            for (OTPCode code : codes) {
                csv.append(code.getId())
                   .append(",")
                   .append(user.getUsername())
                   .append(",")
                   .append(code.getCode())
                   .append(",")
                   .append(code.getStatus())
                   .append(",")
                   .append(code.getChannel())
                   .append(",")
                   .append(code.getCreatedAt().format(formatter))
                   .append(",")
                   .append(code.getExpiresAt().format(formatter))
                   .append(",")
                   .append(code.getOperationId())
                   .append("\n");
            }
        }
        
        return new ByteArrayResource(csv.toString().getBytes(StandardCharsets.UTF_8));
    }
    
    /**
     * Генерирует CSV-содержимое истории OTP кодов для конкретного пользователя.
     *
     * @param user пользователь, для которого формируется CSV
     * @return Строка с CSV содержимым
     */
    private String generateOtpHistoryCsv(User user) {
        List<OTPCode> codes = otpCodeRepository.findByUser(user);
        
        StringBuilder csv = new StringBuilder();
        csv.append("ID,Код,Статус,Канал,Создан,Действителен до,ID операции\n");
        
        if (codes.isEmpty()) {
            return csv.toString();
        }
        
        for (OTPCode code : codes) {
            csv.append(code.getId())
               .append(",")
               .append(code.getCode())
               .append(",")
               .append(code.getStatus())
               .append(",")
               .append(code.getChannel())
               .append(",")
               .append(code.getCreatedAt().format(formatter))
               .append(",")
               .append(code.getExpiresAt().format(formatter))
               .append(",")
               .append(code.getOperationId())
               .append("\n");
        }
        
        log.info("Сгенерирована CSV история OTP для пользователя {}, найдено {} записей", 
                user.getUsername(), codes.size());
        return csv.toString();
    }
    
    /**
     * Пример генерации произвольного текстового файла для пользователя.
     * Данный метод позволяет сформировать содержимое и отдать его в виде resource без сохранения на диск.
     *
     * @param username имя пользователя
     * @return текстовый файл в виде Resource
     */
    public Resource generateTextReport(String username) {
        log.info("Генерация текстового отчёта для пользователя: {}", username);
        
        // Пример содержимого отчёта
        String reportContent = "Отчёт для пользователя " + username + "\n"
                + "Статистика по OTP кодам:\n"
                + "Количество запросов: " + otpCodeRepository.findByUser(userRepository.findByUsername(username)
                        .orElseThrow(() -> new RuntimeException("Пользователь не найден"))).size();
        
        return new ByteArrayResource(reportContent.getBytes(StandardCharsets.UTF_8));
    }
}
