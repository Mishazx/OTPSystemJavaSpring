package ru.mishazx.otpsystemjavaspring.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import ru.mishazx.otpsystemjavaspring.model.User;
import ru.mishazx.otpsystemjavaspring.model.api.APIResponse;
import ru.mishazx.otpsystemjavaspring.repository.UserRepository;
import ru.mishazx.otpsystemjavaspring.service.RoleService;
import ru.mishazx.otpsystemjavaspring.service.auth.JwtService;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleService roleService;
//    private final UserRoleService userRoleService;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    //
    @PostMapping("/register")
    public ResponseEntity<APIResponse<?>> registerUser(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");

        if (username == null || password == null) {
            log.warn("Попытка регистрации с пустым именем пользователя или паролем");
            return ResponseEntity.badRequest().body(new APIResponse<>("error", "Имя пользователя и пароль обязательны", null));
        }

        if (userRepository.findByUsername(username).isPresent()) {
            log.warn("Попытка регистрации с существующим именем пользователя: {}", username);
            return ResponseEntity.badRequest().body(new APIResponse<>("error", "Пользователь с таким именем уже существует", null));
        }

        try {
            User newUser = User.builder()
                    .username(username)
                    .password(passwordEncoder.encode(password))
                    .enabled(true)
                    .accountNonExpired(true)
                    .accountNonLocked(true)
                    .credentialsNonExpired(true)
                    .build();

            boolean isAdmin = roleService.assignRolesForNewUser(newUser);
            User savedUser = userRepository.save(newUser);

            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password)
            );

            String token = jwtService.generateToken(authentication);

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("token", token);
            responseData.put("id", savedUser.getId());
            responseData.put("username", savedUser.getUsername());
            responseData.put("roles", savedUser.getRoleUsers().stream()
                    .map(role -> role.getNameRole())
                    .toList());
            responseData.put("isAdmin", isAdmin);

            log.info("Успешная регистрация пользователя: {}, isAdmin: {}, токен выдан", username, isAdmin);
            return ResponseEntity.ok(new APIResponse<>("ok", "Регистрация успешна", responseData));
        } catch (Exception e) {
            log.error("Ошибка при регистрации пользователя: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(new APIResponse<>("error", "Ошибка при регистрации пользователя: " + e.getMessage(), null));
        }
    }

    //
    @PostMapping("/login")
    public ResponseEntity<APIResponse<?>> loginUser(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");

        if (username == null || password == null) {
            log.warn("Попытка авторизации с пустым именем пользователя или паролем");
            return ResponseEntity.badRequest().body(new APIResponse<>("error", "Имя пользователя и пароль обязательны", null));
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password)
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            String token = jwtService.generateToken(authentication);

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("token", token);
            responseData.put("username", username);
            responseData.put("roles", authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList()));

            log.info("Успешная авторизация пользователя: {}", username);
            return ResponseEntity.ok(new APIResponse<>("ok", "Авторизация успешна", responseData));
        } catch (Exception e) {
            log.warn("Ошибка при авторизации пользователя: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new APIResponse<>("error", "Неверное имя пользователя или пароль", null));
        }
    }

    // Изменение пароля пользователя
    @PostMapping("/change-password")
    public ResponseEntity<APIResponse<?>> changePassword(
            Authentication authentication,
            @RequestBody Map<String, String> request) {

        String currentPassword = request.get("currentPassword");
        String newPassword = request.get("newPassword");

        if (currentPassword == null || newPassword == null) {
            return ResponseEntity.badRequest().body(new APIResponse<>("error", "Текущий и новый пароли должны быть указаны", null));
        }

        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            log.warn("Попытка изменения пароля с неверным текущим паролем для пользователя: {}", username);
            return ResponseEntity.badRequest().body(new APIResponse<>("error", "Текущий пароль неверен", null));
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        log.info("Пароль успешно изменен для пользователя: {}", username);
        return ResponseEntity.ok(new APIResponse<>("ok", "Пароль успешно изменен", null));
    }

    // Получение информации о текущем пользователе
    @GetMapping("/profile")
    public ResponseEntity<APIResponse<?>> getUserProfile(Authentication authentication) {
        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        Map<String, Object> profile = new HashMap<>();
        profile.put("id", user.getId());
        profile.put("username", user.getUsername());
        profile.put("roles", user.getRoleUsers().stream()
                .map(role -> role.getNameRole())
                .collect(Collectors.toList()));

        return ResponseEntity.ok(new APIResponse<>("ok", "Информация о пользователе получена", profile));
    }

    //Получает информацию о текущем статусе аутентификации пользователя.

    @GetMapping("/status")
    public ResponseEntity<?> getAuthStatus() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() ||
                authentication.getPrincipal().equals("anonymousUser")) {
            log.debug("Запрос статуса аутентификации: не аутентифицирован");
            return ResponseEntity.ok(Map.of(
                    "authenticated", false,
                    "message", "Пользователь не аутентифицирован"
            ));
        }

        String authType = authentication.getClass().getSimpleName();

        Map<String, Object> response = new HashMap<>();
        response.put("authenticated", true);
        response.put("username", authentication.getName());
        response.put("authType", authType);
        response.put("roles", authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList()));

        log.debug("Запрос статуса аутентификации: аутентифицирован как {} через {}",
                authentication.getName(), authType);

        return ResponseEntity.ok(response);
    }
}