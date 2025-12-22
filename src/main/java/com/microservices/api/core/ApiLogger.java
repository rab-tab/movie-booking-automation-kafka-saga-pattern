package main.java.com.microservices.api.core;

import io.restassured.response.Response;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class ApiLogger {
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ---------------- Request Logging ----------------
    public static void logRequest(String method, String endpoint) {
        System.out.println("--------- REQUEST ---------");
        System.out.println("Timestamp: " + LocalDateTime.now().format(formatter));
        System.out.println("Method: " + method);
        System.out.println("Endpoint: " + endpoint);
    }


    // ---------------- Request Logging ----------------
    public static void logRequest(String method, String endpoint, Map<String, ?> headers, Object body) {
        System.out.println("--------- REQUEST ---------");
        System.out.println("Timestamp: " + LocalDateTime.now().format(formatter));
        System.out.println("Method: " + method);
        System.out.println("Endpoint: " + endpoint);
        if (headers != null && !headers.isEmpty()) {
            System.out.println("Headers: " + headers);
        }
        if (body != null) {
            System.out.println("Body: " + body);
        }
        System.out.println("---------------------------");
    }

    // ---------------- Response Logging ----------------
    public static void logResponse(Response response) {
        System.out.println("--------- RESPONSE ---------");
        System.out.println("Timestamp: " + LocalDateTime.now().format(formatter));
        System.out.println("Status Code: " + response.getStatusCode());
        System.out.println("Headers: " + response.getHeaders());
        System.out.println("Body: " + response.getBody().asPrettyString());
        System.out.println("----------------------------");
    }

    // ---------------- Conditional Logging ----------------
    public static void logOnFailure(Response response) {
        if (response.getStatusCode() >= 400) {
            System.out.println("----- API FAILURE LOG -----");
            logResponse(response);
        }
    }
}
