FROM adoptopenjdk/openjdk11:armv7l-ubuntu-jdk-11.0.15_10

WORKDIR /app

COPY . .
RUN ./gradlew build buildBackend -x test

CMD java -Dfelix.cm.dir=/app/tools/docker/openems-backend/config.d -jar build/openems-backend.jar