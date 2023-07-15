FROM node:18-alpine as web-builder

WORKDIR /tmp/app

COPY web-app/package.json .
RUN npm i

COPY web-app/tsconfig.json .
COPY web-app/preact.config.js .
COPY web-app/replace-react.js .
COPY web-app/rollup.config.js .
COPY web-app/rollup.config-process-ts.js .
COPY web-app/generateInfo.sh .
COPY web-app/src src

RUN npm run dist


FROM bellsoft/liberica-openjdk-alpine:11  as builder

RUN apk add --no-cache bash

WORKDIR /tmp/app
COPY gradlew .
COPY gradle gradle
COPY settings.gradle .
RUN ./gradlew --no-daemon

COPY build.gradle .
COPY be/build.gradle be/build.gradle
RUN ./gradlew --no-daemon -Dorg.gradle.jvmargs="-Xmx2g -Xms2g" :be:resolveDependencies

COPY be be
COPY --from=web-builder /tmp/app/build be/src/main/resources/public

RUN ./gradlew --no-daemon :be:generateTables
RUN ./gradlew --no-daemon -Dorg.gradle.jvmargs="-Xmx2g -Xms2g" fatJar

FROM bellsoft/liberica-openjdk-alpine:11

WORKDIR /app

ARG TARGETARCH

RUN wget "https://johnvansickle.com/ffmpeg/releases/ffmpeg-release-${TARGETARCH}-static.tar.xz" \
    && tar xf "ffmpeg-release-${TARGETARCH}-static.tar.xz" \
    && mv ffmpeg-*-static/ffmpeg . \
    && rm -rf ffmpeg-*-static \
    && rm ffmpeg-release-*-static.tar.xz
#    && mv ffmpeg-*-static/ffprobe . \

COPY --from=builder /tmp/app/be/build/libs/be-all-0.0.1-SNAPSHOT.jar app.jar

ENV JAVA_OPTS "-Xmx256m \
 -Xss256k \
 -XX:+UseShenandoahGC \
 -XX:+UnlockExperimentalVMOptions \
 -XX:ShenandoahUncommitDelay=10000 \
 -XX:ShenandoahGuaranteedGCInterval=30000 \
 --add-opens java.base/java.lang=ALL-UNNAMED \
 --add-opens java.base/java.nio=ALL-UNNAMED \
 "

CMD ["sh", "-c", "java ${JAVA_OPTS} -jar app.jar"]