package com.boycottpro.users;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

import com.boycottpro.models.Users;
import com.boycottpro.utilities.JwtUtility;
import com.boycottpro.utilities.Logger;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;

import java.util.*;
import java.util.stream.Collectors;

public class GetUserProfileHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final String TABLE_NAME = "users";
    private final DynamoDbClient dynamoDb;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GetUserProfileHandler() {
        this.dynamoDb = DynamoDbClient.create();
    }

    public GetUserProfileHandler(DynamoDbClient dynamoDb) {
        this.dynamoDb = dynamoDb;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        String sub = null;
        int lineNum = 39;
        try {
            sub = JwtUtility.getSubFromRestEvent(event);
            if (sub == null) {
            Logger.error(42, sub, "user is Unauthorized");
            return response(401, Map.of("message", "Unauthorized"));
        }
        lineNum = 45;
            Map<String, AttributeValue> key = Map.of("user_id", AttributeValue.builder().s(sub).build());
            GetItemRequest request = GetItemRequest.builder()
                    .tableName(TABLE_NAME)
                    .key(key)
                    .build();
            var result = dynamoDb.getItem(request);
            if (!result.hasItem()) {
                return response(404, Map.of("error", "User not found"));
            }
            Users user = mapToUser(result.item());
            return response(200,user);
        } catch (Exception e) {
            Logger.error(lineNum, sub, e.getMessage());
            return response(500,Map.of("error", "Unexpected server error: " + e.getMessage()));
        }
    }

    private APIGatewayProxyResponseEvent response(int status, Object body) {
        String responseBody = null;
        try {
            responseBody = objectMapper.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(status)
                .withHeaders(Map.of("Content-Type", "application/json"))
                .withBody(responseBody);
    }

    private Users mapToUser(Map<String, AttributeValue> item) {
        Users user = new Users();
        user.setUser_id(null);
        user.setEmail_addr(item.get("email_addr").s());
        user.setUsername(item.get("username").s());
        user.setCreated_ts(Long.parseLong(item.get("created_ts").n()));
        user.setPassword_hash("***");
        user.setPaying_user(item.get("paying_user").bool());
        return user;
    }
}