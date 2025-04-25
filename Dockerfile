FROM openjdk:17-slim
EXPOSE 8080

ARG JAR_FILE
COPY ${JAR_FILE} app.jar
LABEL authors="jks83"

ENTRYPOINT ["java", "-Xms128m", "-Xmx256m", "-XX:MaxMetaspaceSize=128m", "-XX:+UseG1GC", "-Xlog:gc*:file=/logs/g1-gc.log:tags,uptime,time,level", "-XX:+HeapDumpOnOutOfMemoryError", "-jar", "/app.jar"]
