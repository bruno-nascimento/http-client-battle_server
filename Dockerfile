# anapsix/alpine-java:8u172b11_server-jre_unlimited
FROM openjdk:10.0.2-13-slim 

ADD ./target/http-client-battle-server-0.0.1-jar-with-dependencies.jar /opt/http-client-battle-server/http-client-battle-server.jar
EXPOSE 8080
CMD java -Xmx2G -Xms2G -server -XX:+UseNUMA -XX:+UseParallelGC -XX:+AggressiveOpts -jar /opt/http-client-battle-server/http-client-battle-server.jar
