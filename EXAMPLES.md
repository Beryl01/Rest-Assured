# Adding More Tests - Examples & Best Practices

This guide shows you how to add new API tests to the project.

## File Structure

All test files go in: `src/test/java/`

```
src/test/java/
├── RestAssuredConceptsGuide.java
├── BasicSupplierApiTest.java
├── SupplierCRUDTest.java
└── [YOUR_TEST_FILES_HERE]
```

---

## Example 1: Simple GET Test

```java
package com.qa.api.tests;

import io.restassured.RestAssured;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;

public class ProductApiTest {

    @BeforeClass
    public void setup() {
        RestAssured.baseURI = "https://api.example.com";
        RestAssured.basePath = "/api/v1";
    }

    @Test(description = "GET /products returns 200 and has data")
    public void test_get_all_products() {
        given()
                .header("Accept", "application/json")
        .when()
                .get("/products")
        .then()
                .statusCode(200)
                .body("$", hasSize(greaterThan(0)))  // Array has at least 1 item
                .body("[0].id", notNullValue())
                .body("[0].name", notNullValue())
                .log().all();
    }

    @Test(description = "GET /products/{id} with specific product")
    public void test_get_product_by_id() {
        given()
                .pathParam("id", 123)
        .when()
                .get("/products/{id}")
        .then()
                .statusCode(200)
                .body("name", notNullValue())
                .body("price", greaterThan(0));
    }
}
```

---

## Example 2: POST Test with Validation

```java
@Test(description = "POST /products creates new product")
public void test_create_product() {
    String requestBody = "{\n" +
            "  \"name\": \"New Product\",\n" +
            "  \"category\": \"Electronics\",\n" +
            "  \"price\": 99.99,\n" +
            "  \"stock\": 50\n" +
            "}";

    given()
            .header("Content-Type", "application/json")
            .body(requestBody)
    .when()
            .post("/products")
    .then()
            .statusCode(201)  // Created
            .body("id", notNullValue())
            .body("name", equalTo("New Product"))
            .body("price", equalTo(99.99f));
}
```

---

## Example 3: Extract and Use Values

```java
@Test(description = "Extract product ID for use in other tests")
public void test_extract_product_data() {
    Response response = given()
            .pathParam("id", 123)
    .when()
            .get("/products/{id}")
    .then()
            .statusCode(200)
            .extract()
            .response();

    // Extract multiple values
    String productName = response.jsonPath().getString("name");
    Double price = response.jsonPath().getDouble("price");
    Integer stock = response.jsonPath().getInt("stock");

    System.out.println("Product: " + productName + " - $" + price);
}
```

---

## Example 4: Chained Tests with Dependencies

```java
public class OrderWorkflowTest {

    private static Integer productId;
    private static Integer orderId;

    @Test(priority = 1, description = "Get product ID")
    public void test_01_get_product() {
        Response response = given()
        .when()
                .get("/products?category=Electronics")
        .then()
                .statusCode(200)
                .extract()
                .response();

        productId = response.jsonPath().getInt("[0].id");
        System.out.println("Product ID: " + productId);
    }

    @Test(priority = 2, dependsOnMethods = {"test_01_get_product"}, 
          description = "Create order with product from Test 1")
    public void test_02_create_order() {
        String orderBody = "{\n" +
                "  \"product_id\": " + productId + ",\n" +
                "  \"quantity\": 2\n" +
                "}";

        Response response = given()
                .header("Content-Type", "application/json")
                .body(orderBody)
        .when()
                .post("/orders")
        .then()
                .statusCode(201)
                .extract()
                .response();

        orderId = response.jsonPath().getInt("id");
        System.out.println("Order ID: " + orderId);
    }

    @Test(priority = 3, dependsOnMethods = {"test_02_create_order"}, 
          description = "Get order status")
    public void test_03_get_order_status() {
        given()
                .pathParam("id", orderId)
        .when()
                .get("/orders/{id}")
        .then()
                .statusCode(200)
                .body("status", anyOf(equalTo("pending"), equalTo("confirmed")));
    }
}
```

---

## Example 5: Parameterized Tests

```java
import org.testng.annotations.DataProvider;

public class ParameterizedProductTest {

    @DataProvider(name = "productIds")
    public Object[][] productIds() {
        return new Object[][]{
                {1},
                {2},
                {3},
                {999}  // Test with non-existent ID
        };
    }

    @Test(dataProvider = "productIds", description = "Test multiple product IDs")
    public void test_get_products_with_different_ids(int productId) {
        given()
                .pathParam("id", productId)
        .when()
                .get("/products/{id}")
        .then()
                // Some IDs should return 200, others 404
                .statusCode(anyOf(equalTo(200), equalTo(404)));
    }
}
```

---

## Example 6: Error Handling & Negative Tests

