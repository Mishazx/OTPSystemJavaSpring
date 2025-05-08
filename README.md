# OTP System на Java Spring

Система генерации и проверки одноразовых паролей (OTP) с возможностями отправки через SMS, Email и Telegram.

## Функциональность

- Генерация OTP-кодов различной длины
- Отправка OTP-кодов через SMS, Email, Telegram
- Сохранение OTP-кодов в файлы
- Верификация OTP-кодов
- Административный интерфейс для управления пользователями и OTP-кодами
- Автоматическая очистка устаревших OTP-кодов
- JWT-аутентификация
- Разграничение доступа по ролям (USER, ADMIN)

## Технологии

- Java 17+
- Spring Boot 3.x
- Spring Security с JWT
- Spring Data JPA
- PostgreSQL
- Telegram Bot API
- SMPP для SMS
- JavaMail для Email
- Docker для запуска через compose.yaml

## Настройка и запуск

### 1. Настройка secret.properties

Для безопасного хранения секретных данных проект использует файл `secret.properties`, который не должен попадать в репозиторий.
Создайте файл `secret.properties` в корне проекта со следующим содержимым:

```properties
# Database Configuration
spring.datasource.username=postgres
spring.datasource.password=postgres

# JWT Configuration
jwt.secret=ваш_jwt_секретный_ключ_должен_быть_достаточно_длинным_минимум_32_символа
jwt.expiration=3600000

# Email Configuration (Gmail)
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=ваша_почта@gmail.com
spring.mail.password=ваш_пароль_приложения
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
spring.mail.properties.mail.smtp.starttls.required=true

# Telegram Bot Configuration
telegram.bot.username=имя_вашего_бота
telegram.bot.token=токен_вашего_бота

# Admin Configuration (optional, default values shown)
admin.username=admin
admin.password=admin
admin.email=admin@example.com
admin.phone=+79000000000

# SMS Configuration (SMPP)
smpp.host=localhost
smpp.port=2775
smpp.system-id=smppclient1
smpp.password=password
smpp.source-address=OTP
```

### 2. Настройка БД PostgreSQL

Убедитесь, что PostgreSQL запущен и доступен по указанным в настройках реквизитам.

### 3. Запуск проекта

```bash
# Запуск через Maven
./mvnw spring-boot:run

# Через Docker Compose
docker-compose up
```

### 4. Запуск SMS сервера для разработки (опционально)

Для тестирования SMS можно использовать SMPPSim:

```bash
# Запуск SMS сервера
./start_sms.sh
```

## API Endpoints

### Аутентификация
- **POST /api/auth/register** - Регистрация пользователя
- **POST /api/auth/login** - Аутентификация и получение JWT-токена

### OTP
- **POST /api/otp/sms** - Генерация и отправка OTP через SMS
- **POST /api/otp/email** - Генерация и отправка OTP через Email
- **POST /api/otp/telegram** - Генерация и отправка OTP через Telegram
- **POST /api/otp/verify** - Верификация OTP-кода

### Экспорт
- **GET /api/export/csv** - Экспорт истории OTP в CSV
- **GET /api/export/csv/all** - Экспорт всей истории OTP (для админов)
- **POST /api/export/otp** - Генерация OTP и сохранение в файл
- **GET /api/export/files** - Получение списка файлов с OTP
- **GET /api/export/file/{filename}** - Получение содержимого файла с OTP

### Telegram
- **GET /api/telegram/link** - Генерация токена для привязки Telegram
- **GET /api/telegram/status** - Проверка статуса привязки Telegram
- **POST /api/telegram/send** - Отправка тестового сообщения в Telegram
- **POST /api/telegram/verify** - Верификация OTP из Telegram

### Админ
- **GET /api/admin/users** - Получение списка пользователей
- **GET /api/admin/users/{userId}** - Получение информации о пользователе
- **PUT /api/admin/users/{userId}/toggle-status** - Блокировка/разблокировка пользователя
- **GET /api/admin/otp/active** - Получение активных OTP-кодов
- **POST /api/admin/otp/{otpId}/revoke** - Аннулирование OTP-кода
- **GET /api/admin/otp/files** - Получение всех файлов с OTP-кодами
- **POST /api/admin/otp/cleanup** - Очистка устаревших OTP-кодов

## Автоматическая очистка устаревших OTP-кодов

Система автоматически очищает устаревшие OTP-коды по расписанию. Настройка расписания:

```properties
# В application.properties можно настроить расписание очистки
# По умолчанию: каждый час в начале часа
otp.cleanup.schedule=0 0 * * * *
```

Примеры настроек расписания:
- `0 0 * * * *` - каждый час в начале часа
- `0 */15 * * * *` - каждые 15 минут
- `0 0 0 * * *` - раз в день в полночь
- `0 0 12 * * *` - раз в день в полдень

## Postman коллекции

В каталоге `/postman` находятся готовые коллекции для тестирования API:
- **Auth.postman_collection.json** - Регистрация и авторизация
- **OTP.postman_collection.json** - Работа с OTP-кодами
- **Export.postman_collection.json** - Экспорт и работа с файлами
- **LinkTelegram.postman_collection.json** - Интеграция с Telegram
- **Admin.postman_collection.json** - Административные функции 
