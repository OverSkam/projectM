FROM eclipse-temurin:21-jdk
WORKDIR /app
COPY target/*.jar app.jar
COPY libs/visual-runtime.jar /app/libs/visual-runtime.jar
COPY target/libs/spigot-api.jar /app/libs/spigot-api.jar
ENV SPIGOT_API_PATH=/app/libs/spigot-api.jar \
    VISUAL_RUNTIME_PATH=/app/libs/visual-runtime.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]