```java
@Test(description = "POST with invalid data returns 400")
public void test_create_product_invalid_data() {
    String invalidBody = "{\n" +
            "  \"name\": \"\",\n" +  // Empty name
            "  \"price\": -10\n" +    // Negative price
            "}";

    given()
            .header("Content-Type", "application/json")
            .body(invalidBody)
    .when()
            .post("/products")
    .then()
            .statusCode(400)  // Bad Request
            .body("error", notNullValue())
            .body("error.message", containsString("validation"));
}

@Test(description = "GET non-existent product returns 404")
public void test_get_nonexistent_product() {
    given()
            .pathParam("id", 99999)
    .when()
            .get("/products/{id}")
    .then()
            .statusCode(404)
            .body("message", containsString("not found"));
}

@Test(description = "Missing authentication returns 401")
public void test_unauthorized_request() {
    given()
            // No Authorization header
    .when()
            .delete("/products/123")
    .then()
            .statusCode(401);
}
```

---

## Example 7: Authentication & Headers

```java
public class AuthenticatedApiTest {

    private String authToken;

    @BeforeClass
    public void setup() {
        RestAssured.baseURI = "https://api.example.com";
        // Get auth token before all tests
        authenticateAndGetToken();
    }

    private void authenticateAndGetToken() {
        String loginBody = "{\"username\": \"testuser\", \"password\": \"pass123\"}";

        Response response = given()
                .header("Content-Type", "application/json")
                .body(loginBody)
        .when()
                .post("/auth/login")
        .then()
                .statusCode(200)
                .extract()
                .response();

        authToken = response.jsonPath().getString("token");
    }

    @Test(description = "POST with authentication token")
    public void test_authenticated_request() {
        given()
                .header("Authorization", "Bearer " + authToken)
                .header("Content-Type", "application/json")
                .body("{\"name\": \"New Item\"}")
        .when()
                .post("/items")
        .then()
                .statusCode(201);
    }
}
```

---

## Example 8: Request Specifications (Reusable Configuration)

```java
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.specification.RequestSpecification;

public class ReusableRequestSpecTest {

    private RequestSpecification requestSpec;

    @BeforeClass
    public void setup() {
        requestSpec = new RequestSpecBuilder()
                .setBaseUri("https://api.example.com")
                .setBasePath("/api/v1")
                .addHeader("Accept", "application/json")
                .addHeader("Content-Type", "application/json")
                .build();
    }

    @Test(description = "Use reusable request spec")
    public void test_with_spec() {
        given()
                .spec(requestSpec)
                .pathParam("id", 123)
        .when()
                .get("/products/{id}")
        .then()
                .statusCode(200);
    }
}
```

---

## Best Practices

### ✓ DO

```java
// 1. Use descriptive test names
@Test(description = "Verify GET /products returns 200 with valid JSON")
public void test_get_all_products_returns_200() { ... }

// 2. Use assertions for validation
.then()
    .statusCode(200)
    .body("name", notNullValue())
    .body("price", greaterThan(0));

// 3. Extract data for reuse
Response response = given()...when()...then().extract().response();
int id = response.jsonPath().getInt("id");

// 4. Use @DataProvider for parameterized tests
@DataProvider(name = "ids")
public Object[][] ids() { return new Object[][]{{1}, {2}, {3}}; }

// 5. Use dependsOnMethods for chained tests
@Test(dependsOnMethods = {"test_authenticate"})
public void test_create_user() { ... }

// 6. Clean up resources
@AfterClass
public void cleanup() {
    // Delete test data, logout, etc.
}
```

### ✗ DON'T

```java
// 1. Don't hardcode URLs
"https://api.example.com"  // ✗ Bad

// 2. Don't ignore test failures
.then()
    .log().all();  // ✗ Bad - no assertions!

// 3. Don't repeat code
// ✗ Bad - copy-paste code in every test
given().header(...).when().get(...).then()...

// 4. Don't use Thread.sleep()
Thread.sleep(5000);  // ✗ Bad - slows down tests

// 5. Don't test multiple things in one test
@Test
public void test_everything() {  // ✗ Bad
    // Create, update, delete all in one test
}
```

---

## Adding Your Test to testng.xml

After creating a new test class, add it to `testng.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE suite SYSTEM "http://testng.org/testng-1.0.dtd">
<suite name="API Test Suite" parallel="false">
    <test name="Product API Tests">
        <classes>
            <class name="com.qa.api.tests.ProductApiTest" />
            <class name="com.qa.api.tests.OrderWorkflowTest" />
        </classes>
    </test>
</suite>
```

---

## Running Your New Tests

```bash
# Run all tests
mvn test

# Run only your new test class
mvn test -Dtest=ProductApiTest

# Run specific test method
mvn test -Dtest=ProductApiTest#test_get_all_products
```

---

## Common Assertions

```java
// Status codes
.statusCode(200)
.statusCode(201)  // Created
.statusCode(404)  // Not Found
.statusCode(500)  // Server Error

// Body assertions
.body("name", equalTo("John"))
.body("age", greaterThan(18))
.body("age", lessThan(65))
.body("email", containsString("@"))
.body("email", matchesPattern("^[A-Za-z0-9+_.-]+@(.+)$"))
.body("$", hasSize(5))
.body("$", notNullValue())
.body("$", nullValue())

// Headers
.header("Content-Type", containsString("application/json"))
.header("Authorization", notNullValue())

// Time
.time(lessThan(2000L))  // Response in < 2 seconds
```

---

## Test Report Location

After running tests:
```
target/surefire-reports/
├── index.html          ← Open this in browser
├── TEST-*.xml
└── ...
```

---

**Happy testing!** 🚀 Add your new tests and run `mvn test` to verify they work.
