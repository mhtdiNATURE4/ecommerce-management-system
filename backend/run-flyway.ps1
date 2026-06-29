Set-Location "C:\Users\imuht\Spring Boot\ecommerce-management-system\backend"
$flywayUrl = 'jdbc:mysql://127.0.0.1:3306/ecommerce?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true'
& "C:\Program Files\Apache\maven\bin\mvn.cmd" -DskipTests "-Dflyway.url=$flywayUrl" -Dflyway.user=root -Dflyway.password=rootpassword flyway:info
