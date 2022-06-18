FROM openjdk:11-jdk-slim

WORKDIR /app

COPY . .
RUN ./gradlew build buildBackend -x test

CMD java -Dfelix.cm.dir=/app/tools/docker/openems-backend/config.d -jar build/openems-backend.jar