# ---------- 构建阶段 ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B clean package -DskipTests

# ---------- 运行阶段 ----------
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/homework-online-1.0.0.jar app.jar
RUN mkdir -p /app/uploads /app/data
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
