package ru.aqa.recruitment.tests;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.qameta.allure.Epic;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import ru.aqa.recruitment.client.InternalServiceClient;
import ru.aqa.recruitment.support.TestApplication;

import java.nio.file.Path;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

@Epic("API управления пользовательской сессией")
abstract class BaseApiTest {
    protected static WireMockServer externalService;
    protected static InternalServiceClient client;
    private static TestApplication application;

    @BeforeAll
    static void startEnvironment() throws Exception {
        externalService = new WireMockServer(wireMockConfig().port(8888));
        externalService.start();
        application = TestApplication.start(
                Path.of("app", "internal-0.0.1-SNAPSHOT.jar"),
                externalService.baseUrl());
        client = new InternalServiceClient("http://localhost:8080");
    }

    @BeforeEach
    void resetExternalService() {
        externalService.resetAll();
    }

    @AfterAll
    static void stopEnvironment() {
        if (application != null) {
            application.close();
        }
        if (externalService != null) {
            externalService.stop();
        }
    }
}
