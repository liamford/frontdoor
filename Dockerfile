FROM openjdk:21-jdk-slim
VOLUME /tmp
COPY build/libs/*.jar frontdoor.jar
ENTRYPOINT ["java","-jar","/frontdoor.jar"]