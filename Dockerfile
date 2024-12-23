FROM openjdk:21-jdk-slim
EXPOSE 8080
COPY build/libs/frontdoor-0.0.1-SNAPSHOT.jar frontdoor.jar
ENTRYPOINT ["java","-jar","/frontdoor.jar"]