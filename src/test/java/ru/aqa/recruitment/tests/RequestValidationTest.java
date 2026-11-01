package ru.aqa.recruitment.tests;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.aqa.recruitment.client.InternalServiceClient.API_KEY;
import static ru.aqa.recruitment.support.TokenFactory.validToken;

@Feature("Проверка входного запроса")
class RequestValidationTest extends BaseApiTest {

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidTokens")
    @Story("Некорректный токен отклоняется")
    void rejectsInvalidToken(String caseName, String token) {
        var response = client.sendForm(token, "LOGIN", API_KEY);

        assertErrorResponse(response);
    }

    static Stream<Arguments> invalidTokens() {
        return Stream.of(
                Arguments.of("токен отсутствует", null),
                Arguments.of("токен пустой", ""),
                Arguments.of("длина 31 символ", "A".repeat(31)),
                Arguments.of("длина 33 символа", "A".repeat(33)),
                Arguments.of("содержит строчную букву", "A".repeat(31) + "z"),
                Arguments.of("содержит спецсимвол", "A".repeat(31) + "-")
        );
    }

    @ParameterizedTest(name = "действие: {0}")
    @MethodSource("invalidActions")
    @Story("Некорректное действие отклоняется")
    void rejectsInvalidAction(String action) {
        var response = client.sendForm(validToken(), action, API_KEY);

        assertErrorResponse(response);
    }

    static Stream<String> invalidActions() {
        return Stream.of("", "login", "UNKNOWN");
    }

    @Test
    @Story("Отсутствующее действие отклоняется")
    void rejectsMissingAction() {
        assertErrorResponse(client.sendForm(validToken(), null, API_KEY));
    }

    @Test
    @Story("Запрос без API-ключа недоступен")
    void rejectsMissingApiKey() {
        var response = client.sendForm(validToken(), "LOGIN", null);

        assertThat(response.statusCode()).isEqualTo(401);
    }

    @Test
    @Story("Запрос с неверным API-ключом недоступен")
    void rejectsWrongApiKey() {
        var response = client.sendForm(validToken(), "LOGIN", "wrong-key");

        assertThat(response.statusCode()).isEqualTo(401);
    }

    private static void assertErrorResponse(io.restassured.response.Response response) {
        assertThat(response.statusCode()).isEqualTo(400);
        var body = client.body(response);
        assertThat(body.result()).isEqualTo("ERROR");
        assertThat(body.message()).isNotBlank();
    }
}
