FROM openjdk:17-slim

ENV TZ America/New_York

ADD coordinator-service/target/coordinator-service-2.1-SNAPSHOT.jar /opt/coordinator.jar

WORKDIR /opt/

CMD java -Xmx2G -jar coordinator.jar
