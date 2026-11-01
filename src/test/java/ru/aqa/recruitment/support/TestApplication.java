package ru.aqa.recruitment.support;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

public final class TestApplication implements AutoCloseable {
    private static final Duration START_TIMEOUT = Duration.ofSeconds(30);
    private final Process process;

    private TestApplication(Process process) {
        this.process = process;
    }

    public static TestApplication start(Path jar, String mockUrl) throws IOException {
        if (!Files.isRegularFile(jar)) {
            throw new IllegalStateException("Application JAR not found: " + jar.toAbsolutePath());
        }

        Path log = Path.of("target", "application-under-test.log");
        Files.createDirectories(log.getParent());
        Process process = new ProcessBuilder(
                javaExecutable(),
                "-Dsecret=" + "qazWSXedc",
                "-Dmock=" + mockUrl,
                "-jar",
                jar.toAbsolutePath().toString())
                .redirectErrorStream(true)
                .redirectOutput(ProcessBuilder.Redirect.appendTo(log.toFile()))
                .start();

        TestApplication application = new TestApplication(process);
        application.awaitReady();
        return application;
    }

    private void awaitReady() {
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(1)).build();
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:8080/endpoint"))
                .timeout(Duration.ofSeconds(1))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        long deadline = System.nanoTime() + START_TIMEOUT.toNanos();

        while (System.nanoTime() < deadline) {
            if (!process.isAlive()) {
                throw new IllegalStateException("Application stopped during startup, exit code: " + process.exitValue());
            }
            try {
                client.send(request, HttpResponse.BodyHandlers.discarding());
                return;
            } catch (IOException | InterruptedException ignored) {
                if (ignored instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Interrupted while waiting for application", ignored);
                }
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Interrupted while waiting for application", e);
                }
            }
        }
        throw new IllegalStateException("Application did not start within " + START_TIMEOUT);
    }

    private static String javaExecutable() {
        return Path.of(System.getProperty("java.home"), "bin", "java").toString();
    }

    @Override
    public void close() {
        process.destroy();
        try {
            if (!process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS)) {
                process.destroyForcibly();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
        }
    }
}
