# CrptApi

## Описание
**CrptApi** — это Java-библиотека для взаимодействия с API Честного Знака (ГИС МП) с поддержкой:
- Формирования документа в формате JSON.
- Отправки HTTP-запросов с подписью.
- Ограничения частоты запросов (rate limiting) с настраиваемым окном времени.

Проект написан с учётом требований из технического задания и легко расширяем за счёт модульной архитектуры.

---

## Возможности
- Поддержка настраиваемого лимита запросов (`requests per SECOND | MINUTE | HOUR`).
- Автоматическое ожидание при превышении лимита (thread-safe).
- Простое создание и заполнение документов через класс `Document`.
- Отправка POST-запроса с `clientToken` и `userName` в заголовках.
- Гибкая структура данных для дальнейшего расширения.

---

## Установка

### 1. Клонируйте репозиторий
```bash
git clone https://github.com/yourusername/CrptApi.git
cd CrptApi
```
### 2. Соберите проект с помощью Maven
```bash
mvn clean install
```

---

## Использование
```java
import ru.zolotuhin.CrptApi.CrptApi;
import ru.zolotuhin.CrptApi.CrptApi.Document;

public class Main {
    public static void main(String[] args) throws Exception {
        CrptApi api = new CrptApi(CrptApi.TimeUnit.SECOND, 1);

        Document document = new Document(
            "1234567890", "doc-001", "DRAFT", "LP_INTRODUCE_GOODS",
            "0987654321", "1122334455",
            "2025-08-12", "type", "2025-08-12", "reg-001"
        );

        api.runRequest(document, "digital-signature");
    }
}
```

---

## Тестирование
Проект использует JUnit 5 для тестирования.

Запуск тестов:
```bash
mvn test
```

---

## Стек
- Java 17+
- Maven — сборка и управление зависимостями
- JUnit 5 — модульное тестирование
- SLF4J + Logback — логирование
- Jackson — сериализация/десериализация JSON
- Apache HttpClient — отправка HTTP-запросов






