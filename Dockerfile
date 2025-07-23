FROM openjdk:17-jdk-slim as build

WORKDIR /app
COPY . .

# Gradle로 빌드 (Gradle Wrapper가 있다면)
RUN chmod +x gradlew
RUN ./gradlew clean build -x test

FROM openjdk:17-jdk-slim
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080
CMD ["java", "-jar", "app.jar"]