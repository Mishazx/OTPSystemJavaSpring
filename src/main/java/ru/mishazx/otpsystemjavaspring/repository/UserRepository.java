package ru.mishazx.otpsystemjavaspring.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.mishazx.otpsystemjavaspring.model.User;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User,Long> {
    // Ищет пользователя по логину (username).
    Optional<User> findByUsername (String login);
    // Ищет пользователя по почте (Email).
    Optional<User> findByEmail (String email);
    // Ищет пользователя по токен для регистрации через Telegram.
    Optional<User> findByTelegramLinkToken(String token);
    // Ищет пользователя по номеру телефона
    Optional<User> findByPhone(String phone);
    // Ищет пользователя по идентификатору Telegram чата.
    Optional<User> findByTelegramChatId(Long chatId);
    // Получить номер телефон пользователя по логину пользователя.
    User findPhoneNumberByUsername(String username);
}
