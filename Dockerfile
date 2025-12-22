# Build arguments (must be declared before first FROM to be available in all FROM statements)
ARG BUILD_IMAGE=eclipse-temurin:21-jdk
ARG RUNTIME_IMAGE=eclipse-temurin:21-jre

# Build stage
FROM ${BUILD_IMAGE} AS build

WORKDIR /app

# Install Maven
RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*

# Copy Maven files
COPY pom.xml .

# Download dependencies (cached layer)
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build application
RUN mvn clean package -DskipTests -B

# Runtime stage
FROM ${RUNTIME_IMAGE}

WORKDIR /app

# Copy JAR from build stage
COPY --from=build /app/target/*.jar app.jar

# Expose port
EXPOSE 8080

# Set entrypoint
ENTRYPOINT ["java", "-jar", "app.jar"]

