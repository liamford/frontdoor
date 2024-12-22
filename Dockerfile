FROM openjdk:21-jdk-slim
EXPOSE 8080
VOLUME /tmp
ARG JAR_FILE
COPY ${JAR_FILE} frontdoor.jar
ENTRYPOINT ["java","-jar","/frontdoor.jar"]