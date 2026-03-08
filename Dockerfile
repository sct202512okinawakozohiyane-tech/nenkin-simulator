# ===== Stage 1: Build =====
FROM gradle:8.12-jdk17 AS build
WORKDIR /app

# 依存関係キャッシュ最適化（build.gradle 先にコピー）
COPY PensionStartingSimulator/build.gradle PensionStartingSimulator/settings.gradle* ./
RUN gradle dependencies --no-daemon 2>/dev/null || true

# ソースコードをコピーしてビルド（テストはスキップ）
COPY PensionStartingSimulator/ ./
RUN gradle bootJar --no-daemon -x test

# ===== Stage 2: Run =====
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# ビルド成果物のみコピー
COPY --from=build /app/build/libs/*.jar app.jar

# Render が自動的に PORT 環境変数を設定する（application.properties で ${PORT:8080} 対応済み）
EXPOSE 8080

# 本番プロファイルで起動（静的ファイルを classpath から配信）
ENV SPRING_PROFILES_ACTIVE=prod

ENTRYPOINT ["java", "-jar", "app.jar"]
