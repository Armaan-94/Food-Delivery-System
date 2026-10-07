# One Dockerfile for every service: docker build --build-arg MODULE=user-service .
# docker-compose.yml passes the right MODULE for each service.

FROM maven:3.9-eclipse-temurin-17 AS build
ARG MODULE
WORKDIR /build
COPY . .
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q -pl ${MODULE} -am -DskipTests package

FROM eclipse-temurin:17-jre
ARG MODULE
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --no-create-home app
WORKDIR /app
COPY --from=build /build/${MODULE}/target/${MODULE}-*.jar app.jar
USER app
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
