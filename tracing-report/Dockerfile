FROM eclipse-temurin:11-jdk-alpine

RUN mkdir /opt/app
COPY target/report-service-0.0.1-SNAPSHOT.jar /opt/app

CMD ["java", "-jar", "report-service-0.0.1-SNAPSHOT.jar"]
