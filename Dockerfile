FROM gradle:8.5-jdk17 AS build

WORKDIR /app
COPY . .

RUN gradle clean build -x test

FROM tomcat:9.0-jdk17-openjdk-slim

RUN rm -rf /usr/local/tomcat/webapps/ROOT
COPY --from=build /app/build/libs/*.war /usr/local/tomcat/webapps/ROOT.war

EXPOSE 8080
CMD ["catalina.sh", "run"]