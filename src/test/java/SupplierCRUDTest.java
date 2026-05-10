package com.qa.api.tests;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;

/**
 * Supplier CRUD Tests with State Chaining
 * 
 * Demonstrates the Postman collection run concept:
 * Test 1: Authenticate (POST /auth) → extract token → store in class variable
 * Test 2: Create supplier (POST /suppliers) → extract ID → store in class variable
 * Test 3: Update supplier (PUT /suppliers/{id}) → uses ID from Test 2
 * Test 4: Get supplier (GET /suppliers/{id}) → uses ID from Test 2
 * Test 5: Delete supplier (DELETE /suppliers/{id}) → uses ID from Test 2
 * 
 * Key concepts:
 * - @Test(priority = N) controls execution order
 * - dependsOnMethods = "previous_test" skips this test if dependency fails
 * - Class variables store state (like Postman environment variables)
 */
public class SupplierCRUDTest {

    private String baseURL = "https://api.example.com";
    
    // Class variables to store state between tests (like Postman environment variables)
    private static String authToken;
    private static Integer supplierId;
    private static String supplierName = "Test Supplier Corp";

    @BeforeClass
    public void setup() {
        RestAssured.baseURI = baseURL;
        RestAssured.basePath = "/api/v1";
    }

    /**
     * Test 1: Authenticate and extract token
     * This is the foundation - all other tests depend on this.
     */
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

        // Extract token and store in class variable (like pm.environment.set("token", ...))
        authToken = response.jsonPath().getString("token");
        System.out.println("✓ Test 1 PASSED: Token extracted - " + authToken);
    }

    /**
     * Test 2: Create a supplier using the authenticated token
     * Depends on Test 1 (auth) to have a valid token
     */
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
                .header("Authorization", "Bearer " + authToken)  // Use token from Test 1
                .header("Content-Type", "application/json")
                .body(createBody)
        .when()
                .post("/suppliers")
        .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("name", equalTo(supplierName))
                .extract()
                .response();

        // Extract supplier ID and store in class variable
        supplierId = response.jsonPath().getInt("id");
        System.out.println("✓ Test 2 PASSED: Supplier created with ID - " + supplierId);
    }

    /**
     * Test 3: Update the supplier
     * Depends on Test 2 (create) to have a valid supplier ID
     */
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
                .pathParam("id", supplierId)                  // Use ID from Test 2
                .body(updateBody)
        .when()
                .put("/suppliers/{id}")
        .then()
                .statusCode(200)
                .body("name", equalTo(supplierName + " - Updated"))
                .body("email", equalTo("updated@company.com"))
                .log().all();

        System.out.println("✓ Test 3 PASSED: Supplier updated successfully");
    }

    /**
     * Test 4: Retrieve the supplier
     * Depends on Test 2 (create) to have a valid supplier ID
     */
    @Test(priority = 4, dependsOnMethods = {"test_02_create_supplier"}, 
          description = "Step 4: Get supplier details - requires supplier ID")
    public void test_04_get_supplier() {
        given()
                .header("Authorization", "Bearer " + authToken)
                .pathParam("id", supplierId)                  // Use ID from Test 2
        .when()
                .get("/suppliers/{id}")
        .then()
                .statusCode(200)
                .body("id", equalTo(supplierId))
                .body("email", notNullValue())
                .log().all();

        System.out.println("✓ Test 4 PASSED: Supplier retrieved successfully");
    }

    /**
     * Test 5: Delete the supplier
     * Depends on Test 2 (create) to have a valid supplier ID
     * Can also depend on Test 3 to ensure update completed first
     */
    @Test(priority = 5, dependsOnMethods = {"test_02_create_supplier", "test_03_update_supplier"}, 
          description = "Step 5: Delete supplier - requires supplier ID")
    public void test_05_delete_supplier() {
        given()
                .header("Authorization", "Bearer " + authToken)
                .pathParam("id", supplierId)                  // Use ID from Test 2
        .when()
                .delete("/suppliers/{id}")
        .then()
                .statusCode(204)  // No Content on successful delete
                .log().all();

        System.out.println("✓ Test 5 PASSED: Supplier deleted successfully");
    }

    /**
     * Test 6: Verify supplier is deleted (404)
     * Depends on Test 5 (delete) to ensure deletion completed
     */
    @Test(priority = 6, dependsOnMethods = {"test_05_delete_supplier"}, 
          description = "Step 6: Verify deletion - supplier should not exist")
    public void test_06_verify_deletion() {
        given()
                .header("Authorization", "Bearer " + authToken)
                .pathParam("id", supplierId)                  // Use ID from Test 2
        .when()
                .get("/suppliers/{id}")
        .then()
                .statusCode(404)  // Not Found
                .log().all();

        System.out.println("✓ Test 6 PASSED: Verified supplier is deleted (404)");
    }

}

/*
 * EXECUTION FLOW:
 * 
 * Test 1: test_01_authenticate()
 *   ├─ POST /auth/login
 *   ├─ Extract token
 *   └─ Store in authToken (class variable)
 *
 * Test 2: test_02_create_supplier() [depends on Test 1]
 *   ├─ IF Test 1 fails → SKIP Test 2
 *   ├─ POST /suppliers (with authToken)
 *   ├─ Extract ID
 *   └─ Store in supplierId (class variable)
 *
 * Test 3: test_03_update_supplier() [depends on Test 2]
 *   ├─ IF Test 2 fails → SKIP Test 3
 *   └─ PUT /suppliers/{supplierId}
 *
 * Test 4: test_04_get_supplier() [depends on Test 2]
 *   ├─ IF Test 2 fails → SKIP Test 4
 *   └─ GET /suppliers/{supplierId}
 *
 * Test 5: test_05_delete_supplier() [depends on Test 2 & 3]
 *   ├─ IF Test 2 OR 3 fail → SKIP Test 5
 *   └─ DELETE /suppliers/{supplierId}
 *
 * Test 6: test_06_verify_deletion() [depends on Test 5]
 *   ├─ IF Test 5 fails → SKIP Test 6
 *   └─ GET /suppliers/{supplierId} (should be 404)
 * 
 * KEY ADVANTAGE:
 * If any test fails, dependent tests are automatically skipped.
 * Example: If test_02_create_supplier() fails, tests 3-6 are skipped automatically.
 * This prevents cascading false failures and makes debugging easier.
 */
