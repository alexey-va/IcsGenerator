FROM eclipse-temurin:latest
LABEL authors="Grocer"

EXPOSE 9292

WORKDIR /app

COPY build/libs/*.jar /app/app.jar

ENV JAVA_OPTS="-Xmx256M"

CMD java $JAVA_OPTS -jar app.jar