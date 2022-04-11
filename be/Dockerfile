FROM bellsoft/liberica-openjdk-alpine:11


WORKDIR /tmp/be
COPY ./build/libs/be-0.0.1-SNAPSHOT.jar .
COPY ffmpeg-5.0.1-arm64-static/ffmpeg .
COPY ffmpeg-5.0.1-arm64-static/ffprobe .
COPY test.mp3 .

ENV SPRING_DATASOURCE_URL "jdbc:postgresql://172.17.0.2:5432/postgres"

CMD ["java", "-Dspring.profiles.active=docker", "-jar", "be-0.0.1-SNAPSHOT.jar"]