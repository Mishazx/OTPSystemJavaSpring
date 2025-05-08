package ru.mishazx.otpsystemjavaspring.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mishazx.otpsystemjavaspring.model.otp.OTPCode;
import ru.mishazx.otpsystemjavaspring.model.otp.OTPStatus;
import ru.mishazx.otpsystemjavaspring.repository.OTPCodeRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Сервис для автоматической очистки OTP-кодов по расписанию
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OTPSchedulerService {

    private final OTPCodeRepository otpCodeRepository;
    
    @Value("${otp.cleanup.schedule:0 0 * * * *}")
    private String cleanupSchedule;

    /**
     * Автоматически запускается по расписанию для очистки устаревших OTP-кодов.
     * Находит все активные коды с истекшим сроком действия и изменяет их статус на EXPIRED.
     * Расписание настраивается через свойство otp.cleanup.schedule в application.properties.
     * По умолчанию: каждый час в начале часа (0-я минута).
     */
    @Scheduled(cron = "${otp.cleanup.schedule:0 0 * * * *}")
    @Transactional
    public void cleanupExpiredOTPCodes() {
        log.info("Запуск запланированной задачи по очистке устаревших OTP-кодов (расписание: {})", cleanupSchedule);
        
        try {
            List<OTPCode> expiredCodes = otpCodeRepository.findByStatusAndExpiresAtBefore(
                    OTPStatus.ACTIVE, 
                    LocalDateTime.now());
            
            int count = expiredCodes.size();
            
            for (OTPCode code : expiredCodes) {
                code.setStatus(OTPStatus.EXPIRED);
                otpCodeRepository.save(code);
            }
            
            log.info("Автоматически очищено {} устаревших OTP-кодов", count);
        } catch (Exception e) {
            log.error("Ошибка при автоматической очистке устаревших OTP-кодов: {}", e.getMessage(), e);
        }
    }
} 
