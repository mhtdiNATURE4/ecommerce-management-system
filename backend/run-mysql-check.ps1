Set-Location "C:\Users\imuht\Spring Boot\ecommerce-management-system\backend"
& "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe" -u root -prootpassword -e "SELECT user, host FROM mysql.user; SELECT schema_name FROM information_schema.schemata WHERE schema_name='ecommerce';"
