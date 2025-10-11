# Development Environment Setup Guide

This guide will help you set up the development environment for the Feature Service project.

## Prerequisites
- **Docker** (for running PostgreSQL)
- **Java 21** (OpenJDK 21)
- **Maven** (wrapper included: `./mvnw`)

## Steps

### 1. Install Java 21
On Ubuntu:
```bash
sudo apt update
sudo apt install -y openjdk-21-jdk
```
Verify installation:
```bash
java --version
javac --version
```

### 2. Install Docker
Follow the official guide: https://docs.docker.com/engine/install/
Verify installation:
```bash
docker --version
```

### 3. Start PostgreSQL Database
Run the following command to start PostgreSQL 16 in Docker:
```bash
docker run --name feature-db -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres -p 55432:5432 -d postgres:16
```

### 4. Configure Application
The default database connection is set in `src/main/resources/application.properties`:
```
spring.datasource.url=jdbc:postgresql://localhost:55432/postgres
spring.datasource.username=postgres
spring.datasource.password=postgres
```

### 5. Build and Run the Project
Build and test:
```bash
./mvnw clean verify
```
Run the application:
```bash
./mvnw spring-boot:run
```

### 6. Access the Application
- The service runs on port `8081` by default.
- API documentation is available via Swagger (see `application.properties` for details).

## Troubleshooting
- **Database connection errors:** Ensure the PostgreSQL container is running and accessible at `localhost:55432`.
- **Port conflicts:** Change the port in `application.properties` or Docker command if needed.

---
For further help, contact: support@sivalabs.in
