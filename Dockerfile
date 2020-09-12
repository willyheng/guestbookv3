FROM openjdk:8-alpine

COPY target/uberjar/guestbookv3.jar /guestbookv3/app.jar

EXPOSE 3000

CMD ["java", "-jar", "/guestbookv3/app.jar"]
