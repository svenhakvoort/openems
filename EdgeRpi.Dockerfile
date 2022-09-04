FROM adoptopenjdk/openjdk11:armv7l-ubuntu-jdk-11.0.15_10

WORKDIR /app

USER root
RUN apt-get update && apt-get install librxtx-java

RUN adduser root dialout
RUN adduser root tty

COPY . .
RUN ./gradlew build buildEdge -x test


CMD java -Djava.library.path=/usr/lib/jni -Dfelix.cm.dir=/app/tools/docker/openems-edge/config.d -Dopenems.data.dir=/app/tools/docker/openems-edge/data -jar build/openems-edge.jar