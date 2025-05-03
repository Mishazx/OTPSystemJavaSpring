package ru.mishazx.otpsystemjavaspring.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.mishazx.otpsystemjavaspring.model.User;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User,Long> {

    //Ищет пользователя по логину (username).
    Optional<User> findByUsername (String login);
    //Ищет пользователя по токену для регистрации через Telegram.
    Optional<User> findByTelegramLinkToken(String token);

    //Ищет пользователя по идентификатору Telegram чата.
    Optional<User> findByTelegramChatId(Long chatId);
}
