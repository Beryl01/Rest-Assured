# Quick Setup Guide - Rest Assured with Java 17 LTS

This guide will get you running API tests in **5 minutes**.

## Step 1: Install Java 17 (Windows)

### Using Adoptium (Recommended)
1. Go to https://adoptium.net/
2. Download **Java 17 LTS** (Latest LTS Release)
3. Run installer
4. **Important:** During installation, check "Set JAVA_HOME variable"

### Verify Installation
Open PowerShell and run:
```powershell
java -version
```
You should see:
```
openjdk version "17.0.x" ...
```

---

## Step 2: Install Maven

### Option A: Download & Extract (Simplest)
1. Download from https://maven.apache.org/download.cgi
   - Get the **Binary zip archive** (apache-maven-3.9.x-bin.zip)
2. Extract to `C:\maven\`
3. Add `C:\maven\bin` to Windows PATH:
   - Press **Win + R**, type `sysdm.cpl`
   - Click "Environment Variables"
   - Under "System variables", find "Path" → Edit
   - Add: `C:\maven\bin`
   - Click OK

### Option B: Use Chocolatey (Faster)
Open PowerShell as Admin and run:
```powershell
choco install maven -y
```

### Verify Installation
```powershell
mvn -version
```
You should see:
```
Apache Maven 3.9.x ...
```

---

## Step 3: Open Project in VS Code

1. Open VS Code
2. **File** → **Open Folder**
3. Navigate to: `c:\Users\beryl\OneDrive\Desktop\QA Projects\Jenkins\Rest Assured`
4. Click **Select Folder**

You should see the project structure in the left panel.

---

## Step 4: Run Tests

### Open Terminal in VS Code
**Ctrl + `** (backtick) or **Terminal** → **New Terminal**

### Run All Tests
```bash
mvn test
```

### Expected Output
```
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.qa.api.tests.RestAssuredConceptsGuide
[INFO] Tests run: 10, Failures: 0, Skipped: 0, Time elapsed: 5.2 s
...
[INFO] BUILD SUCCESS
```

---

## Step 5: View Test Reports

After tests complete:
```powershell
# Navigate to reports directory
cd target/surefire-reports

# Open in browser
start index.html
```

---

## Common Commands

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=BasicSupplierApiTest

# Run specific test method
mvn test -Dtest=RestAssuredConceptsGuide#test_GET_with_status_assertion

# Clean and rebuild
mvn clean test

# Skip tests (just build)
mvn clean compile

# Verbose output (debugging)
mvn test -X
```

---

## Update Base URL for Your API

The test files currently use `https://api.example.com`. Change this to your actual API:

### File 1: BasicSupplierApiTest.java
```java
private String baseURL = "https://your-api-url.com";  // ← Change this
```

### File 2: SupplierCRUDTest.java
```java
private String baseURL = "https://your-api-url.com";  // ← Change this
```

### File 3: RestAssuredConceptsGuide.java
```java
private String baseURL = "https://your-api-url.com";  // ← Change this
```

---

## Project Structure

```
Rest Assured/
├── pom.xml                          ← Maven config (dependencies)
├── testng.xml                       ← Test suite configuration
├── Jenkinsfile                      ← CI/CD for Jenkins
├── README.md                        ← Full documentation
├── SETUP.md                         ← This file
├── src/test/java/
│   ├── RestAssuredConceptsGuide.java    (reference guide)
│   ├── BasicSupplierApiTest.java        (simple tests)
│   └── SupplierCRUDTest.java            (chained tests)
└── src/test/resources/
    └── config.properties                (configuration file)
```

---

## Understanding the Tests

### RestAssuredConceptsGuide.java
Reference file showing ALL Rest Assured syntax with Postman mappings.
- Maps each Postman concept to Java code
- Safe to read; not all tests may pass against example.com

### BasicSupplierApiTest.java
Simple tests that are **independent**:
- GET, POST, PUT, DELETE requests
- No dependencies between tests
- Easy to understand; good for learning

### SupplierCRUDTest.java
**Advanced** - Tests that depend on each other:
1. Authenticate (extract token)
2. Create supplier (extract ID)
3. Update supplier (use ID from step 2)
4. Get supplier (use ID from step 2)
5. Delete supplier (use ID from step 2)
6. Verify deleted (should return 404)

If step 1 fails, steps 2-6 are **automatically skipped** ✓

---

## What's Happening Behind the Scenes

### Command: `mvn test`
```
Maven (your build tool)
  ↓
Reads pom.xml
  ↓
Downloads dependencies (Rest Assured, TestNG, Gson, etc.) to ~/.m2/
  ↓
Compiles your .java files
  ↓
Runs TestNG test suite
  ↓
Generates reports in target/surefire-reports/
```

---

## Troubleshooting

### Error: "java: command not found"
**Solution:** Java 17 not installed or not in PATH
```powershell
# Check if Java is installed
dir "C:\Program Files\Eclipse Adoptium"

# If not there, install from https://adoptium.net/
```

### Error: "mvn: command not found"
**Solution:** Maven not installed or not in PATH
```powershell
# Check if Maven is installed
dir C:\maven\bin

# If not there, download from https://maven.apache.org/
```

### Error: "Connection refused" when running tests
**Solution:** Your API is not running or URL is wrong
1. Check the baseURL in test file
2. Verify API is actually running
3. Test with browser: `https://your-api-url.com/api/v1/suppliers`

### Error: "BUILD FAILURE" with dependency errors
**Solution:** Maven couldn't download dependencies
```powershell
# Clear cache and retry
mvn clean -U test
```

---

## Running on Jenkins (CI/CD)

Once this project is on GitHub, Jenkins can run tests automatically without needing local setup:

1. **Push to GitHub:** `git push origin main`
2. **Create Jenkins Pipeline job** pointing to your GitHub repo
3. **Jenkins handles:** Java 17, Maven, test execution, reports

See **README.md** for full Jenkins setup.

---

## Next Steps

1. ✓ Set baseURL to your actual API
2. ✓ Run `mvn test` to verify setup
3. ✓ Read **RestAssuredConceptsGuide.java** to understand syntax
4. ✓ Modify tests to match your API endpoints
5. ✓ Commit to GitHub for Jenkins integration

---

## Key Concepts

| Concept | Explanation |
|---------|-------------|
| **pom.xml** | Maven config - defines dependencies (Rest Assured, TestNG, etc.) |
| **testng.xml** | Defines which test classes to run |
| **Jenkinsfile** | Script for CI/CD pipeline |
| **mvn test** | Command to compile + run all tests |
| **Rest Assured** | Java library that does what Postman does (HTTP requests, assertions) |
| **TestNG** | Test framework (like unittest in Python) |
| **Java 17 LTS** | Long-term support version of Java |

---

**You're all set!** 🚀

Run `mvn test` and watch your API tests execute in VS Code, then deploy to Jenkins for CI/CD.
