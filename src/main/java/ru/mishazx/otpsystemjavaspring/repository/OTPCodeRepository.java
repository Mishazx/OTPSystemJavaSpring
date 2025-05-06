package ru.mishazx.otpsystemjavaspring.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.mishazx.otpsystemjavaspring.model.otp.OTPCode;
import ru.mishazx.otpsystemjavaspring.model.otp.OTPStatus;
import ru.mishazx.otpsystemjavaspring.model.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OTPCodeRepository extends JpaRepository<OTPCode, Long> {
    // Получить все OTP-коды для пользователя с указанным статусом
    List<OTPCode> findByUserAndStatus(User user, OTPStatus status);
    // Получить OTP-код по коду и статусу
    Optional<OTPCode> findByCodeAndStatus(String code, OTPStatus status);
    // Получить все OTP-коды с указанным статусом, которые истекли ранее указанного времени
    List<OTPCode> findByStatusAndExpiresAtBefore(OTPStatus status, LocalDateTime time);
    // Получить OTP-код по пользователю, коду и статусу
    Optional<OTPCode> findByUserAndCodeAndStatus(User user, String code, OTPStatus status);
    // Получить все OTP-коды для пользователя
    List<OTPCode> findByUser(User user);
}