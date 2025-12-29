package com.helesto.rest;

import com.helesto.core.Trader;
import com.helesto.infrastructure.TestFixAcceptor;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Testing the FIX session lifecycle:
 * 1. @Order(1) testStartInitiator: First, it is necessary to start the initiator and establish a connection. This
 * transitions the system to the "Logged On" state.
 *
 * 2. @Order(2) testSessionGet: This test expects the system to already be in the "Logged On" state. If it runs
 * before the first step, it will fail because the session will be inactive.
 *
 * 3. @Order(3) testMessageGet: Checks that message records have appeared in the database. These records appear only
 * as a result of a successful logon (step 1).
 *
 * 4. @Order(4) testLogout: Performs the logout procedure. This action makes sense only when the session is active.
 *
 * 5. @Order(5) testStopInitiator: Final cleanup and stopping of the service.
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SessionRestTestIT {

    private static final int FIX_PORT = 9880;
    private static TestFixAcceptor mockExchange;

    @Inject
    Trader trader;

    @BeforeAll
    public static void startExchange() throws Exception {
        // Initializes mock exchange to accept broker connections
        mockExchange = new TestFixAcceptor(FIX_PORT);
        mockExchange.start();
    }

    @AfterAll
    public static void stopExchange() {
        if (mockExchange != null) {
            mockExchange.stop();
        }
    }

    @Test
    @Order(1)
    public void testStartInitiator() {
        // Ensures clean state before execution
        if (trader.isInitiatorStarted()) {
            given().post("/session/stop-initiator").then().statusCode(200);
        }

        given()
                .when()
                .post("/session/start-initiator")
                .then()
                .statusCode(200)
                .body("initiatorStarted", is(true));

        // Awaits asynchronous logon completion to ensure session readiness for subsequent tests
        await().atMost(10, TimeUnit.SECONDS).until(() -> trader.getSession() != null && trader.getSession().isLoggedOn());
    }

    @Test
    @Order(2)
    public void testSessionGet() {
        given()
                .when()
                .get("/session")
                .then()
                .statusCode(200)
                .body("initiatorStarted", is(true))
                .body("loggedOn", is(true))
                .body("sessionID", notNullValue());
    }

    @Test
    @Order(3)
    public void testMessageGet() {
        // Verifies that the successful logon sequence generated persisted message records
        given()
                .when()
                .get("/session/messages")
                .then()
                .statusCode(200)
                .body("$.size()", greaterThan(0));
    }

    @Test
    @Order(4)
    public void testLogout() {
        given()
                .when()
                .post("/session/logout")
                .then()
                .statusCode(200);

        // Awaits asynchronous session termination
        await().atMost(5, TimeUnit.SECONDS).until(() -> !trader.getSession().isLoggedOn());
    }

    @Test
    @Order(5)
    public void testStopInitiator() {
        given()
                .when()
                .post("/session/stop-initiator")
                .then()
                .statusCode(200)
                .body("initiatorStarted", is(false));
    }
}