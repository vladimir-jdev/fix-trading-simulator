package com.helesto.rest;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

@QuarkusTest
public class SessionRestTest {

    @Test
    public void testSessionGet() {
        given()
                .when()
                .get("/session")
                .then()
                .statusCode(200);
    }

    @Test
    public void testStartInitiator() throws Exception {
        given()
                .when()
                .post("/session/start-initiator")
                .then()
                .statusCode(200);
    }

    @Test
    public void testStopInitiator() throws Exception {
        given()
                .when()
                .post("/session/stop-initiator")
                .then()
                .statusCode(200);
    }

    @Test
    public void testLogout() throws Exception {
        given()
                .when()
                .post("/session/logout")
                .then()
                .statusCode(200);
    }

    @Test
    public void testMessageGet() throws Exception {
        given()
                .when()
                .get("/session/messages")
                .then()
                .statusCode(200)
                .body("$.size()", is(0));
    }
}