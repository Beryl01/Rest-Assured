package com.qa.api.tests;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;

/**
 * Rest Assured Concepts Guide
 * 
 * This file maps Postman concepts to Rest Assured syntax.
 * 
 * Postman vs Rest Assured mapping:
 * ┌─────────────────────────────┬────────────────────────────────────┐
 * │ Postman Concept             │ Rest Assured Code                  │
 * ├─────────────────────────────┼────────────────────────────────────┤
 * │ Set base URL                │ RestAssured.baseURI = "..."        │
 * │ Headers tab                 │ .header("Key", "Value")            │
 * │ Body tab                    │ .body("{ ... }")                   │
 * │ Send button (GET)           │ .when().get("/path")               │
 * │ Tests - status check        │ .then().statusCode(200)            │
 * │ Tests - extract token       │ .extract().response()              │
 * │ Environment variable        │ Java class variable                │
 * │ Collection run (ordered)    │ TestNG @Priority + dependsOnMethods│
 * └─────────────────────────────┴────────────────────────────────────┘
 */
public class RestAssuredConceptsGuide {

    private String baseURL = "https://restful-booker.herokuapp.com";

    @BeforeClass
    public void setup() {
        // Set base URL - equivalent to Postman's "base_url" environment variable
        RestAssured.baseURI = baseURL;
    }

    @Test(description = "GET request with status validation")
    public void test_GET_with_status_assertion() {
        /*
         * Postman equivalent:
         * 1. Set method to GET
         * 2. URL: {{base_url}}/booking
         * 3. Send
         * 4. In Tests tab: pm.response.code === 200
         */
        given()
                .header("Accept", "application/json")  // Headers tab
        .when()
                .get("/booking")                        // Send button - GET method
        .then()
                .statusCode(200)                        // Tests tab - status check
                .log().all();                           // Print response for debugging
    }

    @Test(description = "GET with JSON Path extraction - extract response body value")
    public void test_GET_extract_response_value() {
        /*
         * Postman equivalent:
         * In Tests tab:
         * var bookingId = pm.response.json().bookingids[0];
         */
        Response response = given()
                .header("Accept", "application/json")
        .when()
                .get("/booking")
        .then()
                .statusCode(200)
                .extract()                              // Extract response object
                .response();
        
        // Now access the response like pm.response.json() in Postman
        int firstBookingId = response.jsonPath().getInt("bookingids[0]");
        System.out.println("First booking ID: " + firstBookingId);
    }

    @Test(description = "GET single booking with nested JSON assertions")
    public void test_GET_with_body_assertions() {
        /*
         * Postman Tests tab assertions mapped to Rest Assured:
         */
        given()
                .header("Accept", "application/json")
        .when()
                .get("/booking/1")
        .then()
                .statusCode(200)
                // Assertion 1: firstname is not null
                .body("firstname", notNullValue())
                
                // Assertion 2: totalprice is an Integer
                .body("totalprice", instanceOf(Integer.class))
                
                // Assertion 3: checkin follows YYYY-MM-DD format (nested JSON)
                .body("bookingdates.checkin", matchesPattern("\\d{4}-\\d{2}-\\d{2}"))
                
                // Assertion 4: firstname equals a specific value
                .body("firstname", equalTo("Sally"))
                
                .log().all();
    }

    @Test(description = "POST request with body - create a booking")
    public void test_POST_with_body() {
        /*
         * Postman equivalent:
         * 1. Set method to POST
         * 2. Headers: Content-Type: application/json
         * 3. Body tab (raw, JSON):
         *    {
         *      "firstname": "Jim",
         *      "lastname": "Brown",
         *      ...
         *    }
         * 4. Send
         * 5. Assert response code === 200
         */
        String requestBody = "{\n" +
                "  \"firstname\": \"Jim\",\n" +
                "  \"lastname\": \"Brown\",\n" +
                "  \"totalprice\": 111,\n" +
                "  \"depositpaid\": true,\n" +
                "  \"bookingdates\": {\n" +
                "    \"checkin\": \"2024-01-01\",\n" +
                "    \"checkout\": \"2024-01-05\"\n" +
                "  },\n" +
                "  \"additionalneeds\": \"Breakfast\"\n" +
                "}";

        given()
                .header("Content-Type", "application/json")
                .body(requestBody)                      // Body tab
        .when()
                .post("/booking")                       // Send POST
        .then()
                .statusCode(200)
                .body("booking.firstname", equalTo("Jim"))
                .log().all();
    }

    @Test(description = "POST with Cookies and Headers - authentication example")
    public void test_POST_with_authentication() {
        /*
         * Simulating Postman:
         * 1. Headers: send auth token
         * 2. Cookies: send session cookie
         * 3. Body: JSON payload
         */
        String authToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9";
        String sessionCookie = "session_id=12345";

        given()
                .header("Authorization", "Bearer " + authToken)
                .cookie("session", sessionCookie)
                .header("Content-Type", "application/json")
                .body("{ \"name\": \"Test\" }")
        .when()
                .post("/secure-endpoint")
        .then()
                .statusCode(200)
                .log().all();
    }

    @Test(description = "DELETE request - remove a booking")
    public void test_DELETE_request() {
        /*
         * Postman equivalent:
         * 1. Set method to DELETE
         * 2. URL: {{base_url}}/booking/1
         * 3. Headers: Cookie: token={{token}}
         * 4. Send
         * 5. Assert statusCode === 201
         */
        String authToken = "dummy_token_here";
        
        given()
                .cookie("token", authToken)
        .when()
                .delete("/booking/1")
        .then()
                .statusCode(201)  // Restful booker returns 201 for successful delete
                .log().all();
    }

    @Test(description = "Request with path parameters and query parameters")
    public void test_path_and_query_parameters() {
        /*
         * Postman equivalent:
         * URL: {{base_url}}/booking/1?sortby=firstname&limit=10
         */
        int bookingId = 1;
        
        given()
                .pathParam("id", bookingId)             // Path parameter: /booking/{id}
                .queryParam("sortby", "firstname")      // Query parameter: ?sortby=...
                .queryParam("limit", 10)
                .header("Accept", "application/json")
        .when()
                .get("/booking/{id}")                   // Use pathParam placeholder
        .then()
                .statusCode(200)
                .log().all();
    }

    @Test(description = "Response time assertion")
    public void test_response_time() {
        /*
         * Postman Tests tab:
         * pm.expect(pm.response.responseTime).to.be.below(1000);
         */
        given()
                .header("Accept", "application/json")
        .when()
                .get("/booking")
        .then()
                .time(lessThan(2000L))  // Assert response time < 2 seconds (milliseconds)
                .statusCode(200)
                .log().all();
    }

    @Test(description = "Extract multiple values from response")
    public void test_extract_multiple_values() {
        /*
         * Postman Tests tab:
         * var bookingId = pm.response.json().bookingid;
         * var firstName = pm.response.json().booking.firstname;
         * pm.environment.set("token", bookingId);
         */
        Response response = given()
                .header("Accept", "application/json")
        .when()
                .get("/booking/1")
        .then()
                .statusCode(200)
                .extract()
                .response();

        // Extract like environment variables in Postman
        String firstName = response.jsonPath().getString("firstname");
        String lastName = response.jsonPath().getString("lastname");
        Integer totalPrice = response.jsonPath().getInt("totalprice");

        System.out.println("Extracted: " + firstName + " " + lastName + ", Price: " + totalPrice);
    }

}
