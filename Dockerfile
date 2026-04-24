FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app

COPY build/libs/*-SNAPSHOT.jar app.jar

ENV SPRING_PROFILES_ACTIVE=prod

EXPOSE 8080

CMD ["java", "-Xms256m", "-Xmx512m", "-jar", "app.jar"]