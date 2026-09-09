# --- 빌드 스테이지 ---
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# 의존성만 먼저 받아서 레이어 캐싱 (소스만 바뀌면 이 레이어는 재사용됨)
COPY gradlew build.gradle settings.gradle ./
COPY gradle gradle
RUN chmod +x gradlew
RUN ./gradlew --no-daemon dependencies || true

COPY src src
RUN ./gradlew --no-daemon bootJar -x test

# --- 실행 스테이지 ---
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 1001 spring
COPY --from=build /app/build/libs/*.jar app.jar
USER spring

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
