package com.qa.api.tests;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;

/**
 * Postman -> Rest Assured mapping guide.
 *
 * Each test shows the Postman equivalent in a block comment above it.
 * Useful for anyone who knows Postman and is learning the Rest Assured syntax.
 *
 * Postman concept          Rest Assured code
 * -------------------------------------------
 * base url env variable    RestAssured.baseURI = "..."
 * Headers tab              .header("Key", "Value")
 * Body tab (raw JSON)      .body("{ ... }")
 * Send button (GET)        .when().get("/path")
 * Tests tab - status check .then().statusCode(200)
 * Tests tab - extract val  .extract().response()
 * Environment variable     Java class variable (static)
 * Collection run (ordered) TestNG @Priority + dependsOnMethods
 */
public class RestAssuredConceptsGuide {

    private String baseURL = "https://restful-booker.herokuapp.com";

    @BeforeClass
    public void setup() {
        // same as setting base_url in the Postman environment
        RestAssured.baseURI = baseURL;
    }

    @Test(description = "GET request with status validation")
    public void test_GET_with_status_assertion() {
        /*
         * Postman:
         * 1. GET {{base_url}}/booking
         * 2. Send
         * 3. Tests tab: pm.response.code === 200
         */
        given()
                .header("Accept", "application/json")  // Headers tab
        .when()
                .get("/booking")                        // Send - GET
        .then()
                .statusCode(200)                        // Tests tab - status check
                .log().all();                           // print response for debugging
    }

    @Test(description = "GET with JSON Path extraction - extract value from response body")
    public void test_GET_extract_response_value() {
        /*
         * Postman Tests tab:
         * var bookingId = pm.response.json().bookingids[0];
         */
        Response response = given()
                .header("Accept", "application/json")
        .when()
                .get("/booking")
        .then()
                .statusCode(200)
                .extract()   // grab the full response object
                .response();

        // jsonPath().getInt() is the equivalent of pm.response.json().field
        int firstBookingId = response.jsonPath().getInt("bookingids[0]");
        System.out.println("First booking ID: " + firstBookingId);
    }

    @Test(description = "GET single booking with nested JSON assertions")
    public void test_GET_with_body_assertions() {
        /*
         * Postman Tests tab assertions mapped to Rest Assured:
         * pm.expect(pm.response.json().firstname).to.not.be.null  ->  .body("firstname", notNullValue())
         * pm.expect(typeof pm.response.json().totalprice).to.equal("number")  ->  instanceOf(Integer.class)
         */
        given()
                .header("Accept", "application/json")
        .when()
                .get("/booking/1")
        .then()
                .statusCode(200)
                .body("firstname", notNullValue())
                .body("totalprice", instanceOf(Integer.class))
                // dot notation for nested JSON: bookingdates.checkin
                .body("bookingdates.checkin", matchesPattern("\\d{4}-\\d{2}-\\d{2}"))
                .body("firstname", equalTo("Sally"))
                .log().all();
    }

    @Test(description = "POST request with body - create a booking")
    public void test_POST_with_body() {
        /*
         * Postman:
         * 1. POST {{base_url}}/booking
         * 2. Headers: Content-Type: application/json
         * 3. Body tab (raw, JSON): { "firstname": "Jim", ... }
         * 4. Tests tab: pm.response.code === 200
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
                .body(requestBody)         // Body tab
        .when()
                .post("/booking")          // Send - POST
        .then()
                .statusCode(200)
                // response wraps the booking under a "booking" key on create
                .body("booking.firstname", equalTo("Jim"))
                .log().all();
    }

    @Test(description = "POST with Cookies and Headers - authentication example")
    public void test_POST_with_authentication() {
        /*
         * Postman: Headers tab for auth token, Cookies tab for session cookie
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
         * Postman:
         * 1. DELETE {{base_url}}/booking/1
         * 2. Headers: Cookie: token={{token}}
         * 3. Tests tab: pm.response.code === 201
         *
         * Restful booker returns 201 on delete, not 204 - unusual but intentional
         */
        String authToken = "dummy_token_here";

        given()
                .cookie("token", authToken)
        .when()
                .delete("/booking/1")
        .then()
                .statusCode(201)
                .log().all();
    }

    @Test(description = "Request with path parameters and query parameters")
    public void test_path_and_query_parameters() {
        /*
         * Postman URL: {{base_url}}/booking/1?sortby=firstname&limit=10
         *
         * Rest Assured splits these:
         * - .pathParam() for /booking/{id}
         * - .queryParam() for ?sortby=...
         */
        int bookingId = 1;

        given()
                .pathParam("id", bookingId)           // replaces {id} in the path
                .queryParam("sortby", "firstname")    // appended as ?sortby=firstname
                .queryParam("limit", 10)
                .header("Accept", "application/json")
        .when()
                .get("/booking/{id}")
        .then()
                .statusCode(200)
                .log().all();
    }

    @Test(description = "Response time assertion")
    public void test_response_time() {
        /*
         * Postman Tests tab:
         * pm.expect(pm.response.responseTime).to.be.below(1000);
         *
         * Rest Assured uses .time() with Hamcrest matchers - value is milliseconds
         */
        given()
                .header("Accept", "application/json")
        .when()
                .get("/booking")
        .then()
                .time(lessThan(2000L))  // 2000ms - adjust based on expected latency
                .statusCode(200)
                .log().all();
    }

    @Test(description = "Extract multiple values from response")
    public void test_extract_multiple_values() {
        /*
         * Postman Tests tab:
         * var firstName = pm.response.json().firstname;
         * pm.environment.set("firstName", firstName);
         *
         * In Java you just assign to local variables - no environment.set needed
         */
        Response response = given()
                .header("Accept", "application/json")
        .when()
                .get("/booking/1")
        .then()
                .statusCode(200)
                .extract()
                .response();

        String firstName  = response.jsonPath().getString("firstname");
        String lastName   = response.jsonPath().getString("lastname");
        Integer totalPrice = response.jsonPath().getInt("totalprice");

        System.out.println("Extracted: " + firstName + " " + lastName + ", Price: " + totalPrice);
    }

}
