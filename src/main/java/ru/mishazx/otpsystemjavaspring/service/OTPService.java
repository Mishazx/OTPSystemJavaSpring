package ru.mishazx.otpsystemjavaspring.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mishazx.otpsystemjavaspring.model.User;
import ru.mishazx.otpsystemjavaspring.model.otp.OTPChannel;
import ru.mishazx.otpsystemjavaspring.model.otp.OTPCode;
import ru.mishazx.otpsystemjavaspring.model.otp.OTPConfiguration;
import ru.mishazx.otpsystemjavaspring.model.otp.OTPStatus;
import ru.mishazx.otpsystemjavaspring.repository.OTPCodeRepository;
import ru.mishazx.otpsystemjavaspring.repository.OTPConfigurationRepository;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@AllArgsConstructor
public class OTPService {

    private final SecureRandom random = new SecureRandom();
    private final OTPCodeRepository otpCodeRepository;
    private final OTPConfigurationRepository otpConfigRepository;

    public String generateOTP(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    public String generateOTP() {
        return generateOTP(6);
    }

    public OTPConfiguration getOrCreateDefaultConfig() {
        return otpConfigRepository.findById(1L).orElseGet(() -> {
            OTPConfiguration config = new OTPConfiguration();
            config.setId(1L);
            config.setCodeLength(6);
            config.setLifetimeMinutes(5);
            return otpConfigRepository.save(config);
        });
    }

    @Transactional
    public OTPCode generateOtpWithoutSending(User user, String destination, OTPChannel channel) {
        // Закрываем все предыдущие активные OTP для пользователя
        List<OTPCode> activeCodes = otpCodeRepository.findByUserAndStatus(user, OTPStatus.ACTIVE);
        for (OTPCode old : activeCodes) {
            old.setStatus(OTPStatus.EXPIRED);
            otpCodeRepository.save(old);
        }

        OTPConfiguration config = getOrCreateDefaultConfig();
        String code = generateOTP(config.getCodeLength());

        OTPCode otpCode = new OTPCode();
        otpCode.setCode(code);
        otpCode.setStatus(OTPStatus.ACTIVE);
        otpCode.setCreatedAt(LocalDateTime.now());
        otpCode.setExpiresAt(LocalDateTime.now().plusMinutes(config.getLifetimeMinutes()));
        otpCode.setUser(user);
        otpCode.setOperationId(UUID.randomUUID().toString());
        otpCode.setChannel(channel);

        otpCode = otpCodeRepository.save(otpCode);

        // Логирование с указанием канала. Можно добавить более детальное логирование, если необходимо.
        if (channel == OTPChannel.TELEGRAM) {
            log.info("Generated OTP {} for Telegram chat {}", code, destination);
        } else if (channel == OTPChannel.SMS) {
            log.info("Generated OTP {} for SMS to {}", code, destination);
        } else if (channel == OTPChannel.EMAIL) {
            log.info("Generated OTP {} for Email to {}", code, destination);
        } else {
            log.info("Generated OTP {} for {} destination {}", code, channel, destination);
        }

        return otpCode;
    }

    // Обёртки для удобного вызова
    @Transactional
    public OTPCode generateTelegramOtpWithoutSending(User user, String chatId) {
        return generateOtpWithoutSending(user, chatId, OTPChannel.TELEGRAM);
    }

    @Transactional
    public OTPCode generateSmsOtpWithoutSending(User user, String phoneNumber) {
        return generateOtpWithoutSending(user, phoneNumber, OTPChannel.SMS);
    }

    @Transactional
    public OTPCode generateEmailOtpWithoutSending(User user, String email) {
        return generateOtpWithoutSending(user, email, OTPChannel.EMAIL);
    }

    @Transactional
    public boolean verify(User user, String code) {
        log.info("Попытка верификации кода для пользователя {}: код={}", user.getUsername(), code);

        List<OTPCode> activeCodes = otpCodeRepository.findByUserAndStatus(user, OTPStatus.ACTIVE);
        log.info("Всего активных кодов для пользователя {}: {}", user.getUsername(), activeCodes.size());

        for (OTPCode activeCode : activeCodes) {
            log.info("Активный код: {}, создан: {}, истекает: {}",
                    activeCode.getCode(), activeCode.getCreatedAt(), activeCode.getExpiresAt());
        }

        Optional<OTPCode> otpOpt = otpCodeRepository.findByUserAndCodeAndStatus(user, code, OTPStatus.ACTIVE);

        if (otpOpt.isEmpty()) {
            log.warn("No active OTP found for user {} with code {}", user.getUsername(), code);
            return false;
        }

        OTPCode otp = otpOpt.get();
        log.info("Найден код: {}, создан: {}, истекает: {}",
                otp.getCode(), otp.getCreatedAt(), otp.getExpiresAt());

        if (LocalDateTime.now().isAfter(otp.getExpiresAt())) {
            otp.setStatus(OTPStatus.EXPIRED);
            otpCodeRepository.save(otp);
            log.warn("OTP expired for user {}. Expiry time: {}, Current time: {}",
                    user.getUsername(), otp.getExpiresAt(), LocalDateTime.now());
            return false;
        }

        otp.setStatus(OTPStatus.USED);
        otpCodeRepository.save(otp);
        log.info("OTP verified successfully for user {}", user.getUsername());

        return true;
    }
}
