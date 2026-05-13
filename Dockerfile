# syntax=docker/dockerfile:1.7

FROM eclipse-temurin:25-jdk AS build

WORKDIR /workspace

COPY .mvn .mvn
COPY --chmod=755 mvnw .
COPY pom.xml .

RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -B -q dependency:go-offline

COPY src ./src

RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -B -q -DskipTests package


FROM eclipse-temurin:25-jre AS runtime

RUN useradd \
    --system \
    --uid 1001 \
    --home-dir /app \
    --create-home \
    --shell /usr/sbin/nologin \
    mcp

WORKDIR /app

COPY --from=build --chown=mcp:mcp /workspace/target/*.jar app.jar

USER mcp

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"

ENTRYPOINT ["java", "-jar", "app.jar"]