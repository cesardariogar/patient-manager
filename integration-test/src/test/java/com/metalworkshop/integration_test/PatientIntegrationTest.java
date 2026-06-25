package com.metalworkshop.integration_test;

import io.restassured.RestAssured;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class PatientIntegrationTest {

    static String token;
    static String createdPatientId;

    @BeforeAll
    static void setUp() {
        RestAssured.baseURI = "http://localhost:4004";

        String loginPayload = """
                {
                    "email": "testuser@test.com",
                    "password": "password123"
                }
                """;
        token = given()
                .contentType("application/json")
                .body(loginPayload)
                .when()
                .post("/auth/login")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .get("token");

    }

    @Order(1)
    @Test
    public void shouldReturnPatientsWithValidToken() {
        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/api/patients")
                .then()
                .statusCode(200)
                .body("content", notNullValue());
    }

    @Order(2)
    @Test
    public void shouldReturnPatientsPageable() {
        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/api/patients?page=1&size=2")
                .then()
                .statusCode(200)
                .body(notNullValue())
                .body("content", hasSize(2))
                .body("pageable.pageNumber", equalTo(1))
                .body("pageable.pageSize", equalTo(2));
    }

    @Order(3)
    @Test
    public void shouldReturnPatientsByName() {
        given()
                .params("name", "John")
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/api/patients")
                .then()
                .statusCode(200)
                .body(notNullValue())
                .body("content", hasSize(1))
                .body("content.name[0]", equalTo("John"))
                .body("content.lastName[0]", equalTo("Doe"));
    }

    @Order(4)
    @Test
    public void shouldReturnPatientsById() {
        given()
                .pathParam("id", "b1a7e8c2-1234-4f56-9abc-1a2b3c4d5e6f")
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/api/patients/{id}")
                .then()
                .statusCode(200)
                .body(notNullValue())
                .body("name", equalTo("John"))
                .body("lastName", equalTo("Doe"));
    }

    @Order(5)
    @Test
    public void shouldCreatePatient() throws JSONException {
        JSONObject jsonObject = new JSONObject(
                """
                        {
                            "name" : "deleteme",
                            "lastName" : "deleteme",
                            "email" : "deleteme@example.com",
                            "address" : "test subject delete me",
                            "dateOfBirth" : "1977-03-09"
                        }
                        """);

        createdPatientId = given()
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token)
                .body(jsonObject.toString())
                .when()
                .post("/api/patients")
                .then()
                .statusCode(201)
                .header("Location", matchesPattern(".*/patients/[0-9a-fA-F\\-]{36}"))
                .extract()
                .jsonPath()
                .get("id");
    }

    @Order(6)
    @Test
    public void shouldUpdatePatientFailWithoutMail() throws JSONException {
        JSONObject jsonObject = new JSONObject(
                """
                        {
                            "name" : "delete.me",
                            "lastName" : "delete.me",
                            "address" : "test subject delete me",
                            "dateOfBirth" : "1977-03-09"
                        }
                        """);
        given()
                .pathParam("id", "62a28d53-c6d1-4ab0-a0ec-eb7b53613639")
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token)
                .body(jsonObject.toString())
                .when()
                .put("/api/patients/{id}")
                .then()
                .statusCode(400)
                .body("email", equalTo("Email is required"));
    }

    @Order(7)
    @Test
    public void shouldUpdatePatient() throws JSONException {
        JSONObject jsonObject = new JSONObject(
                """
                        {
                            "name" : "delete.me",
                            "lastName" : "delete.me",
                            "email" : "delete.me@example.com",
                            "address" : "test subject delete.me",
                            "dateOfBirth" : "1977-03-09"
                        }
                        """);
        given()
                .pathParam("id", createdPatientId)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token)
                .body(jsonObject.toString())
                .when()
                .put("/api/patients/{id}")
                .then()
                .statusCode(200);
    }

    @Order(8)
    @Test
    public void shouldDeletePatient() throws JSONException {
        given()
                .pathParam("id", createdPatientId)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token)
                .when()
                .delete("/api/patients/{id}")
                .then()
                .statusCode(204);
    }

    @Order(9)
    @Test
    public void shouldFailDeletingPatientIdNotFound() throws JSONException {
        String nonExistingId = "AAABBB-7de2-46d2-9107-222233333";
        given()
                .pathParam("id", nonExistingId)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token)
                .when()
                .delete("/api/patients/{id}")
                .then()
                .statusCode(400)
                .body("message", equalTo("Patient not found."));

    }
}
