package com.microservices.api.core;

import io.restassured.response.Response;
import org.testng.Assert;

import java.util.List;
import java.util.Map;

public class ResponseValidator {
    // ---------------- Status Code Validation ----------------
    public static void verifyStatusCode(Response response, int expectedStatus) {
        Assert.assertEquals(response.getStatusCode(), expectedStatus,
                "Expected status code " + expectedStatus + " but got " + response.getStatusCode());
    }

    // ---------------- Response Logging ----------------
    public static void logResponse(Response response) {
        System.out.println("--------- RESPONSE ---------");
        System.out.println("Status Code: " + response.getStatusCode());
        System.out.println("Headers: " + response.getHeaders());
        System.out.println("Body: " + response.getBody().asPrettyString());
        System.out.println("----------------------------");
    }

    // ---------------- JSON Extraction ----------------
    public static String getString(Response response, String jsonPath) {
        return response.jsonPath().getString(jsonPath);
    }

    public static int getInt(Response response, String jsonPath) {
        return response.jsonPath().getInt(jsonPath);
    }

    public static boolean getBoolean(Response response, String jsonPath) {
        return response.jsonPath().getBoolean(jsonPath);
    }

    public static List<Object> getList(Response response, String jsonPath) {
        return response.jsonPath().getList(jsonPath);
    }

    public static Map<String, Object> getMap(Response response, String jsonPath) {
        return response.jsonPath().getMap(jsonPath);
    }

    // ---------------- Body Assertions ----------------
    public static void assertBodyContains(Response response, String expectedText) {
        Assert.assertTrue(response.getBody().asString().contains(expectedText),
                "Response body does not contain expected text: " + expectedText);
    }

    public static void assertJsonValue(Response response, String jsonPath, Object expectedValue) {
        Object actualValue = response.jsonPath().get(jsonPath);
        Assert.assertEquals(actualValue, expectedValue,
                "Mismatch at JSON path: " + jsonPath);
    }

    // ---------------- Retry Helper ----------------
    /**
     * Retry the request until expected status code or max attempts.
     */
    public static boolean retryUntilStatus(ResponseSupplier supplier, int expectedStatus, int maxRetries, long intervalMs) {
        int attempt = 0;
        Response response;
        while (attempt < maxRetries) {
            response = supplier.get();
            if (response.getStatusCode() == expectedStatus) {
                return true;
            }
            attempt++;
            try {
                Thread.sleep(intervalMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    @FunctionalInterface
    public interface ResponseSupplier {
        Response get();
    }
}
