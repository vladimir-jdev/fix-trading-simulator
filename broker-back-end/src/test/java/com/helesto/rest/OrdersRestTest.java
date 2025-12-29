package com.helesto.rest;

import com.helesto.dto.OrderDto;
import com.helesto.service.NewOrderSingleService;
import com.helesto.service.OrderCancelRequestService;
import com.helesto.service.OrderService;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import quickfix.field.OrdStatus;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@QuarkusTest
public class OrdersRestTest {

    public static final String AAPL = "AAPL";
    public static final int CLIENT_ORDER_ID = 123;

    private static final String FIELD_CL_ORD_ID = "clOrdID";
    private static final String FIELD_SYMBOL = "symbol";
    private static final String FIELD_ORD_STATUS = "ordStatus";

    @InjectMock
    NewOrderSingleService newOrderSingleService;

    @InjectMock
    OrderService orderService;

    @InjectMock
    OrderCancelRequestService orderCancelRequestService;

    @Test
    public void testCreateOrder() throws Exception {
        OrderDto request = new OrderDto();
        request.setSymbol(AAPL);
        request.setSide('1');
        request.setOrderQty(100.0);
        request.setPrice(270.0);

        OrderDto response = new OrderDto();
        response.setClOrdID(CLIENT_ORDER_ID);
        response.setSymbol(AAPL);
        response.setOrdStatus(OrdStatus.PENDING_NEW);

        when(newOrderSingleService.newOrderSingle(any(OrderDto.class)))
                .thenReturn(response);

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/orders")
                .then()
                .statusCode(200)
                .body(FIELD_CL_ORD_ID, is(CLIENT_ORDER_ID))
                .body(FIELD_SYMBOL, equalTo(AAPL))
                .body(FIELD_ORD_STATUS, equalTo(String.valueOf(OrdStatus.PENDING_NEW)));
    }

    @Test
    public void testListOrders() throws Exception {
        OrderDto order1 = new OrderDto();
        order1.setClOrdID(CLIENT_ORDER_ID);
        order1.setSymbol(AAPL);

        OrderDto[] orders = new OrderDto[]{order1};

        when(orderService.listOrders()).thenReturn(orders);

        given()
                .when()
                .get("/orders")
                .then()
                .statusCode(200)
                .body("$.size()", is(1))
                .body("[0].clOrdID", is(CLIENT_ORDER_ID))
                .body("[0].symbol", equalTo(AAPL));
    }

    @Test
    public void testGetOrderById() throws Exception {
        OrderDto order = new OrderDto();
        order.setClOrdID(CLIENT_ORDER_ID);
        order.setSymbol(AAPL);

        // Retains original typo 'gerOrder' from OrdersRest service definition
        when(orderService.gerOrder(CLIENT_ORDER_ID)).thenReturn(order);

        given()
                .pathParam("clOrdID", CLIENT_ORDER_ID)
                .when()
                .get("/orders/{clOrdID}")
                .then()
                .statusCode(200)
                .body(FIELD_CL_ORD_ID, is(CLIENT_ORDER_ID))
                .body(FIELD_SYMBOL, equalTo(AAPL));
    }

    @Test
    public void testCancelOrder() throws Exception {
        OrderDto response = new OrderDto();
        response.setClOrdID(CLIENT_ORDER_ID);
        response.setOrdStatus(OrdStatus.PENDING_CANCEL);

        when(orderCancelRequestService.orderCancelRequest(CLIENT_ORDER_ID)).thenReturn(response);

        given()
                .pathParam("clOrdID", CLIENT_ORDER_ID)
                .when()
                .delete("/orders/{clOrdID}")
                .then()
                .statusCode(200)
                .body(FIELD_CL_ORD_ID, is(CLIENT_ORDER_ID));
    }
}