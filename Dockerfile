FROM registry.cn-beijing.aliyuncs.com/wenning/maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM registry.cn-beijing.aliyuncs.com/wenning/eclipse-temurin:21-jre-alpine
WORKDIR /app

ARG PROFILE=dev
ENV SPRING_PROFILES_ACTIVE=${PROFILE}

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
