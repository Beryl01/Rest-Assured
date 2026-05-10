# Rest Assured API Testing Framework - Java 17 LTS

A complete API testing framework using **Rest Assured** and **TestNG** with Java 17 LTS. This project demonstrates how to write API tests in code and execute them both locally and on Jenkins CI/CD.

## Table of Contents
- [Quick Start](#quick-start)
- [Project Structure](#project-structure)
- [Prerequisites](#prerequisites)
- [Setup Instructions](#setup-instructions)
- [Running Tests](#running-tests)
- [File Guide](#file-guide)
- [Postman → Rest Assured Mapping](#postman--rest-assured-mapping)
- [Jenkins Setup](#jenkins-setup)

## Quick Start

### Local Setup (5 minutes)
```bash
# 1. Install Java 17
# Download from: https://adoptium.net/

# 2. Install Maven
# https://maven.apache.org/download.cgi

# 3. Clone/open this repo in VS Code

# 4. Run tests
mvn test

# 5. View reports
# Reports are in: target/surefire-reports/
```

### Run on Jenkins (No Local Setup Needed)
```bash
# 1. Push this repo to GitHub
# 2. Create a Pipeline job in Jenkins
# 3. Set repository URL and point to Jenkinsfile
# 4. Jenkins automatically downloads Java 17 & Maven, runs tests
```

## Project Structure

```
rest-assured-tests/
├── pom.xml                                    # Maven config + dependencies
├── Jenkinsfile                                # CI/CD pipeline for Jenkins
├── testng.xml                                 # Test suite configuration
├── README.md                                  # This file
├── src/
│   └── test/
│       ├── java/
│       │   ├── RestAssuredConceptsGuide.java  # Complete syntax reference
│       │   ├── BasicSupplierApiTest.java      # Simple GET/POST/PUT/DELETE
│       │   └── SupplierCRUDTest.java          # Chained tests with state
│       └── resources/                         # Test data, config files
```

## Prerequisites

### Local Development
- **Java 17 LTS** - [Download from Adoptium](https://adoptium.net/)
- **Maven 3.6+** - [Download from Maven.apache.org](https://maven.apache.org/download.cgi)
- **VS Code** with Extension Pack for Java (recommended)

### Jenkins Server
- Java 17 JDK installed
- Maven 3.6+ installed
- Pipeline plugin enabled
- GitHub webhook configured

## Setup Instructions

### 1. Install Java 17 (Windows)

**Option A: Using Adoptium (Recommended)**
```powershell
# Download from https://adoptium.net/
# Run installer, add to PATH

# Verify installation
java -version
```

**Option B: Using Chocolatey**
```powershell
choco install temurin17 -y
```

### 2. Install Maven

```powershell
# Download from https://maven.apache.org/download.cgi
# Extract to C:\maven\
# Add C:\maven\bin to PATH

# Verify installation
mvn -version
```

### 3. Clone/Open Project in VS Code

```powershell
# Navigate to your project
cd "c:\Users\beryl\OneDrive\Desktop\QA Projects\Jenkins\Rest Assured"

# Open in VS Code
code .
```

### 4. Configure Test URLs

Edit each test file and update the `baseURL`:

**BasicSupplierApiTest.java:**
```java
private String baseURL = "https://api.example.com";  // Change to your API
```

**SupplierCRUDTest.java:**
```java
private String baseURL = "https://api.example.com";  // Change to your API
```

## Running Tests

### Option 1: Run All Tests
```bash
mvn test
```

### Option 2: Run Specific Test Class
```bash
mvn test -Dtest=RestAssuredConceptsGuide
# OR
mvn test -Dtest=BasicSupplierApiTest
# OR
mvn test -Dtest=SupplierCRUDTest
```

### Option 3: Run Specific Test Method
```bash
mvn test -Dtest=BasicSupplierApiTest#test_get_all_suppliers
```

### Option 4: Run with Custom Timeout
```bash
mvn test -DsuiteXmlFile=testng.xml
```

### View Test Reports
```powershell
# After tests complete, open report in browser
.\target\surefire-reports\index.html
```

## File Guide

### pom.xml
Maven configuration file with:
- **Java 17 LTS** compiler settings
- **Rest Assured** 5.4.0 - HTTP client library
- **TestNG** 7.10.1 - Test framework
- **Gson** 2.10.1 - JSON parsing
- **Maven Surefire** - Test execution plugin

### RestAssuredConceptsGuide.java
**Complete syntax reference** mapping Postman concepts to Rest Assured:

| Postman | Rest Assured |
|---------|--------------|
| Set base URL | `RestAssured.baseURI = "..."` |
| Headers tab | `.header("Key", "Value")` |
| Body tab | `.body("{ ... }")` |
| Send GET | `.when().get("/path")` |
| Tests - status | `.then().statusCode(200)` |
| Extract value | `.extract().response().jsonPath().getString("field")` |
| Environment var | Java class variable |
| Collection run | `@Test(priority=N, dependsOnMethods="...")` |

### BasicSupplierApiTest.java
Demonstrates **independent** tests:
```java
✓ GET /suppliers                    - Fetch all suppliers
✓ GET /suppliers/{id}               - Fetch specific supplier
✓ POST /suppliers                   - Create supplier
✓ PUT /suppliers/{id}               - Update supplier
✓ PATCH /suppliers/{id}             - Partial update
✓ DELETE /suppliers/{id}            - Delete supplier
✓ Verify deleted returns 404         - Confirm deletion
```

### SupplierCRUDTest.java
Demonstrates **chained** tests with state:

```
Test 1: Authenticate
  ├─ POST /auth/login
  └─ Extract token → store in authToken variable

Test 2: Create (depends on Test 1)
  ├─ POST /suppliers (with authToken)
  └─ Extract ID → store in supplierId variable

Test 3: Update (depends on Test 2)
  └─ PUT /suppliers/{supplierId}

Test 4: Get (depends on Test 2)
  └─ GET /suppliers/{supplierId}

Test 5: Delete (depends on Test 2 & 3)
  └─ DELETE /suppliers/{supplierId}

Test 6: Verify 404 (depends on Test 5)
  └─ GET /suppliers/{supplierId} → expect 404
```

**Key Advantage:** If Test 1 fails, tests 2-6 are automatically **skipped** — no cascading failures.

## Postman → Rest Assured Mapping

### GET Request
**Postman:**
1. Method: GET
2. URL: `{{base_url}}/booking/1`
3. Send
4. Tests: `pm.response.code === 200`

**Rest Assured:**
```java
given()
    .header("Accept", "application/json")
.when()
    .get("/booking/1")
.then()
    .statusCode(200);
```

### POST with Body
**Postman:**
1. Method: POST
2. Headers: `Content-Type: application/json`
3. Body (raw, JSON):
```json
{
  "firstname": "Jim",
  "lastname": "Brown",
  "totalprice": 111
}
```

**Rest Assured:**
```java
given()
    .header("Content-Type", "application/json")
    .body("{\"firstname\": \"Jim\", \"lastname\": \"Brown\", \"totalprice\": 111}")
.when()
    .post("/booking")
.then()
    .statusCode(200);
```

### Extract Response Value
**Postman Tests Tab:**
```javascript
var bookingId = pm.response.json().bookingid;
pm.environment.set("bookingId", bookingId);
```

**Rest Assured:**
```java
Response response = given()
    .when()
        .get("/booking")
    .then()
        .extract()
        .response();

int bookingId = response.jsonPath().getInt("bookingid");
// Store in class variable for next test
```

### Collection Run with Dependencies
**Postman:** Set tests to run in order with `pm.environment.set()` for data flow

**Rest Assured:**
```java
@Test(priority = 1)
public void test_01_login() { ... authToken = response.jsonPath().getString("token"); }

@Test(priority = 2, dependsOnMethods = {"test_01_login"})
public void test_02_createUser() { ... using authToken ... }

@Test(priority = 3, dependsOnMethods = {"test_02_createUser"})
public void test_03_getUser() { ... using userId ... }
```

## Jenkins Setup

### Step 1: Install Java & Maven on Jenkins Server

**On Jenkins server (SSH):**
```bash
# Ubuntu/Debian
sudo apt-get update
sudo apt-get install openjdk-17-jdk maven -y

# Verify
java -version
mvn -version
```

### Step 2: Configure Jenkins Tools

**Jenkins UI:**
1. Manage Jenkins → Tools
2. Add JDK Installation:
   - Name: `JDK17`
   - JAVA_HOME: `/usr/lib/jvm/java-17-openjdk-amd64`
3. Add Maven Installation:
   - Name: `Maven3`
   - MAVEN_HOME: `/opt/maven`

### Step 3: Create Pipeline Job

**Jenkins UI:**
1. New Job → Pipeline
2. Job Name: `Rest-Assured-Tests`
3. Pipeline → Pipeline script from SCM
4. SCM: Git
5. Repository URL: `https://github.com/your-username/rest-assured-tests.git`
6. Script Path: `Jenkinsfile`
7. Save → Build

### Step 4: Test the Pipeline

```bash
# On Jenkins UI
# Click "Build Now"

# Check console output for:
# ✓ Checkout
# ✓ Build
# ✓ Run API Tests
# ✓ Publish Results
```

## Running Purely on Jenkins (No Local Setup)

**What you need:**
1. Java test files (.java)
2. pom.xml
3. Jenkinsfile
4. All committed to GitHub

**What Jenkins provides:**
- Java 17 runtime
- Maven (auto-downloads Rest Assured, TestNG, etc.)
- Test execution
- Reports

**Workflow:**
```
Write code in GitHub UI (or locally)
    ↓
Push to GitHub
    ↓
Jenkins webhook triggers
    ↓
Jenkins runs: mvn test
    ↓
Reports published to Jenkins UI
```

## Common Commands

### View Test Results in Terminal
```bash
mvn test -X                          # Verbose output with debugging
mvn test -q                          # Quiet mode (minimal output)
mvn test -DsuiteXmlFile=testng.xml   # Run specific suite
```

### Clean Up
```bash
mvn clean                            # Remove target directory
mvn clean compile                    # Clean + recompile
mvn clean test                       # Clean + run all tests
```

### Run Tests Continuously (Watch Mode)
```bash
mvn -f . test -Dtest=BasicSupplierApiTest
# Re-run by typing: r (requires extra configuration)
```

## Troubleshooting

### Issue: "Java version is not 17"
```bash
# Check default Java
java -version

# Set JAVA_HOME
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
# OR on Windows:
set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.x.x
```

### Issue: "Maven not found"
```bash
# Check Maven installation
mvn -version

# If not found, add to PATH
# Windows: C:\maven\bin
# Linux: /opt/maven/bin
```

### Issue: "Cannot connect to API"
1. Verify API is running: `curl https://api.example.com/suppliers`
2. Check baseURL in test files
3. Check network/firewall rules
4. Try with `--noproxy localhost` if behind proxy

### Issue: "Tests fail on Jenkins but pass locally"
1. Jenkins uses different JAVA_HOME — verify JDK 17
2. Check environment variables in Jenkinsfile
3. Verify API endpoint is accessible from Jenkins server
4. Check test reports: `http://jenkins-url/job/Rest-Assured-Tests/XX/testReport/`

## Next Steps

1. **Update test URLs** - Change from `api.example.com` to your actual API
2. **Add more tests** - Copy template from existing test files
3. **Add authentication** - See `SupplierCRUDTest.java` for token handling
4. **Schedule runs** - Add `triggers { cron('0 2 * * *') }` to Jenkinsfile
5. **Send reports** - Configure email notifications in Jenkins

## Resources

- [Rest Assured Documentation](https://rest-assured.io/)
- [TestNG Documentation](https://testng.org/)
- [Maven Documentation](https://maven.apache.org/)
- [Java 17 Documentation](https://docs.oracle.com/en/java/javase/17/)
- [Jenkins Pipeline Guide](https://www.jenkins.io/doc/book/pipeline/)

---

**Summary:** This is a production-ready API testing framework using Rest Assured + TestNG with Java 17. Tests run locally in VS Code, or automatically on Jenkins via CI/CD — no GUI needed. The same code works everywhere because it's pure Java.
