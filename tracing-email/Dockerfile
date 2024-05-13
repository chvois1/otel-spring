FROM eclipse-temurin:11-jdk-alpine

RUN mkdir /opt/app
#COPY /maven/ /opt/app
COPY target/email-service-0.0.1-SNAPSHOT.jar /opt/app
CMD ["java", "-jar", "/opt/app/${project.artifactId}-${project.version}.jar"]
