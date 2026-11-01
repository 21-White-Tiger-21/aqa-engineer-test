package ru.aqa.recruitment.client;

import io.qameta.allure.Step;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.response.Response;
import ru.aqa.recruitment.model.Action;
import ru.aqa.recruitment.model.ApiResponse;

import java.util.LinkedHashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;

public final class InternalServiceClient {
    public static final String API_KEY = "qazWSXedc";
    private final String baseUrl;

    public InternalServiceClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    @Step("Отправить действие {action} для токена {token}")
    public Response send(String token, Action action) {
        return sendForm(token, action == null ? null : action.name(), API_KEY);
    }

    @Step("Отправить запрос с произвольными параметрами")
    public Response sendForm(String token, String action, String apiKey) {
        Map<String, Object> form = new LinkedHashMap<>();
        if (token != null) {
            form.put("token", token);
        }
        if (action != null) {
            form.put("action", action);
        }

        var request = given()
                .filter(new AllureRestAssured())
                .baseUri(baseUrl)
                .accept("application/json")
                .contentType("application/x-www-form-urlencoded")
                .formParams(form);
        if (apiKey != null) {
            request.header("X-Api-Key", apiKey);
        }
        return request.when().post("/endpoint").then().extract().response();
    }

    public ApiResponse body(Response response) {
        return response.as(ApiResponse.class);
    }
}
