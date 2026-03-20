# ClinicCare - Clinic Appointment Booking System

ClinicCare is a full-stack web application for clinic appointment booking and management with three roles:
- `PATIENT`
- `DOCTOR`
- `ADMIN`

## Tech Stack
- Backend: Java 17, Spring Boot, Spring Security, JWT, Spring Data JPA
- Frontend: HTML, CSS, JavaScript, jQuery, Bootstrap, DataTables, Chart.js
- Database: MySQL
- Build Tool: Maven
- API Docs: Swagger/OpenAPI

## Prerequisites
- Java 17
- Maven 3.9+
- MySQL 8+

## Project Structure
- Root contains docs and project metadata
- Spring Boot app is inside `cliniccare/`

## 1) Configure Environment Variables
Create a local `.env` file at the project root (already gitignored) or export variables in your shell.

Example:

```bash
export DB_URL="jdbc:mysql://localhost:3306/cliniccare_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
export DB_USERNAME="root"
export DB_PASSWORD="your_mysql_password"
export APP_JWT_SECRET="your_very_long_random_secret_key_min_32_chars"
export APP_JWT_EXPIRATION_MINUTES="1440"
```

Notes:
- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `APP_JWT_SECRET` are required for normal runs.
- If you use a `.env` file with `source .env`, keep URL values quoted.

## 2) Start the Application

```bash
cd cliniccare
mvn clean spring-boot:run
```

By default, app runs on `http://localhost:8080`.

## 3) Database Behavior
- Hibernate setting: `ddl-auto: update`
- Seed script: `src/main/resources/data.sql`
- SQL init mode: always enabled

On startup, base data is inserted/updated (idempotent inserts for main records), including:
- 1 admin user
- 2 doctor users + doctor profiles
- 2 services
- doctor-service mappings
- future available slots

## Seed Login Accounts (Development)
These are from `data.sql` for local academic/demo usage:
- Admin: `admin@cliniccare.com` / `Password@123`
- Doctor: `doctor@cliniccare.com` / `Password@123`
- Doctor (Dentist): `dentist@cliniccare.com` / `Password@123`

## 4) Access the Application

Frontend pages (served from Spring static resources):
- Home: `http://localhost:8080/`
- Login: `http://localhost:8080/login.html`
- Register: `http://localhost:8080/register.html`
- Patient Dashboard: `http://localhost:8080/patient-dashboard.html`
- Doctor Dashboard: `http://localhost:8080/doctor-dashboard.html`
- Admin Dashboard: `http://localhost:8080/admin-dashboard.html`

## 5) Swagger / OpenAPI
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- OpenAPI YAML: `http://localhost:8080/v3/api-docs.yaml`

To export docs for report submission:

```bash
cd cliniccare
mkdir -p docs
curl -s http://localhost:8080/v3/api-docs > docs/openapi.json
curl -s http://localhost:8080/v3/api-docs.yaml > docs/openapi.yaml
```

## 6) Run Tests

```bash
cd cliniccare
mvn clean verify
```

This runs unit/integration/API/UI test suites configured in the project and generates test reports under `target/`.

## 7) CI Pipeline (Jenkins)
The project includes `cliniccare/Jenkinsfile` with stages:
1. `SCM`
2. `Build & Test`
3. `SonarQube Analysis`
4. `Quality Gate`

JUnit reports are published from Surefire/Failsafe report paths.

## Troubleshooting
- Error: `Unable to determine Dialect without JDBC metadata`  
  Check `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` in your environment.

- Error: `Unknown database 'cliniccare_db'`  
  Ensure MySQL is running and use the provided `DB_URL` with `createDatabaseIfNotExist=true`.

- If startup fails on port conflict, stop the process using the port or run Spring Boot on another port.
