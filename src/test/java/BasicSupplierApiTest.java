package com.qa.api.tests;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;
import static org.testng.Assert.*;

/**
 * Basic Supplier API Tests
 * 
 * Demonstrates simple GET, POST, PUT, DELETE operations
 * without state chaining (simpler than CRUD test with dependencies).
 */
public class BasicSupplierApiTest {

    private String baseURL = "https://api.example.com";  // Change to your actual API

    @BeforeClass
    public void setup() {
        // Set base URL for all requests in this class
        RestAssured.baseURI = baseURL;
        RestAssured.basePath = "/api/v1";
    }

    @Test(description = "Verify GET /suppliers returns 200 OK")
    public void test_get_all_suppliers() {
        given()
                .header("Accept", "application/json")
        .when()
                .get("/suppliers")
        .then()
                .statusCode(200)
                .contentType("application/json")
                .log().all();
    }

    @Test(description = "Verify GET /suppliers/{id} returns supplier details")
    public void test_get_supplier_by_id() {
        int supplierId = 1;
        
        given()
                .pathParam("id", supplierId)
        .when()
                .get("/suppliers/{id}")
        .then()
                .statusCode(200)
                .body("id", equalTo(supplierId))
                .body("name", notNullValue())
                .body("email", matchesPattern("^[A-Za-z0-9+_.-]+@(.+)$"))  // Email regex
                .log().all();
    }

    @Test(description = "Create a new supplier with POST")
    public void test_create_supplier() {
        String requestBody = "{\n" +
                "  \"name\": \"Acme Corp\",\n" +
                "  \"email\": \"contact@acmecorp.com\",\n" +
                "  \"phone\": \"+1-800-123-4567\",\n" +
                "  \"address\": \"123 Business Ave, Commerce City\"\n" +
                "}";

        Response response = given()
                .header("Content-Type", "application/json")
                .body(requestBody)
        .when()
                .post("/suppliers")
        .then()
                .statusCode(201)  // Created
                .body("id", notNullValue())
                .body("name", equalTo("Acme Corp"))
                .extract()
                .response();

        // Extract ID for potential use in subsequent requests
        Integer supplierId = response.jsonPath().getInt("id");
        System.out.println("Created supplier with ID: " + supplierId);
    }

    @Test(description = "Update supplier with PUT")
    public void test_update_supplier() {
        int supplierId = 1;
        
        String updateBody = "{\n" +
                "  \"name\": \"Acme Corp Updated\",\n" +
                "  \"email\": \"newemail@acmecorp.com\",\n" +
                "  \"phone\": \"+1-800-987-6543\"\n" +
                "}";

        given()
                .pathParam("id", supplierId)
                .header("Content-Type", "application/json")
                .body(updateBody)
        .when()
                .put("/suppliers/{id}")
        .then()
                .statusCode(200)
                .body("name", equalTo("Acme Corp Updated"))
                .body("email", equalTo("newemail@acmecorp.com"))
                .log().all();
    }

    @Test(description = "Partial update supplier with PATCH")
    public void test_patch_supplier() {
        int supplierId = 1;
        
        String patchBody = "{\n" +
                "  \"email\": \"patched@acmecorp.com\"\n" +
                "}";

        given()
                .pathParam("id", supplierId)
                .header("Content-Type", "application/json")
                .body(patchBody)
        .when()
                .patch("/suppliers/{id}")
        .then()
                .statusCode(200)
                .body("email", equalTo("patched@acmecorp.com"))
                .log().all();
    }

    @Test(description = "Delete supplier with DELETE")
    public void test_delete_supplier() {
        int supplierId = 1;
        
        given()
                .pathParam("id", supplierId)
        .when()
                .delete("/suppliers/{id}")
        .then()
                .statusCode(204)  // No Content
                .log().all();
    }

    @Test(description = "Verify deleted supplier returns 404")
    public void test_get_deleted_supplier_returns_404() {
        int deletedSupplierId = 1;
        
        given()
                .pathParam("id", deletedSupplierId)
        .when()
                .get("/suppliers/{id}")
        .then()
                .statusCode(404)
                .log().all();
    }

    @Test(description = "Filter suppliers by query parameter")
    public void test_filter_suppliers_by_status() {
        given()
                .queryParam("status", "active")
                .queryParam("limit", 10)
                .queryParam("offset", 0)
        .when()
                .get("/suppliers")
        .then()
                .statusCode(200)
                .body("$", notNullValue())
                .log().all();
    }

    @Test(description = "Validate response headers")
    public void test_response_headers() {
        given()
        .when()
                .get("/suppliers")
        .then()
                .statusCode(200)
                .header("Content-Type", containsString("application/json"))
                .header("X-Total-Count", notNullValue())
                .log().all();
    }

    @Test(description = "Verify request/response logging")
    public void test_with_detailed_logging() {
        given()
                .log().all()  // Log request details
        .when()
                .get("/suppliers")
        .then()
                .log().all()  // Log response details
                .statusCode(200);
    }

}
