# ===== Stage 1: Build =====
# gradle コマンドではなく ./gradlew を使う（Spring Boot 4.0 対応バージョンを自動取得）
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /app

# Gradle wrapper を先にコピー（キャッシュ活用）
COPY PensionStartingSimulator/gradlew ./
COPY PensionStartingSimulator/gradle/ ./gradle/
RUN chmod +x gradlew

# build.gradle をコピーして依存関係を事前解決（キャッシュレイヤー）
COPY PensionStartingSimulator/build.gradle PensionStartingSimulator/settings.gradle* ./
RUN ./gradlew dependencies --no-daemon 2>/dev/null || true

# ソースコードをコピーしてビルド（テストはスキップ）
COPY PensionStartingSimulator/ ./
RUN ./gradlew bootJar --no-daemon -x test

# ===== Stage 2: Run =====
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# ビルド成果物のみコピー
COPY --from=build /app/build/libs/*.jar app.jar

# Render が自動的に PORT 環境変数を設定する
EXPOSE 8080

# 本番プロファイルで起動
ENV SPRING_PROFILES_ACTIVE=prod

ENTRYPOINT ["java", "-jar", "app.jar"]
