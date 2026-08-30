package com.fulfillops;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fulfillops.support.NoDatabaseTestApplication;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest(
        classes = NoDatabaseTestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("no-db")
@Import(ApiErrorContractIntegrationTests.ValidationTestConfiguration.class)
class ApiErrorContractIntegrationTests {

    private static final Pattern REQUEST_ID_PATTERN = Pattern.compile("\\\"requestId\\\":\\\"([^\\\"]+)\\\"");
    private static final Pattern TIMESTAMP_PATTERN = Pattern.compile("\\\"timestamp\\\":\\\"([^\\\"]+)\\\"");

    @LocalServerPort
    private int port;

    @Test
    void invalidRequestBodyReturnsValidationErrorWithFieldDetailsAndRequestId() throws Exception {
        HttpResponse<String> response = postJson("{}");

        assertEquals(HttpStatus.BAD_REQUEST.value(), response.statusCode());
        assertStandardErrorShape(response, "VALIDATION_ERROR");
        assertTrue(response.body().contains("\"message\":\"Request validation failed.\""));
        assertTrue(response.body().contains("\"field\":\"name\""));
        assertTrue(response.body().contains("\"message\":\"must not be blank\""));
    }

    @Test
    void malformedJsonReturnsMalformedJsonErrorWithoutFieldDetailsAndWithRequestId() throws Exception {
        HttpResponse<String> response = postJson("{\"name\":");

        assertEquals(HttpStatus.BAD_REQUEST.value(), response.statusCode());
        assertStandardErrorShape(response, "MALFORMED_JSON");
        assertTrue(response.body().contains("\"message\":\"Request body is malformed.\""));
        assertTrue(response.body().contains("\"fieldErrors\":[]"));
    }

    private HttpResponse<String> postJson(String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/v1/_validation-test"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }

    private void assertStandardErrorShape(HttpResponse<String> response, String expectedCode) {
        String body = response.body();
        assertTrue(body.contains("\"code\":\"" + expectedCode + "\""));
        assertTrue(body.contains("\"status\":400"));
        assertTrue(body.contains("\"path\":\"/api/v1/_validation-test\""));

        String requestId = extract(REQUEST_ID_PATTERN, body);
        String timestamp = extract(TIMESTAMP_PATTERN, body);
        assertEquals(response.headers().firstValue("X-Request-Id").orElseThrow(), requestId);
        Instant.parse(timestamp);
    }

    private String extract(Pattern pattern, String body) {
        Matcher matcher = pattern.matcher(body);
        assertTrue(matcher.find(), "Expected field was not present in error response: " + body);
        return matcher.group(1);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class ValidationTestConfiguration {

        @Bean
        ValidationTestController validationTestController() {
            return new ValidationTestController();
        }
    }

    @RestController
    @RequestMapping("/api/v1/_validation-test")
    static class ValidationTestController {

        @PostMapping
        ResponseEntity<Void> validate(@Valid @RequestBody ValidationTestRequest request) {
            return ResponseEntity.noContent().build();
        }
    }

    record ValidationTestRequest(@NotBlank String name) {
    }
}
