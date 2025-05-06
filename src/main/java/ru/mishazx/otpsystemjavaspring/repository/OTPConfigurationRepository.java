package ru.mishazx.otpsystemjavaspring.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.mishazx.otpsystemjavaspring.model.otp.OTPConfiguration;

public interface OTPConfigurationRepository extends JpaRepository<OTPConfiguration, Long> {

}