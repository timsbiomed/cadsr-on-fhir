FROM jetty:9.4.27-jre11-slim

COPY ./target/fhir-on-cadsr.war /hapi/fhir-on-cadsr.war
COPY ./src/main/resources/fhir-on-cadsr-context.xml /var/lib/jetty/webapps/fhir-on-cadsr-context.xml

USER jetty:jetty
EXPOSE 8080
CMD ["java", "-Xmx8g", "-jar","/usr/local/jetty/start.jar"]
