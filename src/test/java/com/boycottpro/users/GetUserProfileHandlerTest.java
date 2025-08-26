package com.boycottpro.users;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.boycottpro.models.Users;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class GetUserProfileHandlerTest {

    private static final String TABLE_NAME = "";

    @Mock
    private DynamoDbClient dynamoDbMock;

    @InjectMocks
    private GetUserProfileHandler handler;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testSuccessfulLookup() throws Exception {
        // Arrange
        String userId = "user123";
        long createdTs = 1718041200L;
        Map<String, AttributeValue> item = Map.of(
                "user_id", AttributeValue.builder().s(userId).build(),
                "email_addr", AttributeValue.builder().s("test@example.com").build(),
                "username", AttributeValue.builder().s("testuser").build(),
                "created_ts", AttributeValue.builder().n(Long.toString(createdTs)).build(),
                "paying_user", AttributeValue.builder().bool(true).build()
        );
        GetItemResponse mockResponse = GetItemResponse.builder().item(item).build();
        when(dynamoDbMock.getItem(any(GetItemRequest.class))).thenReturn(mockResponse);

        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        Map<String, String> claims = Map.of("sub", "11111111-2222-3333-4444-555555555555");
        Map<String, Object> authorizer = new HashMap<>();
        authorizer.put("claims", claims);

        APIGatewayProxyRequestEvent.ProxyRequestContext rc = new APIGatewayProxyRequestEvent.ProxyRequestContext();
        rc.setAuthorizer(authorizer);
        event.setRequestContext(rc);

        // Path param "s" since client calls /users/s
        event.setPathParameters(Map.of("user_id", "s"));

        // Act
        var response = handler.handleRequest(event, mock(Context.class));

        // Assert
        assertEquals(200, response.getStatusCode());
        Users returnedUser = objectMapper.readValue(response.getBody(), Users.class);
        assertEquals(null, returnedUser.getUser_id());
        assertEquals("testuser", returnedUser.getUsername());
        assertEquals("test@example.com", returnedUser.getEmail_addr());
    }

    @Test
    void testUserNotFound() {
        when(dynamoDbMock.getItem(any(GetItemRequest.class)))
                .thenReturn(GetItemResponse.builder().build());

        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        Map<String, String> claims = Map.of("sub", "11111111-2222-3333-4444-555555555555");
        Map<String, Object> authorizer = new HashMap<>();
        authorizer.put("claims", claims);

        APIGatewayProxyRequestEvent.ProxyRequestContext rc = new APIGatewayProxyRequestEvent.ProxyRequestContext();
        rc.setAuthorizer(authorizer);
        event.setRequestContext(rc);

        // Path param "s" since client calls /users/s
        event.setPathParameters(Map.of("user_id", "s"));

        var response = handler.handleRequest(event, mock(Context.class));

        assertEquals(404, response.getStatusCode());
        assertTrue(response.getBody().contains("User not found"));
    }

    @Test
    void testMissingUserIdPathParam() {
        APIGatewayProxyRequestEvent event = null;

        var response = handler.handleRequest(event, mock(Context.class));

        assertEquals(401, response.getStatusCode());
        assertTrue(response.getBody().contains("Unauthorized"));
    }

    @Test
    void testUnhandledException() {
        when(dynamoDbMock.getItem(any(GetItemRequest.class)))
                .thenThrow(new RuntimeException("Boom"));

        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        Map<String, String> claims = Map.of("sub", "11111111-2222-3333-4444-555555555555");
        Map<String, Object> authorizer = new HashMap<>();
        authorizer.put("claims", claims);

        APIGatewayProxyRequestEvent.ProxyRequestContext rc = new APIGatewayProxyRequestEvent.ProxyRequestContext();
        rc.setAuthorizer(authorizer);
        event.setRequestContext(rc);

        // Path param "s" since client calls /users/s
        event.setPathParameters(Map.of("user_id", "s"));

        var response = handler.handleRequest(event, mock(Context.class));

        assertEquals(500, response.getStatusCode());
        assertTrue(response.getBody().contains("Unexpected server error"));
    }

}