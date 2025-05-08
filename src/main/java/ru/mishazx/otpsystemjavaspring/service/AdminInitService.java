package ru.mishazx.otpsystemjavaspring.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import ru.mishazx.otpsystemjavaspring.model.User;
import ru.mishazx.otpsystemjavaspring.model.role.RoleUser;
import ru.mishazx.otpsystemjavaspring.repository.RoleRepository;
import ru.mishazx.otpsystemjavaspring.repository.UserRepository;

import java.util.HashSet;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class AdminInitService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.username:admin}")
    private String adminUsername;

    @Value("${admin.password:admin}")
    private String adminPassword;

    @Value("${admin.email:admin@example.com}")
    private String adminEmail;

    @Value("${admin.phone:+79000000000}")
    private String adminPhone;

    @PostConstruct
    public void initAdminUser() {
        if (userRepository.findByUsername(adminUsername).isEmpty()) {
            log.info("Администратор не найден, создаю нового администратора с логином: {}", adminUsername);
            
            RoleUser adminRole = roleRepository.findByNameRole("ROLE_ADMIN")
                    .orElseGet(() -> {
                        log.warn("Роль ROLE_ADMIN не найдена, создаю новую");
                        RoleUser newRole = new RoleUser("ROLE_ADMIN");
                        return roleRepository.save(newRole);
                    });
            
            Set<RoleUser> roles = new HashSet<>();
            roles.add(adminRole);
            
            User admin = User.builder()
                    .username(adminUsername)
                    .password(passwordEncoder.encode(adminPassword))
                    .email(adminEmail)
                    .phone(adminPhone)
                    .enabled(true)
                    .roleUsers(roles)
                    .build();
            
            userRepository.save(admin);
            log.info("Администратор успешно создан!");
        } else {
            log.info("Администратор уже существует, инициализация не требуется");
        }
    }
} 
