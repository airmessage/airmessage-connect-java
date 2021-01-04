#Build JAR with gradle
FROM gradle:jdk11 AS build
COPY . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle shadowJar

#Use JDK 11
FROM openjdk:11

ARG WRITE_GOOGLE_APPLICATION_CREDENTIALS
ARG GOOGLE_APPLICATION_CREDENTIALS
ARG WRITE_SERVER_CERTIFICATE
ARG SERVER_CERTIFICATE
ARG WRITE_SENTRY_PROPERTIES_FILE
ARG SENTRY_PROPERTIES_FILE=sentry.pem

#Copy built JAR file
COPY --from=build /home/gradle/src/build/libs/airmessage-connect.jar .

#Write external configuration files
RUN echo $WRITE_GOOGLE_APPLICATION_CREDENTIALS | base64 --decode > $GOOGLE_APPLICATION_CREDENTIALS
RUN echo $WRITE_SERVER_CERTIFICATE | base64 --decode > $SERVER_CERTIFICATE
RUN echo $WRITE_SENTRY_PROPERTIES_FILE | base64 --decode > $SENTRY_PROPERTIES_FILE

#Run JAR
CMD java -jar airmessage-connect.jar insecure