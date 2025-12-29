package com.helesto.rest;

import com.helesto.core.Trader;
import com.helesto.infrastructure.TestFixAcceptor;
import com.helesto.model.OrderEntity;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import quickfix.FieldNotFound;
import quickfix.Message;
import quickfix.SessionNotFound;
import quickfix.field.*;
import quickfix.fix44.ExecutionReport;

import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTest
public class OrdersRestTestIT {

    private static final int FIX_PORT = 9880;
    private static TestFixAcceptor mockExchange;

    private static final String AAPL = "AAPL";
    private static final double PRICE = 270.0;
    private static final double ORDER_QUANTITY = 100.0;
    private static final int HTTP_STATUS_OK = 200;

    private static final String ORDER_JSON_TEMPLATE = """
            {
                "symbol": "%s",
                "side": "%s",
                "orderQty": %s,
                "price": %s,
                "ordType": "%s"
            }""";
    private static final String ORDER_JSON = String.format(ORDER_JSON_TEMPLATE, AAPL, Side.BUY, ORDER_QUANTITY, PRICE,
            OrdType.LIMIT);

    private static final String FIELD_CL_ORD_ID = "clOrdID";
    private static final String FIELD_SYMBOL = "symbol";
    private static final String FIELD_PRICE = "price";
    private static final String FIELD_ORDER_QTY = "orderQty";
    private static final String FIELD_ORD_STATUS = "ordStatus";

    @Inject
    Trader trader;

    @BeforeAll
    public static void startExchange() throws Exception {
        // Initializes mock exchange emulator
        mockExchange = new TestFixAcceptor(FIX_PORT);
        mockExchange.start();
    }

    @AfterAll
    public static void stopExchange() {
        if (mockExchange != null) {
            mockExchange.stop();
        }
    }

    @BeforeEach
    public void setup() throws Exception {
        mockExchange.clearMessages();
        // Ensures active FIX session before test execution
        if (!trader.isInitiatorStarted()) {
            trader.logon();
        }
        await().atMost(10, TimeUnit.SECONDS).until(() -> trader.getSession() != null && trader.getSession().isLoggedOn());
    }

    @Test
    public void testCreateAndGetOrder() throws Exception {
        // Triggers persistence and FIX transmission
        int clOrdId = given()
                .contentType(ContentType.JSON)
                .body(ORDER_JSON)
                .when()
                .post("/orders")
                .then()
                .statusCode(HTTP_STATUS_OK)
                .body(FIELD_SYMBOL, equalTo(AAPL))
                .body(FIELD_ORD_STATUS, equalTo(String.valueOf(OrderEntity.NEW_ORDER_NOT_CONFIRMED)))
                .extract().path(FIELD_CL_ORD_ID);

        // Verifies NewOrderSingle (35=D) transmission
        assertMessageSentToExchange(MsgType.ORDER_SINGLE);

        assertExecutionReport(clOrdId, OrdStatus.FILLED);
    }

    @Test
    public void testListOrders() {
        // Primes order list to ensure non-empty response
        given()
                .contentType(ContentType.JSON)
                .body(ORDER_JSON)
                .post("/orders")
                .then()
                .statusCode(HTTP_STATUS_OK);

        given()
                .when()
                .get("/orders")
                .then()
                .statusCode(HTTP_STATUS_OK)
                .body("$.size()", greaterThan(0))
                .body(FIELD_SYMBOL, hasItem(AAPL));
    }

    @Test
    public void testCancelOrder() throws Exception {
        int clOrdId = given()
                .contentType(ContentType.JSON)
                .body(ORDER_JSON)
                .post("/orders")
                .then()
                .statusCode(HTTP_STATUS_OK)
                .extract().path(FIELD_CL_ORD_ID);

        // Clears initial creation message
        assertMessageSentToExchange(MsgType.ORDER_SINGLE);

        given()
                .pathParam(FIELD_CL_ORD_ID, clOrdId)
                .when()
                .delete("/orders/{clOrdID}")
                .then()
                .statusCode(HTTP_STATUS_OK)
                .body(FIELD_CL_ORD_ID, equalTo(clOrdId));

        // Verifies OrderCancelRequest (35=F) transmission
        assertMessageSentToExchange(MsgType.ORDER_CANCEL_REQUEST);

        assertExecutionReport(clOrdId, OrdStatus.CANCELED);
    }

    public void assertExecutionReport(int clOrdId, char ordStatus, String symbol, double price,
                                      double order_quantity) throws SessionNotFound {
        // Simulates inbound ExecutionReport from exchange
        ExecutionReport report = new ExecutionReport(
                new OrderID("EXEC123"),
                new ExecID("EXEC-ID-001"),
                new ExecType(ExecType.FILL),
                new OrdStatus(ordStatus),
                new Side(Side.BUY),
                new LeavesQty(0),
                new CumQty(order_quantity),
                new AvgPx(price)
        );
        report.set(new ClOrdID(String.valueOf(clOrdId)));
        report.set(new Symbol(symbol));
        mockExchange.sendToBroker(report);

        // Validates state convergence via polling
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            given()
                    .pathParam(FIELD_CL_ORD_ID, clOrdId)
                    .when()
                    .get("/orders/{clOrdID}")
                    .then()
                    .statusCode(HTTP_STATUS_OK)
                    .body(FIELD_CL_ORD_ID, equalTo(clOrdId))
                    .body(FIELD_SYMBOL, equalTo(symbol))
                    .body(FIELD_PRICE, equalTo((float) price))
                    .body(FIELD_ORDER_QTY, equalTo((float) order_quantity))
                    .body(FIELD_ORD_STATUS, equalTo(String.valueOf(ordStatus)));
        });
    }

    public void assertExecutionReport(int clOrdId, char ordStatus) throws SessionNotFound {
        assertExecutionReport(clOrdId, ordStatus, AAPL, PRICE, ORDER_QUANTITY);
    }

    public void assertMessageSentToExchange(String msgType) throws InterruptedException, FieldNotFound {
        Message receivedMessage = mockExchange.getReceivedMessages().poll(10, TimeUnit.SECONDS);
        assertNotNull(receivedMessage, "Exchange failed to receive message");
        assertEquals(msgType, receivedMessage.getHeader().getString(MsgType.FIELD));
    }
}