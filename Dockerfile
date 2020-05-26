FROM jetty:9.4.27-jre11-slim

COPY ./target/cadsr-on-fhir.war /hapi/cadsr-on-fhir.war
COPY ./src/main/resources/cadsr-on-fhir-context.xml /var/lib/jetty/webapps/cadsr=on-fhir-context.xml

USER jetty:jetty
EXPOSE 8080
CMD ["java", "-Xmx8g", "-jar","/usr/local/jetty/start.jar"]
