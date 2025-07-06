# Build stage
FROM eclipse-temurin:21-jdk as builder

WORKDIR /app
COPY . .

RUN ./gradlew clean bootJar -x test

# Runtime stage
FROM eclipse-temurin:21-jdk

WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8443

ENTRYPOINT ["java", "-jar", "app.jar"]
