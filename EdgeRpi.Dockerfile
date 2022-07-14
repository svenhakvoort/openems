FROM adoptopenjdk/openjdk11:armv7l-ubuntu-jdk-11.0.15_10

WORKDIR /app

COPY . .
RUN ./gradlew build buildEdge -x test


CMD java -Dfelix.cm.dir=/app/tools/docker/openems-edge/config.d -Dopenems.data.dir=/app/tools/docker/openems-edge/data -jar build/openems-edge.jar