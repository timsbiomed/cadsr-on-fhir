# FHIR on caDSR

A light-weight FHIR Terminonology service based on [caDSR](https://datascience.cancer.gov/resources/metadata) and [NCIt](https://ncithesaurus.nci.nih.gov/ncitbrowser/).

## Run

The application is deliverd as a docker image on Docker hub. To run the docker image, 

```
docker run -p 8080:8080 hotecosystem/cadsr-on-fhir
```

## Development

To run from source with mvn, run 

```
mvn jetty:run 
```

To build and push docker images with maven, 

```
mvn dockerfile:build
mvn dockerfile:push
```

To learn how to set up Maven to work with docker, see [this](https://medium.com/better-programming/build-push-to-docker-with-maven-eea7c4b8cfa2)




