FROM openjdk:11-jdk-slim

WORKDIR /app

COPY . .
RUN ./gradlew build buildEdge -x test


CMD java -Dfelix.cm.dir=/app/tools/docker/openems-edge/config.d -Dopenems.data.dir=/app/tools/docker/openems-edge/data -jar build/openems-edge.jar