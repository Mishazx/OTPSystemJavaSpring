package ru.mishazx.otpsystemjavaspring.model.otp;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import ru.mishazx.otpsystemjavaspring.model.User;

import java.time.LocalDateTime;

@Entity
@Table(name = "otp_code")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OTPCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String code;

    @Enumerated(EnumType.STRING)
    private OTPStatus status;

    @Enumerated(EnumType.STRING)
    private OTPChannel channel;

    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;

    @ManyToOne
    private User user;

    private String operationId;

}