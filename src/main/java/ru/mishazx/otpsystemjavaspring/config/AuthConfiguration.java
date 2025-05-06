package ru.mishazx.otpsystemjavaspring.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import ru.mishazx.otpsystemjavaspring.service.auth.AuthService;

@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class AuthConfiguration {
    private final AuthService authService;
     private final JWTAuthConfiguration jwtAuthConfiguration;

    //Провайдер аутентификации для логина и пароля
    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(authService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    //Конфигурация безопасности для API
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        log.info("Configuring security filter chain");
        
        return http
            // Отключаем CSRF для REST API
            .csrf(AbstractHttpConfigurer::disable)
            
            // Настраиваем политику безопасности для запросов
            .authorizeHttpRequests(auth -> {

                // Публичные эндпоинты (не требуют аутентификации)
                auth.requestMatchers(new AntPathRequestMatcher("/api/auth/**")).permitAll();
                auth.requestMatchers(new AntPathRequestMatcher("/api/otp/**")).permitAll();

                // Защищенные эндпоинты (требуют аутентификации)
                auth.requestMatchers(new AntPathRequestMatcher("/api/telegram/**")).authenticated();
                // Все остальные запросы требуют аутентификации

                auth.anyRequest().authenticated();
            })
            

            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // Настраиваем провайдер аутентификации
            .authenticationProvider(authenticationProvider())

            // Добавляем JWT фильтр перед стандартным фильтром аутентификации
            .addFilterBefore(jwtAuthConfiguration, UsernamePasswordAuthenticationFilter.class)

            // Строим конфигурацию
            .build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(authService);
        provider.setPasswordEncoder(passwordEncoder());
        log.info("Configured DaoAuthenticationProvider with userService and passwordEncoder");
        return provider;
    }

    //Предоставляет AuthenticationManager для JWT аутентификации
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    //Все пароли в базе хранятся не в чистом виде, а в виде bcrypt
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
