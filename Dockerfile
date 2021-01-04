FROM gradle:jdk11 AS build
COPY . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle shadowJar

FROM openjdk:11
COPY --from=build /home/gradle/src/build/libs/airmessage-connect.jar .
CMD java -jar airmessage-connect.jar