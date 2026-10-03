# Stage 1: build the jar with Maven. This stage is thrown away; only the jar is kept.
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /build
# Copy pom.xml first so the downloaded libraries are cached until the pom changes.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q -DskipTests package

# Stage 2: a small runtime image with only a Java runtime and the jar.
FROM eclipse-temurin:25-jre
WORKDIR /app
# Do not run as root inside the container.
RUN useradd --system --no-create-home uqlab
COPY --from=build /build/target/*.jar app.jar
USER uqlab
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
