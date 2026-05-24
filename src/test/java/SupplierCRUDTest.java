package com.qa.api.tests;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;

/**
 * Supplier CRUD Tests - chained flow with state passing between steps.
 *
 * Same idea as a Postman collection run: authenticate first, grab the token,
 * pass it into every call after that. The supplier ID from step 2 gets
 * reused by steps 3-6, same way you'd store it in a Postman env variable.
 *
 * @Test(priority) controls order. dependsOnMethods skips a test automatically
 * if its parent fails - so one broken step doesn't produce a wall of
 * cascading failures below it.
 *
 * Flow: auth -> create -> update + get -> delete -> verify 404
 */
public class SupplierCRUDTest {

    private String baseURL = "https://api.example.com";

    // acts like a Postman environment variable - set once, reused across tests
    private static String authToken;
    private static Integer supplierId;
    private static String supplierName = "Test Supplier Corp";

    @BeforeClass
    public void setup() {
        RestAssured.baseURI = baseURL;
        RestAssured.basePath = "/api/v1";
    }

    // step 1 - everything else in this suite needs a valid token
    @Test(priority = 1, description = "Step 1: Authenticate and extract token")
    public void test_01_authenticate() {
        String authBody = "{\n" +
                "  \"username\": \"testuser\",\n" +
                "  \"password\": \"testpass123\"\n" +
                "}";

        Response response = given()
                .header("Content-Type", "application/json")
                .body(authBody)
        .when()
                .post("/auth/login")
        .then()
                .statusCode(200)
                .body("token", notNullValue())
                .extract()
                .response();

        // pull the token out and store it - same as pm.environment.set("token", ...)
        authToken = response.jsonPath().getString("token");
        System.out.println("✓ Test 1 PASSED: Token extracted - " + authToken);
    }

    // step 2 - create the supplier record that all later steps will operate on
    @Test(priority = 2, dependsOnMethods = {"test_01_authenticate"},
          description = "Step 2: Create supplier - requires auth token")
    public void test_02_create_supplier() {
        String createBody = "{\n" +
                "  \"name\": \"" + supplierName + "\",\n" +
                "  \"email\": \"supplier@company.com\",\n" +
                "  \"phone\": \"+1-800-111-2222\",\n" +
                "  \"status\": \"active\"\n" +
                "}";

        Response response = given()
                .header("Authorization", "Bearer " + authToken)  // token from step 1
                .header("Content-Type", "application/json")
                .body(createBody)
        .when()
                .post("/suppliers")
        .then()
                .statusCode(201)  // 201 Created - not 200, the server made a new resource
                .body("id", notNullValue())
                .body("name", equalTo(supplierName))
                .extract()
                .response();

        // hold onto this ID - steps 3, 4, 5 and 6 all need it
        supplierId = response.jsonPath().getInt("id");
        System.out.println("✓ Test 2 PASSED: Supplier created with ID - " + supplierId);
    }

    // step 3 - PUT replaces the full resource, so we send name, email and phone
    @Test(priority = 3, dependsOnMethods = {"test_02_create_supplier"},
          description = "Step 3: Update supplier - requires supplier ID")
    public void test_03_update_supplier() {
        String updateBody = "{\n" +
                "  \"name\": \"" + supplierName + " - Updated\",\n" +
                "  \"email\": \"updated@company.com\",\n" +
                "  \"phone\": \"+1-800-333-4444\"\n" +
                "}";

        given()
                .header("Authorization", "Bearer " + authToken)
                .header("Content-Type", "application/json")
                .pathParam("id", supplierId)   // ID from step 2
                .body(updateBody)
        .when()
                .put("/suppliers/{id}")
        .then()
                .statusCode(200)
                // confirm the fields we sent actually landed
                .body("name", equalTo(supplierName + " - Updated"))
                .body("email", equalTo("updated@company.com"))
                .log().all();

        System.out.println("✓ Test 3 PASSED: Supplier updated successfully");
    }

    // step 4 - GET runs independently of step 3; both only need step 2
    @Test(priority = 4, dependsOnMethods = {"test_02_create_supplier"},
          description = "Step 4: Get supplier details - requires supplier ID")
    public void test_04_get_supplier() {
        given()
                .header("Authorization", "Bearer " + authToken)
                .pathParam("id", supplierId)
        .when()
                .get("/suppliers/{id}")
        .then()
                .statusCode(200)
                .body("id", equalTo(supplierId))
                .body("email", notNullValue())
                .log().all();

        System.out.println("✓ Test 4 PASSED: Supplier retrieved successfully");
    }

    // step 5 - waits for step 3 to finish before deleting, avoids a race on the record
    @Test(priority = 5, dependsOnMethods = {"test_02_create_supplier", "test_03_update_supplier"},
          description = "Step 5: Delete supplier - requires supplier ID")
    public void test_05_delete_supplier() {
        given()
                .header("Authorization", "Bearer " + authToken)
                .pathParam("id", supplierId)
        .when()
                .delete("/suppliers/{id}")
        .then()
                .statusCode(204)  // 204 No Content - resource is gone, nothing to return
                .log().all();

        System.out.println("✓ Test 5 PASSED: Supplier deleted successfully");
    }

    // step 6 - 404 here means the record is gone, not just hidden
    // catches soft-delete bugs where GET still returns data after DELETE
    @Test(priority = 6, dependsOnMethods = {"test_05_delete_supplier"},
          description = "Step 6: Verify deletion - supplier should not exist")
    public void test_06_verify_deletion() {
        given()
                .header("Authorization", "Bearer " + authToken)
                .pathParam("id", supplierId)
        .when()
                .get("/suppliers/{id}")
        .then()
                .statusCode(404)  // anything other than 404 means delete didn't work
                .log().all();

        System.out.println("✓ Test 6 PASSED: Verified supplier is deleted (404)");
    }

}

/*
 * EXECUTION FLOW:
 *
 * test_01_authenticate()
 *   POST /auth/login -> extract token -> store in authToken
 *
 * test_02_create_supplier()  [needs test_01]
 *   if test_01 fails -> skip
 *   POST /suppliers with authToken -> extract ID -> store in supplierId
 *
 * test_03_update_supplier()  [needs test_02]
 *   if test_02 fails -> skip
 *   PUT /suppliers/{supplierId}
 *
 * test_04_get_supplier()  [needs test_02]
 *   if test_02 fails -> skip
 *   GET /suppliers/{supplierId}
 *
 * test_05_delete_supplier()  [needs test_02 + test_03]
 *   if either fails -> skip
 *   DELETE /suppliers/{supplierId}
 *
 * test_06_verify_deletion()  [needs test_05]
 *   if test_05 fails -> skip
 *   GET /suppliers/{supplierId} -> expect 404
 */
