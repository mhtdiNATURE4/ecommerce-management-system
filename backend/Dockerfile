FROM maven:3.9.4-eclipse-temurin-21 as build
WORKDIR /app

# copy only what is needed to build
COPY pom.xml mvnw .
COPY .mvn .mvn
COPY src ./src

RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
