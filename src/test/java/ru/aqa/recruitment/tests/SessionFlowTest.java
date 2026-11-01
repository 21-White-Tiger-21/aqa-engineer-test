package ru.aqa.recruitment.tests;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import org.junit.jupiter.api.Test;
import ru.aqa.recruitment.model.Action;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.aqa.recruitment.support.TokenFactory.validToken;

@Feature("Жизненный цикл сессии")
class SessionFlowTest extends BaseApiTest {

    @Test
    @Story("Успешный путь LOGIN - ACTION - LOGOUT")
    void completesFullSessionLifecycle() {
        String token = validToken();
        externalService.stubFor(post("/auth").willReturn(ok()));
        externalService.stubFor(post("/doAction").willReturn(ok()));

        assertSuccess(client.send(token, Action.LOGIN));
        assertSuccess(client.send(token, Action.ACTION));
        assertSuccess(client.send(token, Action.LOGOUT));

        externalService.verify(1, postRequestedFor(urlEqualTo("/auth"))
                .withHeader("Content-Type", containing("application/x-www-form-urlencoded"))
                .withRequestBody(equalTo("token=" + token)));
        externalService.verify(1, postRequestedFor(urlEqualTo("/doAction"))
                .withRequestBody(equalTo("token=" + token)));
    }

    @Test
    @Story("Действие запрещено до входа")
    void rejectsActionWithoutLoginAndDoesNotCallExternalService() {
        String token = validToken();

        assertError(client.send(token, Action.ACTION), 403);

        externalService.verify(0, postRequestedFor(urlEqualTo("/doAction")));
    }

    @Test
    @Story("Неуспешная аутентификация не создаёт сессию")
    void doesNotSaveTokenWhenAuthenticationFails() {
        String token = validToken();
        externalService.stubFor(post("/auth").willReturn(serverError()));

        assertError(client.send(token, Action.LOGIN), 500);
        assertError(client.send(token, Action.ACTION), 403);

        externalService.verify(1, postRequestedFor(urlEqualTo("/auth")));
        externalService.verify(0, postRequestedFor(urlEqualTo("/doAction")));
    }

    @Test
    @Story("Ошибка внешнего сервиса при выполнении действия")
    void returnsErrorWhenActionServiceFailsButKeepsSession() {
        String token = validToken();
        externalService.stubFor(post("/auth").willReturn(ok()));
        externalService.stubFor(post("/doAction")
                .inScenario("temporary failure")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(serverError())
                .willSetStateTo("recovered"));
        externalService.stubFor(post("/doAction")
                .inScenario("temporary failure")
                .whenScenarioStateIs("recovered")
                .willReturn(ok()));

        assertSuccess(client.send(token, Action.LOGIN));
        assertError(client.send(token, Action.ACTION), 500);
        assertSuccess(client.send(token, Action.ACTION));
    }

    @Test
    @Story("Выход завершает сессию")
    void rejectsActionAfterLogout() {
        String token = validToken();
        externalService.stubFor(post("/auth").willReturn(ok()));
        externalService.stubFor(post("/doAction").willReturn(ok()));

        assertSuccess(client.send(token, Action.LOGIN));
        assertSuccess(client.send(token, Action.LOGOUT));
        assertError(client.send(token, Action.ACTION), 403);

        externalService.verify(0, postRequestedFor(urlEqualTo("/doAction")));
    }

    @Test
    @Story("Сессии разных пользователей независимы")
    void keepsUserSessionsIndependent() {
        String loggedInToken = validToken();
        String anonymousToken = validToken();
        externalService.stubFor(post("/auth").willReturn(ok()));
        externalService.stubFor(post("/doAction").willReturn(ok()));

        assertSuccess(client.send(loggedInToken, Action.LOGIN));
        assertSuccess(client.send(loggedInToken, Action.ACTION));
        assertError(client.send(anonymousToken, Action.ACTION), 403);

        externalService.verify(1, postRequestedFor(urlEqualTo("/doAction"))
                .withRequestBody(equalTo("token=" + loggedInToken)));
    }

    @Test
    @Story("Повторный вход с тем же токеном")
    void rejectsDuplicateLogin() {
        String token = validToken();
        externalService.stubFor(post("/auth").willReturn(ok()));

        assertSuccess(client.send(token, Action.LOGIN));
        assertError(client.send(token, Action.LOGIN), 409);

        externalService.verify(1, postRequestedFor(urlEqualTo("/auth")));
    }

    @Test
    @Story("Выход по неизвестному токену")
    void rejectsLogoutForUnknownToken() {
        assertError(client.send(validToken(), Action.LOGOUT), 403);
    }

    private static void assertSuccess(io.restassured.response.Response response) {
        assertThat(response.statusCode()).isEqualTo(200);
        var body = client.body(response);
        assertThat(body.result()).isEqualTo("OK");
        assertThat(body.message()).isNull();
    }

    private static void assertError(io.restassured.response.Response response, int expectedStatus) {
        assertThat(response.statusCode()).isEqualTo(expectedStatus);
        var body = client.body(response);
        assertThat(body.result()).isEqualTo("ERROR");
        assertThat(body.message()).isNotBlank();
    }
}
