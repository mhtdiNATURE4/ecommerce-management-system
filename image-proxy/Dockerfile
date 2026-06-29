FROM maven:3.8.8-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN mvn -e -B package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /workspace/target/image-proxy-1.0.0.jar /app/image-proxy.jar
EXPOSE 9090
ENTRYPOINT ["java","-jar","/app/image-proxy.jar"]
