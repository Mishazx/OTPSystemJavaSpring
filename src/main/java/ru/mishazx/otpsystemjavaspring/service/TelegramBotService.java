package ru.mishazx.otpsystemjavaspring.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.mishazx.otpsystemjavaspring.model.User;
import ru.mishazx.otpsystemjavaspring.model.otp.OTPCode;
import ru.mishazx.otpsystemjavaspring.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class TelegramBotService extends TelegramLongPollingBot {

    @Value("${telegram.bot.username}")
    private String botUsername;

    @Value("${telegram.bot.token}")
    private String botToken;

    private final TelegramLinkService telegramLinkService;
    private final OTPService otpService;

    private final UserRepository userRepository;

    private final Map<Long, String> activeOTPCodes = new ConcurrentHashMap<>();
    private final Map<String, Long> linkTokens = new ConcurrentHashMap<>();

    public TelegramBotService(
            TelegramLinkService telegramLinkService,
            OTPService otpService,
            UserRepository userRepository,
            @Value("${telegram.bot.token}") String botToken) {
        super(botToken);
        this.telegramLinkService = telegramLinkService;
        this.otpService = otpService;
        this.userRepository = userRepository;
        this.botToken = botToken;
    }

    @PostConstruct
    public void initCommands() {
        try {
            List<BotCommand> commandList = new ArrayList<>();
            commandList.add(new BotCommand("/start", "Запустить бота"));
            commandList.add(new BotCommand("/code", "Получить OTP-код"));
            commandList.add(new BotCommand("/link", "Привязать аккаунт"));
            commandList.add(new BotCommand("/help", "Получить помощь"));

            execute(new SetMyCommands(commandList, new BotCommandScopeDefault(), null));
            log.info("Бот успешно инициализировал команды");
        } catch (TelegramApiException e) {
            log.error("Ошибка при инициализации команд бота", e);
        }
    }

    // Обработка сообщений от пользователей
    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            Long chatId = update.getMessage().getChatId();

            if (messageText.startsWith("/start")) {
                handleStartCommand(messageText, chatId);
            } else if (messageText.startsWith("/link_")) {
                String token = messageText.substring(6);
                linkAccount(chatId, token);
            } else if (messageText.startsWith("/link") || messageText.equals("Привязать аккаунт")) {
                handleLinkCommand(chatId);
            } else if (messageText.startsWith("/unlink") || messageText.equals("Отвязать аккаунт")) {
                handleUnlinkCommand(chatId);
            } else if (messageText.startsWith("/code") || messageText.equals("Получить код")) {
                handleSendCode(chatId);
            } else if (messageText.startsWith("/help") || messageText.equals("Получить помощь")) {
                handleHelpCommand(messageText, chatId);
            }
        }
    }

    private void handleSendCode(Long chatId) {
        String otpCode = otpService.generateOTP();
        activeOTPCodes.put(chatId, otpCode);

        SendMessage message = createMessageWithKeyboard(chatId,
                "🔐 Ваш код подтверждения: *" + otpCode + "*\n\n" +
                        "Код действителен в течение 5 минут. " +
                        "Введите его на странице авторизации.");

        message.enableMarkdown(true);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Ошибка при отправке OTP-кода", e);
        }
    }

    private void handleHelpCommand(String messageText, Long chatId) {
        SendMessage message = createMessageWithKeyboard(chatId, """
                ℹ️ Справка по использованию бота:
                1️⃣ Для получения кода нажмите кнопку «Получить код» или используйте команду /code
                2️⃣ Если ваш аккаунт уже привязан к Telegram, система сможет автоматически отправлять вам коды
                3️⃣ Коды действительны в течение 5 минут после генерации
                4️⃣ При возникновении проблем обратитесь в техническую поддержку
                """);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Ошибка при отправке справочного сообщения", e);
        }
    }

    private void handleLinkCommand(Long chatId) {
        // Проверяем, привязан ли уже аккаунт
        boolean isLinked = userRepository.findByTelegramChatId(chatId).isPresent();
        
        if (isLinked) {
            SendMessage message = createMessageWithKeyboard(chatId, 
                "⚠️ Ваш аккаунт уже привязан к Telegram. Используйте команду /unlink для отвязки аккаунта.");
            
            try {
                execute(message);
            } catch (TelegramApiException e) {
                log.error("Ошибка при отправке сообщения о статусе привязки", e);
            }
            return;
        }
        
        String token = UUID.randomUUID().toString();

        log.info("Генерация нового токена привязки для chatId: {}: {}", chatId, token);
        for (Map.Entry<String, Long> entry : linkTokens.entrySet()) {
            if (entry.getValue().equals(chatId)) {
                log.info("Удаляем устаревший токен для chatId {}: {}", chatId, entry.getKey());
                linkTokens.remove(entry.getKey());
            }
        }

        sendLinkCode(chatId, token);
    }

    private void handleUnlinkCommand(Long chatId) {
        // Проверяем, привязан ли аккаунт
        User user = userRepository.findByTelegramChatId(chatId).orElse(null);
        
        if (user == null) {
            SendMessage message = createMessageWithKeyboard(chatId, 
                "❗ Ваш аккаунт не привязан к Telegram. Используйте команду /link для привязки аккаунта.");
            
            try {
                execute(message);
            } catch (TelegramApiException e) {
                log.error("Ошибка при отправке сообщения о статусе привязки", e);
            }
            return;
        }
        
        // Отвязываем аккаунт
        user.setTelegramChatId(null);
        userRepository.save(user);
        
        SendMessage message = createMessageWithKeyboard(chatId,
                "✅ Ваш аккаунт успешно отвязан от Telegram.\n" +
                "Вы больше не будете получать автоматические коды подтверждения через бота.");
        
        try {
            execute(message);
            log.info("Аккаунт успешно отвязан от Telegram для chatId: {}", chatId);
        } catch (TelegramApiException e) {
            log.error("Ошибка при отправке сообщения об отвязке аккаунта", e);
        }
    }

    private void handleStartCommand(String messageText, Long chatId) {
        String[] parts = messageText.split(" ", 2);

        if (parts.length > 1 && parts[1].startsWith("link_")) {
            String token = parts[1].substring(5);
            linkAccount(chatId, token);
        } else {
            sendWelcomeMessage(chatId);
        }
    }

    private void sendWelcomeMessage(Long chatId) {
        SendMessage message = createMessageWithKeyboard(chatId,
                """
                        Добро пожаловать в сервис OTP-авторизации! ⚙️

                        Здесь вы можете получить коды подтверждения для входа в систему:

                            Нажмите кнопку «Получить код» для генерации нового OTP. 🔑
                        При привязанном аккаунте коды будут отправляться автоматически с сайта. 🔄
                        Для дополнительной информации выберите «Помощь». ℹ️
                        """);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Ошибка при отправке приветственного сообщения", e);
        }
    }

    private SendMessage createMessageWithKeyboard(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);

        List<KeyboardRow> keyboard = new ArrayList<>();

        // Проверяем, привязан ли аккаунт
        boolean isLinked = userRepository.findByTelegramChatId(chatId).isPresent();

        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton("Получить код"));
        
        // Показываем "Отвязать аккаунт" или "Привязать аккаунт" в зависимости от статуса
        if (isLinked) {
            row1.add(new KeyboardButton("Отвязать аккаунт"));
        } else {
            row1.add(new KeyboardButton("Привязать аккаунт"));
        }

        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton("Получить помощь"));

        keyboard.add(row1);
        keyboard.add(row2);

        keyboardMarkup.setKeyboard(keyboard);
        message.setReplyMarkup(keyboardMarkup);

        return message;
    }

    private void linkAccount(long chatId, String token) {
        boolean success = telegramLinkService.linkTelegramAccount(token, chatId);

        if (success) {
            SendMessage message = createMessageWithKeyboard(chatId,
                    """
                            ✅ Ваш Telegram успешно привязан к аккаунту.
                            Теперь вы можете получать коды подтверждения через этот чат.

                            При авторизации система будет автоматически отправлять коды.
                            """);

            try {
                execute(message);
            } catch (TelegramApiException e) {
                log.error("Ошибка при отправке сообщения об успешной привязке", e);
            }
        } else {
            String newToken = UUID.randomUUID().toString();
            sendLinkCode(chatId, newToken);

            SendMessage message = createMessageWithKeyboard(chatId,
                    "ℹ️ Для привязки Telegram к аккаунту скопируйте код, указанный выше, и введите его на сайте.");

            try {
                execute(message);
            } catch (TelegramApiException e) {
                log.error("Ошибка при отправке сообщения о неудачной привязке", e);
            }
        }
    }

    public void sendLinkCode(long chatId, String token) {
        linkTokens.put(token, chatId);

        SendMessage message = createMessageWithKeyboard(chatId,
                "🔑 Ваш код для привязки аккаунта: *" + token + "*\n\n" +
                        "Скопируйте этот код и вставьте его на сайте в поле 'Код из Telegram'.\n" +
                        "⚠️ Код действителен только для одной привязки.");

        message.enableMarkdown(true);

        try {
            execute(message);
            log.info("Отправлен код привязки для chatId: {} с токеном: {}", chatId, token);
        } catch (TelegramApiException e) {
            log.error("Ошибка при отправке кода привязки", e);
        }
    }

    public boolean sendOTPForUser(Long chatId) {
        try {
            User user = userRepository.findByTelegramChatId(chatId).orElse(null);
            if (user == null) {
                log.error("Пользователь с chatId {} не найден", chatId);
                return false;
            }

            OTPCode otpCode = otpService.generateTelegramOtpWithoutSending(user, String.valueOf(chatId));

            String code = otpCode.getCode();

            activeOTPCodes.put(chatId, code);

            SendMessage message = createMessageWithKeyboard(chatId,
                    "🔐 Ваш код подтверждения: *" + code + "*\n\n" +
                            "Код действителен в течение 5 минут. " +
                            "Введите его на странице авторизации.");

            message.enableMarkdown(true);

            try {
                execute(message);
                log.info("OTP код {} успешно отправлен пользователю через Telegram", code);
                return true;
            } catch (TelegramApiException e) {
                log.error("Ошибка при отправке сообщения с кодом: {}", e.getMessage(), e);
                return false;
            }
        } catch (Exception e) {
            log.error("Ошибка при отправке OTP-кода пользователю: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public String getBotUsername() {
        // Получение имени бота из конфигурации
        return botUsername;
    }

    public Long getChatIdByToken(String token) {
        log.info("Поиск chatId по токену: {}", token);
        Long chatId = linkTokens.get(token);

        if (chatId != null) {
            log.info("Найден chatId: {} для токена: {}", chatId, token);
        } else {
            log.warn("ChatId не найден для токена: {}. Текущие токены: {}", token, linkTokens.keySet());
        }

        return chatId;
    }

    public void removeToken(String token) {
        if (linkTokens.containsKey(token)) {
            Long chatId = linkTokens.remove(token);
            log.info("Токен {} удален из хранилища после успешной привязки для chatId {}", token, chatId);
        } else {
            log.warn("Попытка удалить несуществующий токен: {}", token);
        }
    }

    public Map<String, Long> getActiveLinkTokens() {
        log.info("Активные токены: {}", linkTokens);
        return linkTokens;
    }

}
