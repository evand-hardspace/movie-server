FROM eclipse-temurin:21-jre
WORKDIR /app
COPY build/libs/movie-server-all.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
