# Stage 1: Build
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder
WORKDIR /build
COPY pom.xml .
RUN mvn dependency:go-offline -B || true
COPY src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
LABEL maintainer="EduSphere Platform Team"
LABEL service="iam-service"
RUN apk add --no-cache curl tzdata
RUN addgroup -g 10001 -S spring && adduser -u 10001 -S spring -G spring
WORKDIR /app
COPY --from=builder /build/target/*.jar /app/app.jar
RUN chown -R spring:spring /app
USER spring:spring
EXPOSE 8091
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"
HEALTHCHECK --interval=10s --timeout=5s --start-period=90s --retries=6 \
  CMD curl -f http://localhost:8091/actuator/health || exit 1
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
