@echo off
cd /d "C:\Users\imuht\Spring Boot\ecommerce-management-system\backend"
set "FLYWAY_URL=jdbc:mysql://127.0.0.1:3306/ecommerce?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true"
mvn -DskipTests -Dflyway.url=%FLYWAY_URL% -Dflyway.user=root -Dflyway.password=rootpassword flyway:info
