FROM openjdk:11-jdk-slim

USER 1000
WORKDIR /app

RUN apt-get update && apt-get install librxtx-java
RUN adduser 1000 dialout
RUN adduser 1000 tty

COPY . .
RUN ./gradlew build buildEdge -x test


CMD java -Djava.library.path=/usr/lib/jni -Dfelix.cm.dir=/app/tools/docker/openems-edge/config.d -Dopenems.data.dir=/app/tools/docker/openems-edge/data -jar build/openems-edge.jar