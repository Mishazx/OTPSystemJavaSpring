package ru.mishazx.otpsystemjavaspring.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.mishazx.otpsystemjavaspring.service.TelegramBotService;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class TelegramBotConfiguration {

    private final TelegramBotService telegramBotService;

    @Bean
    public TelegramBotsApi telegramBotsApi() throws TelegramApiException {
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
        try {
            botsApi.registerBot(telegramBotService);
            log.info("Telegram бот успешно зарегистрирован");
        } catch (TelegramApiException e) {
            log.error("Ошибка при регистрации Telegram бота: {}", e.getMessage(), e);
            throw e;
        }
        return botsApi;
    }
} 