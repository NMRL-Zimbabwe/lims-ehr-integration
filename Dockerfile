FROM openjdk:11
EXPOSE 8080
ADD target/lims-ehr-integration-0.0.1.jar lims-ehr-integration-0.0.1.jar
ENTRYPOINT ["java","-jar", "/lims-ehr-integration-0.0.1.jar"]
