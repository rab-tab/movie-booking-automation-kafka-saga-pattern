package main.java.com.microservices.api.core;

import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import java.util.Map;

import static io.restassured.RestAssured.given;

public class RestClient {
    // ---------------- GET ----------------
    public static Response get(String endpoint, RequestSpecification spec) {
        return given()
                .spec(spec)
                .when()
                .get(endpoint);
    }

    public static Response get(String endpoint, RequestSpecification spec, Map<String, ?> queryParams) {
        return given()
                .spec(spec)
                .queryParams(queryParams)
                .when()
                .get(endpoint);
    }

    // ---------------- POST ----------------
    public static Response post(String endpoint, RequestSpecification spec) {
        return given()
                .spec(spec)
                .when()
                .post(endpoint);
    }

    public static Response post(String endpoint, RequestSpecification spec, Object body) {
        return given()
                .spec(spec)
                .body(body)
                .when()
                .post(endpoint);
    }

    // ---------------- PUT ----------------
    public static Response put(String endpoint, RequestSpecification spec) {
        return given()
                .spec(spec)
                .when()
                .put(endpoint);
    }

    public static Response put(String endpoint, RequestSpecification spec, Object body) {
        return given()
                .spec(spec)
                .body(body)
                .when()
                .put(endpoint);
    }

    // ---------------- DELETE ----------------
    public static Response delete(String endpoint, RequestSpecification spec) {
        return given()
                .spec(spec)
                .when()
                .delete(endpoint);
    }
}
